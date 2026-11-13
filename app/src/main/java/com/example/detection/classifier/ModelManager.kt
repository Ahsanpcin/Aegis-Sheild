package com.example.detection.classifier

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.preferences.AegisPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream

data class ActiveModelInfo(
    val isLoaded: Boolean,
    val displayName: String?,
    val sourceLabel: String,
    val inputWidth: Int = 224,
    val inputHeight: Int = 224,
    val isQuantizedInput: Boolean = false,
    val outputClassCount: Int = 0,
    val inputSpecLabel: String = "Not Loaded",
    val statusMessage: String = "No .tflite model loaded",
    val errorMessage: String? = null
)

sealed class ModelImportResult {
    data class Success(val info: ActiveModelInfo) : ModelImportResult()
    data class Failure(val reason: String) : ModelImportResult()
}

class ModelManager(
    private val context: Context,
    private val preferences: AegisPreferences
) {
    private val modelsDir: File = File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    val activeModelFile: File
        get() = File(modelsDir, ACTIVE_MODEL_FILENAME)

    private val _modelVersion = MutableStateFlow(0)
    val modelVersion: StateFlow<Int> = _modelVersion.asStateFlow()

    private val _activeModelInfo = MutableStateFlow(inspectCurrentModel())
    val activeModelInfo: StateFlow<ActiveModelInfo> = _activeModelInfo.asStateFlow()

    fun hasBundledAssetModel(): Boolean {
        return try {
            context.assets.openFd(BUNDLED_ASSET_NAME).use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun inspectCurrentModel(): ActiveModelInfo {
        val file = activeModelFile
        if (file.exists() && file.length() > 0L) {
            val displayName = preferences.importedModelName.value ?: "Imported Model (.tflite)"
            val inspected = validateModelFile(file, displayName, "Phone Storage")
            if (inspected is ModelImportResult.Success) {
                return inspected.info
            } else if (inspected is ModelImportResult.Failure) {
                return ActiveModelInfo(
                    isLoaded = false,
                    displayName = displayName,
                    sourceLabel = "Phone Storage",
                    statusMessage = "Model Error",
                    errorMessage = inspected.reason
                )
            }
        }

        if (hasBundledAssetModel()) {
            val classifier = TFLiteContentClassifier(context, this)
            return try {
                if (classifier.isModelReady) {
                    ActiveModelInfo(
                        isLoaded = true,
                        displayName = BUNDLED_ASSET_NAME,
                        sourceLabel = "App Assets",
                        inputWidth = classifier.inputWidth,
                        inputHeight = classifier.inputHeight,
                        isQuantizedInput = classifier.isQuantizedInput,
                        outputClassCount = classifier.outputClassCount,
                        inputSpecLabel = "${classifier.inputWidth}x${classifier.inputHeight} ${if (classifier.isQuantizedInput) "UInt8" else "Float32"}",
                        statusMessage = "Model Ready (${classifier.outputClassCount}-Class)"
                    )
                } else {
                    ActiveModelInfo(
                        isLoaded = false,
                        displayName = BUNDLED_ASSET_NAME,
                        sourceLabel = "App Assets",
                        statusMessage = "Model Error",
                        errorMessage = classifier.lastErrorMessage ?: "Unable to initialize bundled asset model"
                    )
                }
            } finally {
                classifier.close()
            }
        }

        return ActiveModelInfo(
            isLoaded = false,
            displayName = null,
            sourceLabel = "None",
            inputSpecLabel = "Not Loaded",
            statusMessage = "No .tflite model loaded",
            errorMessage = null
        )
    }

    fun refreshStatus(): ActiveModelInfo {
        val info = inspectCurrentModel()
        _activeModelInfo.value = info
        return info
    }

    fun importModelFromUri(uri: Uri): ModelImportResult {
        val displayName = resolveDisplayName(uri) ?: "custom_model.tflite"
        if (!displayName.endsWith(".tflite", ignoreCase = true)) {
            Log.w(TAG, "Importing file without .tflite extension: $displayName (will still validate header/Interpreter)")
        }

        val stagingFile = File(modelsDir, "staging_classifier.tflite")
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ModelImportResult.Failure("Could not open selected file stream.")

            inputStream.use { input ->
                FileOutputStream(stagingFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            if (!stagingFile.exists() || stagingFile.length() < 1024L) {
                stagingFile.delete()
                return ModelImportResult.Failure("Selected file is empty or too small to be a valid TFLite model.")
            }

            val validation = validateModelFile(stagingFile, displayName, "Phone Storage")
            if (validation is ModelImportResult.Failure) {
                stagingFile.delete()
                return validation
            }

            val target = activeModelFile
            if (target.exists()) {
                target.delete()
            }
            if (!stagingFile.renameTo(target)) {
                stagingFile.copyTo(target, overwrite = true)
                stagingFile.delete()
            }

            preferences.setImportedModelName(displayName)
            val updatedInfo = (validation as ModelImportResult.Success).info
            _activeModelInfo.value = updatedInfo
            _modelVersion.value += 1
            Log.i(TAG, "Successfully imported TFLite model: $displayName (${updatedInfo.inputSpecLabel}, ${updatedInfo.outputClassCount} classes)")
            return ModelImportResult.Success(updatedInfo)
        } catch (e: Exception) {
            stagingFile.delete()
            Log.e(TAG, "Model import failed for $uri", e)
            return ModelImportResult.Failure("Import failed: ${e.message ?: "Unknown I/O error"}")
        }
    }

    fun clearImportedModel() {
        if (activeModelFile.exists()) {
            activeModelFile.delete()
        }
        preferences.setImportedModelName(null)
        _activeModelInfo.value = inspectCurrentModel()
        _modelVersion.value += 1
    }

    private fun validateModelFile(file: File, displayName: String, sourceLabel: String): ModelImportResult {
        var tempInterpreter: Interpreter? = null
        return try {
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            tempInterpreter = Interpreter(file, options)

            if (tempInterpreter.inputTensorCount < 1 || tempInterpreter.outputTensorCount < 1) {
                return ModelImportResult.Failure("Model must have at least 1 input and 1 output tensor.")
            }

            val inTensor = tempInterpreter.getInputTensor(0)
            val inShape = inTensor.shape()
            val inType = inTensor.dataType()

            if (inShape.size != 4 || inShape[0] != 1 || inShape[3] != 3) {
                return ModelImportResult.Failure(
                    "Unsupported input shape ${inShape.contentToString()}. Expected [1, Height, Width, 3] RGB image tensor."
                )
            }

            val height = inShape[1]
            val width = inShape[2]
            if (height !in 32..1024 || width !in 32..1024) {
                return ModelImportResult.Failure("Unsupported input image dimensions ${width}x${height}.")
            }

            if (inType != DataType.FLOAT32 && inType != DataType.UINT8) {
                return ModelImportResult.Failure(
                    "Unsupported input data type $inType. Expected FLOAT32 or UINT8."
                )
            }

            val outTensor = tempInterpreter.getOutputTensor(0)
            val outShape = outTensor.shape()
            val outType = outTensor.dataType()

            val classCount = outShape.lastOrNull() ?: 0
            if (classCount !in setOf(2, 3, 5)) {
                return ModelImportResult.Failure(
                    "Unsupported output shape ${outShape.contentToString()}. Supported class counts are 2 (Safe/NSFW), 3 (Safe/Suggestive/Explicit), or 5 (Drawings/Hentai/Neutral/Porn/Sexy)."
                )
            }

            if (outType != DataType.FLOAT32 && outType != DataType.UINT8) {
                return ModelImportResult.Failure("Unsupported output data type $outType.")
            }

            val isQuantized = (inType == DataType.UINT8)
            val typeLabel = if (isQuantized) "UInt8" else "Float32"

            ModelImportResult.Success(
                ActiveModelInfo(
                    isLoaded = true,
                    displayName = displayName,
                    sourceLabel = sourceLabel,
                    inputWidth = width,
                    inputHeight = height,
                    isQuantizedInput = isQuantized,
                    outputClassCount = classCount,
                    inputSpecLabel = "${width}x${height} $typeLabel",
                    statusMessage = "Model Ready ($classCount-Class)"
                )
            )
        } catch (e: Exception) {
            ModelImportResult.Failure("Invalid TFLite file: ${e.message ?: "Cannot initialize Interpreter"}")
        } finally {
            try {
                tempInterpreter?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    companion object {
        private const val TAG = "ModelManager"
        const val ACTIVE_MODEL_FILENAME = "active_classifier.tflite"
        const val BUNDLED_ASSET_NAME = "aegis_classifier.tflite"
    }
}
