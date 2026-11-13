package com.example.core.models

data class EngineTelemetry(
    val isProtectionEnabled: Boolean = false,
    val isMediaProjectionActive: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val isOverlayPermitted: Boolean = false,
    val framesProcessedCount: Long = 0L,
    val currentInspectionFps: Float = 0.0f,
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
)
