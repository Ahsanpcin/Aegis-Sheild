package com.example.detection.classifier

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.SystemClock
import android.util.Log
import com.example.core.models.ThreatCategory
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteContentClassifier(
    private val context: Context,
    private val modelAssetPath: String = "aegis_classifier.tflite"
) : ContentClassifier {

    private var interpreter: Interpreter? = null

    override var isModelReady: Boolean = false
        private set

    override val modelSpecification: String =
        "TensorFlow Lite Standard • Input: [1, 224, 224, 3] Float32 RGB • Output: [1, 3] Float32 Softmax (0: SAFE, 1: SUGGESTIVE, 2: EXPLICIT)"

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelFile(context, modelAssetPath)
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(2)
                    setUseNNAPI(false) // Safe CPU execution on Android 7.0
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelReady = true
                Log.i(TAG, "TensorFlow Lite content classification model successfully loaded from assets: $modelAssetPath")
            } else {
                isModelReady = false
                Log.w(TAG, "Model file '$modelAssetPath' not found in assets. TFLite pipeline primed for model asset.")
            }
        } catch (e: Exception) {
            isModelReady = false
            Log.e(TAG, "Failed to initialize TensorFlow Lite Interpreter for $modelAssetPath", e)
        }
    }

    override fun classifyTensorBuffer(tensorBuffer: ByteBuffer): ClassificationResult {
        val tflite = interpreter
        if (tflite == null || !isModelReady) {
            return ClassificationResult(
                safeScore = 1.0f,
                suggestiveScore = 0.0f,
                explicitScore = 0.0f,
                topCategory = ThreatCategory.BENIGN_SAFE,
                confidence = 1.0f,
                isModelLoaded = false,
                latencyMs = 0L,
                diagnosticInfo = "TFLite Model Pending: Place '$modelAssetPath' with spec $modelSpecification"
            )
        }

        val startTime = SystemClock.uptimeMillis()
        val outputScores = Array(1) { FloatArray(3) }

        tensorBuffer.rewind()
        tflite.run(tensorBuffer, outputScores)

        val latency = SystemClock.uptimeMillis() - startTime
        val safe = outputScores[0][0]
        val suggestive = outputScores[0][1]
        val explicit = outputScores[0][2]

        val topCategory = when {
            explicit >= suggestive && explicit >= safe -> ThreatCategory.EXPLICIT_ADULT
            suggestive >= safe -> ThreatCategory.SUGGESTIVE_CONTENT
            else -> ThreatCategory.BENIGN_SAFE
        }

        val maxConfidence = maxOf(safe, suggestive, explicit)

        return ClassificationResult(
            safeScore = safe,
            suggestiveScore = suggestive,
            explicitScore = explicit,
            topCategory = topCategory,
            confidence = maxConfidence,
            isModelLoaded = true,
            latencyMs = latency,
            diagnosticInfo = "Inference complete in ${latency}ms (Safe: ${(safe * 100).toInt()}%, Sugg: ${(suggestive * 100).toInt()}%, Expl: ${(explicit * 100).toInt()}%)"
        )
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        isModelReady = false
    }

    private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer? {
        return try {
            val fileDescriptor: AssetFileDescriptor = context.assets.openFd(assetName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private const val TAG = "TFLiteClassifier"
    }
}
