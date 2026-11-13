package com.example.detection.decision

import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.detection.classifier.ClassificationResult

data class DecisionOutcome(
    val shouldBlock: Boolean,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val confidence: Float,
    val reason: String
)

class ThreatDecisionEngine {

    // Consecutive violation counter to filter out transient single-frame camera/animation glitches
    private var consecutiveSuspiciousCount: Int = 0
    private var lastSuspectCategory: ThreatCategory = ThreatCategory.BENIGN_SAFE

    /**
     * Evaluates a classification result against the active sensitivity policy and strict everything flag.
     */
    fun evaluate(
        result: ClassificationResult,
        sensitivity: SensitivityLevel,
        isStrictEverything: Boolean,
        explicitThresholdOverride: Float? = null,
        suggestiveThresholdOverride: Float? = null,
        consecutiveFramesRequired: Int = 2
    ): DecisionOutcome {
        val explicitThreshold = explicitThresholdOverride ?: sensitivity.threshold
        val suggestiveThreshold = suggestiveThresholdOverride ?: sensitivity.threshold
        val requiredFrames = consecutiveFramesRequired.coerceIn(1, 5)
        val explicit = result.explicitScore
        val suggestive = result.suggestiveScore
        val safe = result.safeScore

        // 1. Explicit Adult Content: Immediate critical threat
        if (explicit >= explicitThreshold || (isStrictEverything && explicit >= 0.50f)) {
            consecutiveSuspiciousCount = 0
            return DecisionOutcome(
                shouldBlock = true,
                category = ThreatCategory.EXPLICIT_ADULT,
                severity = ThreatSeverity.CRITICAL,
                confidence = explicit,
                reason = "Explicit adult visual content detected with ${(explicit * 100).toInt()}% confidence"
            )
        }

        // 2. Suggestive Adult / Provocative Content:
        if (suggestive >= suggestiveThreshold || (isStrictEverything && suggestive >= 0.55f)) {
            if (isStrictEverything || suggestive >= 0.85f || requiredFrames <= 1) {
                // High confidence suggestive, Strict Everything enabled, or 1-frame immediate confirmation
                consecutiveSuspiciousCount = 0
                return DecisionOutcome(
                    shouldBlock = true,
                    category = ThreatCategory.SUGGESTIVE_CONTENT,
                    severity = ThreatSeverity.HIGH,
                    confidence = suggestive,
                    reason = "Suggestive visual content identified with ${(suggestive * 100).toInt()}% confidence"
                )
            } else {
                // Borderline suggestive: require configured consecutive frame confirmation
                consecutiveSuspiciousCount++
                if (consecutiveSuspiciousCount >= requiredFrames) {
                    consecutiveSuspiciousCount = 0
                    return DecisionOutcome(
                        shouldBlock = true,
                        category = ThreatCategory.SUGGESTIVE_CONTENT,
                        severity = ThreatSeverity.MEDIUM,
                        confidence = suggestive,
                        reason = "Consecutive suggestive frames ($requiredFrames) confirmed policy violation"
                    )
                }
            }
        } else {
            // Clean frame: reset consecutive counter
            consecutiveSuspiciousCount = 0
        }

        return DecisionOutcome(
            shouldBlock = false,
            category = ThreatCategory.BENIGN_SAFE,
            severity = ThreatSeverity.LOW,
            confidence = safe,
            reason = "Content approved safe (Safe score: ${(safe * 100).toInt()}%)"
        )
    }

    fun reset() {
        consecutiveSuspiciousCount = 0
        lastSuspectCategory = ThreatCategory.BENIGN_SAFE
    }
}
