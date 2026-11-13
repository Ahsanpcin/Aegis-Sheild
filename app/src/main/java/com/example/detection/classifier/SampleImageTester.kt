package com.example.detection.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.core.models.SensitivityLevel
import com.example.detection.decision.DecisionOutcome
import com.example.detection.decision.ThreatDecisionEngine
import com.example.detection.preprocessing.FramePreprocessor

data class SampleTestResult(
    val previewBitmap: Bitmap,
    val originalDimensions: String,
    val classification: ClassificationResult,
    val decision: DecisionOutcome
)

object SampleImageTester {

    fun testImageUri(
        context: Context,
        uri: Uri,
        modelManager: ModelManager,
        sensitivity: SensitivityLevel,
        isStrictEverything: Boolean
    ): Result<SampleTestResult> {
        val classifier = TFLiteContentClassifier(context, modelManager)
        return try {
            if (!classifier.isModelReady) {
                return Result.failure(IllegalStateException(classifier.lastErrorMessage ?: "No .tflite model loaded."))
            }

            // First pass: decode bounds only to prevent OOM on high-res photos
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return Result.failure(IllegalStateException("Unable to read selected image."))

            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) {
                return Result.failure(IllegalStateException("Selected file is not a valid image."))
            }

            val targetMaxDim = maxOf(classifier.inputWidth, classifier.inputHeight).coerceAtLeast(320)
            var sampleSize = 1
            while ((origW / sampleSize) > targetMaxDim * 2 || (origH / sampleSize) > targetMaxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return Result.failure(IllegalStateException("Failed to decode selected image."))

            val preprocessor = FramePreprocessor(
                inputWidth = classifier.inputWidth,
                inputHeight = classifier.inputHeight,
                isQuantizedUInt8 = classifier.isQuantizedInput
            )
            val tensorBuffer = preprocessor.preprocessBitmapToTensorBuffer(decodedBitmap)
            val classification = classifier.classifyTensorBuffer(tensorBuffer)

            val engine = ThreatDecisionEngine()
            val decision = engine.evaluate(classification, sensitivity, isStrictEverything)

            // Create compact preview thumbnail for UI display (never saved to disk)
            val previewWidth = 280
            val previewHeight = ((decodedBitmap.height.toFloat() / decodedBitmap.width.coerceAtLeast(1)) * previewWidth)
                .toInt()
                .coerceIn(120, 360)
            val preview = Bitmap.createScaledBitmap(decodedBitmap, previewWidth, previewHeight, true)
            if (preview != decodedBitmap) {
                decodedBitmap.recycle()
            }

            Result.success(
                SampleTestResult(
                    previewBitmap = preview,
                    originalDimensions = "${origW}x${origH}",
                    classification = classification,
                    decision = decision
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            classifier.close()
        }
    }
}
