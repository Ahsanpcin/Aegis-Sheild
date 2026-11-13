package com.example.core.models

data class SecurityIncident(
    val id: Long = 0,
    val timestampMillis: Long,
    val packageName: String,
    val appDisplayName: String,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val confidenceScore: Float,
    val actionTaken: String,
    val diagnosticSummary: String = "",
    val lockdownDurationMinutes: Int = 0,
    val isReviewedFalsePositive: Boolean = false
)
