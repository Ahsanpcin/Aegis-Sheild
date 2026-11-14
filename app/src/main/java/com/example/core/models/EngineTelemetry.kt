package com.example.core.models

data class EngineTelemetry(
    val isProtectionEnabled: Boolean = false,
    val isMediaProjectionActive: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val isOverlayPermitted: Boolean = false,
    val framesProcessedCount: Long = 0L,
    val framesSkippedCount: Long = 0L,
    val currentInspectionFps: Float = 0.0f,
    val effectiveIntervalMs: Int = 350,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveModeActive: Boolean = false,
    val isLowBatteryThrottled: Boolean = false,
    val lastDetectionTimestamp: Long? = null,
    val lastDetectedCategory: ThreatCategory? = null,
    val activeMonitoredPackage: String? = null,
    val statusMessage: String = "Protection Disengaged (Standby)",
    val isModelLoaded: Boolean = false,
    val loadedModelName: String? = null,
    val modelInputSpec: String = "Not Loaded",
    val modelOutputClasses: Int = 0,
    val modelStatusMessage: String = "No .tflite model loaded",
    val modelErrorMessage: String? = null
) {
    fun ecoSavedPercent(): Int {
        val total = framesProcessedCount + framesSkippedCount
        return if (total <= 0L) 0 else ((framesSkippedCount * 100L) / total).toInt().coerceIn(0, 99)
    }
}
