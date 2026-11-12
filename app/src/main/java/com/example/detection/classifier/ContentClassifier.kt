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
    val diagnosticInfo: String
)

interface ContentClassifier {
    val isModelReady: Boolean
    val modelSpecification: String

    fun classifyTensorBuffer(tensorBuffer: ByteBuffer): ClassificationResult
    fun close()
}
