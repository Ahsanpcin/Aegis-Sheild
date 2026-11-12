package com.example.detection.preprocessing

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FramePreprocessor(
    val inputWidth: Int = 224,
    val inputHeight: Int = 224
) {
    // Reusable direct byte buffer for TFLite input tensor [1, 224, 224, 3] Float32 (4 bytes per float)
    private val tensorBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }

    private val intValues = IntArray(inputWidth * inputHeight)

    // Hash/fingerprint of previously inspected frame to avoid re-processing identical video/static frames
    private var hasProcessedFrame: Boolean = false
    private var lastSampleHash: Long = 0L

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

        // If rowPadding exists, crop to exact image width/height
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
     * the direct ByteBuffer with normalized RGB values (0.0f..1.0f).
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
        // Convert [ARGB_8888] -> normalized RGB [0.0f .. 1.0f]
        for (i in 0 until inputWidth * inputHeight) {
            val pixel = intValues[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            tensorBuffer.putFloat(r)
            tensorBuffer.putFloat(g)
            tensorBuffer.putFloat(b)
        }

        return tensorBuffer
    }

    fun reset() {
        hasProcessedFrame = false
        lastSampleHash = 0L
    }
}
