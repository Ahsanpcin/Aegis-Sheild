package com.example.detection.preprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.Image
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class FramePreprocessor(
    inputWidth: Int = 224,
    inputHeight: Int = 224,
    isQuantizedUInt8: Boolean = false
) {
    var inputWidth: Int = inputWidth
        private set
    var inputHeight: Int = inputHeight
        private set
    var isQuantizedUInt8: Boolean = isQuantizedUInt8
        private set

    private var tensorBuffer: ByteBuffer = allocateBuffer(inputWidth, inputHeight, isQuantizedUInt8)
    private var intValues: IntArray = IntArray(inputWidth * inputHeight)

    // Reusable scratch bitmaps to eliminate per-frame heap allocations & GC churn
    private var reusableRowBitmap: Bitmap? = null
    private var reusableCroppedBitmap: Bitmap? = null
    private var reusableScaledBitmap: Bitmap? = null
    private var reusableScaledCanvas: Canvas? = null
    private val scaleSrcRect = Rect()
    private val scaleDstRect = Rect(0, 0, inputWidth, inputHeight)
    private val scalePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Perceptual 5x5 grid luma fingerprint to skip both identical and near-static frames
    private var hasProcessedFrame: Boolean = false
    private val lastSampleLumas = IntArray(LUMA_GRID_SIZE * LUMA_GRID_SIZE)
    private val currentSampleLumas = IntArray(LUMA_GRID_SIZE * LUMA_GRID_SIZE)

    fun configure(width: Int, height: Int, quantizedUInt8: Boolean) {
        if (this.inputWidth == width && this.inputHeight == height && this.isQuantizedUInt8 == quantizedUInt8) {
            return
        }
        this.inputWidth = width
        this.inputHeight = height
        this.isQuantizedUInt8 = quantizedUInt8
        this.tensorBuffer = allocateBuffer(width, height, quantizedUInt8)
        this.intValues = IntArray(width * height)
        this.scaleDstRect.set(0, 0, width, height)
        this.reusableScaledCanvas = null
        this.reusableScaledBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        this.reusableScaledBitmap = null
    }

    private fun allocateBuffer(width: Int, height: Int, quantizedUInt8: Boolean): ByteBuffer {
        val bytesPerChannel = if (quantizedUInt8) 1 else 4
        return ByteBuffer.allocateDirect(1 * width * height * 3 * bytesPerChannel).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Extracts an ARGB Bitmap from an ImageReader Image buffer using pooled reusable bitmaps.
     * Callers must NOT call Bitmap.recycle() on the returned pooled bitmap.
     */
    fun extractBitmapFromImage(image: Image): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null

        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride.coerceAtLeast(1)
        val rowStride = planes[0].rowStride
        val rowPadding = (rowStride - pixelStride * image.width).coerceAtLeast(0)
        val rowBitmapWidth = image.width + rowPadding / pixelStride

        val rowBitmap = obtainPooledBitmap(
            existing = reusableRowBitmap,
            width = rowBitmapWidth,
            height = image.height
        ).also { reusableRowBitmap = it }

        buffer.rewind()
        rowBitmap.copyPixelsFromBuffer(buffer)

        if (rowPadding <= 0) {
            return rowBitmap
        }

        val cropped = obtainPooledBitmap(
            existing = reusableCroppedBitmap,
            width = image.width,
            height = image.height
        ).also { reusableCroppedBitmap = it }

        val canvas = Canvas(cropped)
        scaleSrcRect.set(0, 0, image.width, image.height)
        val dst = Rect(0, 0, image.width, image.height)
        canvas.drawBitmap(rowBitmap, scaleSrcRect, dst, null)
        return cropped
    }

    private fun obtainPooledBitmap(existing: Bitmap?, width: Int, height: Int): Bitmap {
        if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
            return existing
        }
        if (existing != null && !existing.isRecycled) {
            existing.recycle()
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Perceptual 5x5 block-luma delta check:
     * Returns true when a newly captured frame is visually identical or near-static
     * (e.g. blinking cursor, clock minute change, minor compression noise) so we can skip inference
     * without missing real content changes.
     */
    fun isFrameDuplicate(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false

        var idx = 0
        for (gy in 0 until LUMA_GRID_SIZE) {
            val y = ((gy + 0.5f) * h / LUMA_GRID_SIZE).toInt().coerceIn(0, h - 1)
            for (gx in 0 until LUMA_GRID_SIZE) {
                val x = ((gx + 0.5f) * w / LUMA_GRID_SIZE).toInt().coerceIn(0, w - 1)
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                currentSampleLumas[idx++] = (r * 30 + g * 59 + b * 11) / 100
            }
        }

        if (!hasProcessedFrame) {
            hasProcessedFrame = true
            System.arraycopy(currentSampleLumas, 0, lastSampleLumas, 0, lastSampleLumas.size)
            return false
        }

        var sumDiff = 0
        var maxDiff = 0
        for (i in 0 until lastSampleLumas.size) {
            val diff = abs(currentSampleLumas[i] - lastSampleLumas[i])
            sumDiff += diff
            if (diff > maxDiff) maxDiff = diff
        }
        val avgDiff = sumDiff / lastSampleLumas.size

        // Skip only when both overall frame average delta AND single-cell peak delta stay within perceptual noise tolerance
        val isDuplicate = avgDiff <= MAX_AVG_LUMA_DELTA && maxDiff <= MAX_PEAK_LUMA_DELTA
        if (!isDuplicate) {
            System.arraycopy(currentSampleLumas, 0, lastSampleLumas, 0, lastSampleLumas.size)
        }
        return isDuplicate
    }

    /**
     * Scales bitmap into pooled scratch surface and fills the direct ByteBuffer
     * with normalized Float32 RGB [0.0f..1.0f] or UInt8 [0..255] with zero heap churn.
     */
    fun preprocessBitmapToTensorBuffer(bitmap: Bitmap): ByteBuffer {
        val sourceBitmap = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            val scaled = obtainPooledBitmap(reusableScaledBitmap, inputWidth, inputHeight)
                .also { reusableScaledBitmap = it }
            val canvas = reusableScaledCanvas ?: Canvas(scaled).also { reusableScaledCanvas = it }
            scaleSrcRect.set(0, 0, bitmap.width, bitmap.height)
            scaleDstRect.set(0, 0, inputWidth, inputHeight)
            canvas.drawBitmap(bitmap, scaleSrcRect, scaleDstRect, scalePaint)
            scaled
        } else {
            bitmap
        }

        sourceBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        tensorBuffer.rewind()
        val pixelCount = inputWidth * inputHeight
        if (isQuantizedUInt8) {
            for (i in 0 until pixelCount) {
                val pixel = intValues[i]
                tensorBuffer.put(((pixel shr 16) and 0xFF).toByte())
                tensorBuffer.put(((pixel shr 8) and 0xFF).toByte())
                tensorBuffer.put((pixel and 0xFF).toByte())
            }
        } else {
            for (i in 0 until pixelCount) {
                val pixel = intValues[i]
                tensorBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                tensorBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                tensorBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        }

        tensorBuffer.rewind()
        return tensorBuffer
    }

    fun reset() {
        hasProcessedFrame = false
        lastSampleLumas.fill(0)
    }

    fun recyclePools() {
        reset()
        reusableRowBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableRowBitmap = null
        reusableCroppedBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableCroppedBitmap = null
        reusableScaledCanvas = null
        reusableScaledBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableScaledBitmap = null
    }

    companion object {
        private const val LUMA_GRID_SIZE = 5
        private const val MAX_AVG_LUMA_DELTA = 4
        private const val MAX_PEAK_LUMA_DELTA = 14
    }
}
