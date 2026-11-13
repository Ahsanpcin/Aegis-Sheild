package com.example.detection.preprocessing

import android.graphics.Bitmap
import android.media.Image
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    // Hash/fingerprint of previously inspected frame to avoid re-processing identical video/static frames
    private var hasProcessedFrame: Boolean = false
    private var lastSampleHash: Long = 0L

    fun configure(width: Int, height: Int, quantizedUInt8: Boolean) {
        if (this.inputWidth == width && this.inputHeight == height && this.isQuantizedUInt8 == quantizedUInt8) {
            return
        }
        this.inputWidth = width
        this.inputHeight = height
        this.isQuantizedUInt8 = quantizedUInt8
        this.tensorBuffer = allocateBuffer(width, height, quantizedUInt8)
        this.intValues = IntArray(width * height)
    }

    private fun allocateBuffer(width: Int, height: Int, quantizedUInt8: Boolean): ByteBuffer {
        val bytesPerChannel = if (quantizedUInt8) 1 else 4
        return ByteBuffer.allocateDirect(1 * width * height * 3 * bytesPerChannel).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Extracts a downsampled ARGB Bitmap from an ImageReader Image buffer.
     * Accurately handles rowStride padding in ImageReader plane.
     */
    fun extractBitmapFromImage(image: Image): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null

        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            if (cropped != bitmap) {
                bitmap.recycle()
            }
            cropped
        } else {
            bitmap
        }
    }

    /**
     * Quickly checks whether the newly captured frame is identical or nearly identical
     * to the previous frame (e.g. static screen or paused video) to skip duplicate inference.
     */
    fun isFrameDuplicate(bitmap: Bitmap): Boolean {
        var hash = 17L
        val w = bitmap.width
        val h = bitmap.height
        val stepX = (w / 4).coerceAtLeast(1)
        val stepY = (h / 4).coerceAtLeast(1)

        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                hash = 31 * hash + bitmap.getPixel(x, y)
            }
        }

        if (!hasProcessedFrame) {
            hasProcessedFrame = true
            lastSampleHash = hash
            return false
        }

        val isDuplicate = (hash == lastSampleHash)
        lastSampleHash = hash
        return isDuplicate
    }

    /**
     * Scales bitmap to model input dimensions [inputWidth x inputHeight] and fills
     * the direct ByteBuffer with either normalized Float32 RGB [0.0f..1.0f] or UInt8 [0..255].
     */
    fun preprocessBitmapToTensorBuffer(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        } else {
            bitmap
        }

        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

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
        lastSampleHash = 0L
    }
}
