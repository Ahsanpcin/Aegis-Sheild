package com.example.detection.classifier

import com.example.core.models.ThreatCategory
import java.nio.ByteBuffer

data class ClassificationResult(
    val safeScore: Float,
    val suggestiveScore: Float,
    val explicitScore: Float,
    val topCategory: ThreatCategory,
    val confidence: Float,
    val isModelLoaded: Boolean,
    val latencyMs: Long,
    val diagnosticInfo: String,
    val outputClassCount: Int = 0,
    val rawClassScores: Map<String, Float> = emptyMap()
)

interface ContentClassifier {
    val isModelReady: Boolean
    val modelSpecification: String
    val inputWidth: Int
    val inputHeight: Int
    val isQuantizedInput: Boolean
    val outputClassCount: Int

    fun classifyTensorBuffer(tensorBuffer: ByteBuffer): ClassificationResult
    fun reloadModel(): Boolean
    fun close()
}
