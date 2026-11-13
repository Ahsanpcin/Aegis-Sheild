package com.example.detection.classifier

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.SystemClock
import android.util.Log
import com.example.core.models.ThreatCategory
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteContentClassifier(
    private val context: Context,
    private val modelManager: ModelManager? = null,
    private val assetName: String = ModelManager.BUNDLED_ASSET_NAME
) : ContentClassifier {

    private var interpreter: Interpreter? = null

    override var isModelReady: Boolean = false
        private set

    override var inputWidth: Int = 224
        private set

    override var inputHeight: Int = 224
        private set

    override var isQuantizedInput: Boolean = false
        private set

    override var outputClassCount: Int = 0
        private set

    private var outputDataType: DataType = DataType.FLOAT32
    private var outputScale: Float = 1.0f
    private var outputZeroPoint: Int = 0

    var lastErrorMessage: String? = null
        private set

    override val modelSpecification: String
        get() = if (isModelReady) {
            "TFLite Active • Input: [1, $inputHeight, $inputWidth, 3] ${if (isQuantizedInput) "UInt8" else "Float32"} • Output: [1, $outputClassCount]"
        } else {
            "No TFLite Model Loaded • Supported: [1, H, W, 3] Float32/UInt8 -> [1, 2|3|5] Classes"
        }

    init {
        reloadModel()
    }

    @Synchronized
    override fun reloadModel(): Boolean {
        close()
        lastErrorMessage = null
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(2)
                setUseNNAPI(false) // Safe CPU execution on Android 7.0
            }

            val importedFile = modelManager?.activeModelFile
            val createdInterpreter: Interpreter? = when {
                importedFile != null && importedFile.exists() && importedFile.length() > 0L -> {
                    Log.i(TAG, "Loading user-imported TFLite model from ${importedFile.absolutePath}")
                    Interpreter(importedFile, options)
                }
                else -> {
                    val assetBuffer = loadAssetModelFile(context, assetName)
                    if (assetBuffer != null) {
                        Log.i(TAG, "Loading bundled TFLite asset model: $assetName")
                        Interpreter(assetBuffer, options)
                    } else {
                        null
                    }
                }
            }

            if (createdInterpreter == null) {
                isModelReady = false
                lastErrorMessage = "No .tflite model file loaded. Pick a .tflite model from phone storage."
                Log.w(TAG, lastErrorMessage!!)
                return false
            }

            val inTensor = createdInterpreter.getInputTensor(0)
            val inShape = inTensor.shape()
            val inType = inTensor.dataType()

            if (inShape.size != 4 || inShape[0] != 1 || inShape[3] != 3) {
                createdInterpreter.close()
                isModelReady = false
                lastErrorMessage = "Incompatible input tensor ${inShape.contentToString()} (expected [1, H, W, 3])."
                return false
            }

            val outTensor = createdInterpreter.getOutputTensor(0)
            val outShape = outTensor.shape()
            val classes = outShape.lastOrNull() ?: 0
            if (classes !in setOf(2, 3, 5)) {
                createdInterpreter.close()
                isModelReady = false
                lastErrorMessage = "Unsupported output classes: $classes (supported: 2, 3, or 5)."
                return false
            }

            inputHeight = inShape[1]
            inputWidth = inShape[2]
            isQuantizedInput = (inType == DataType.UINT8)
            outputClassCount = classes
            outputDataType = outTensor.dataType()
            outputScale = outTensor.quantizationParams().scale
            outputZeroPoint = outTensor.quantizationParams().zeroPoint

            interpreter = createdInterpreter
            isModelReady = true
            Log.i(TAG, "TFLite ready: ${inputWidth}x${inputHeight} (${if (isQuantizedInput) "UInt8" else "Float32"}), $outputClassCount output classes.")
            return true
        } catch (e: Exception) {
            isModelReady = false
            lastErrorMessage = "Failed to load TFLite model: ${e.message ?: "Unknown error"}"
            Log.e(TAG, lastErrorMessage!!, e)
            return false
        }
    }

    @Synchronized
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
                diagnosticInfo = lastErrorMessage ?: "No TFLite model loaded"
            )
        }

        val startTime = SystemClock.uptimeMillis()
        tensorBuffer.rewind()

        val probabilities: FloatArray = if (outputDataType == DataType.UINT8) {
            val rawOut = Array(1) { ByteArray(outputClassCount) }
            tflite.run(tensorBuffer, rawOut)
            FloatArray(outputClassCount) { idx ->
                val unsignedVal = rawOut[0][idx].toInt() and 0xFF
                val scale = if (outputScale > 0f) outputScale else (1f / 255f)
                ((unsignedVal - outputZeroPoint) * scale).coerceIn(0f, 1f)
            }
        } else {
            val floatOut = Array(1) { FloatArray(outputClassCount) }
            tflite.run(tensorBuffer, floatOut)
            val raw = floatOut[0]
            if (raw.any { it < -0.001f || it > 1.001f }) {
                softmax(raw)
            } else {
                FloatArray(outputClassCount) { idx -> raw[idx].coerceIn(0f, 1f) }
            }
        }

        val latency = SystemClock.uptimeMillis() - startTime
        return mapProbabilities(probabilities, latency)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size) { i -> kotlin.math.exp((logits[i] - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(1e-7f)
        return FloatArray(logits.size) { i -> (exps[i] / sum).coerceIn(0f, 1f) }
    }

    private fun mapProbabilities(probs: FloatArray, latency: Long): ClassificationResult {
        val safe: Float
        val suggestive: Float
        val explicit: Float
        val rawMap: Map<String, Float>

        when (probs.size) {
            2 -> {
                // 2-class: [0]=SAFE, [1]=NSFW/EXPLICIT
                safe = probs[0]
                suggestive = 0.0f
                explicit = probs[1]
                rawMap = mapOf("Safe" to probs[0], "NSFW / Explicit" to probs[1])
            }
            3 -> {
                // 3-class: [0]=SAFE, [1]=SUGGESTIVE, [2]=EXPLICIT
                safe = probs[0]
                suggestive = probs[1]
                explicit = probs[2]
                rawMap = mapOf("Safe" to probs[0], "Suggestive" to probs[1], "Explicit" to probs[2])
            }
            5 -> {
                // Standard 5-class NSFW MobileNet/Inception: [Drawings, Hentai, Neutral, Porn, Sexy]
                val drawings = probs[0]
                val hentai = probs[1]
                val neutral = probs[2]
                val porn = probs[3]
                val sexy = probs[4]

                safe = (drawings + neutral).coerceIn(0f, 1f)
                suggestive = sexy.coerceIn(0f, 1f)
                explicit = (hentai + porn).coerceIn(0f, 1f)
                rawMap = mapOf(
                    "Drawings" to drawings,
                    "Hentai" to hentai,
                    "Neutral" to neutral,
                    "Porn" to porn,
                    "Sexy" to sexy
                )
            }
            else -> {
                safe = 1.0f
                suggestive = 0.0f
                explicit = 0.0f
                rawMap = emptyMap()
            }
        }

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
            outputClassCount = probs.size,
            rawClassScores = rawMap,
            diagnosticInfo = "Inference in ${latency}ms (${probs.size}-Class) • Safe: ${(safe * 100).toInt()}%, Sugg: ${(suggestive * 100).toInt()}%, Expl: ${(explicit * 100).toInt()}%"
        )
    }

    @Synchronized
    override fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {
        }
        interpreter = null
        isModelReady = false
    }

    private fun loadAssetModelFile(context: Context, assetName: String): MappedByteBuffer? {
        return try {
            val fileDescriptor: AssetFileDescriptor = context.assets.openFd(assetName)
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
            }
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private const val TAG = "TFLiteClassifier"
    }
}
