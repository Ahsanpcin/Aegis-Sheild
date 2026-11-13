package com.example.data.repository

import android.content.Context
import com.example.core.models.EngineTelemetry
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.detection.classifier.ActiveModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProtectionRepository(private val context: Context) {

    private val _telemetry = MutableStateFlow(
        EngineTelemetry(
            isProtectionEnabled = false,
            isMediaProjectionActive = false,
            isAccessibilityActive = false,
            isOverlayPermitted = PermissionHelper.hasOverlayPermission(context),
            framesProcessedCount = 0L,
            currentInspectionFps = 0.0f,
            statusMessage = "Protection Disengaged (Standby)"
        )
    )
    val telemetry: StateFlow<EngineTelemetry> = _telemetry.asStateFlow()

    private val _activeAppLabel = MutableStateFlow("Active Screen")
    val activeAppLabel: StateFlow<String> = _activeAppLabel.asStateFlow()

    fun refreshPermissionHealth() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        _telemetry.update { current ->
            current.copy(
                isOverlayPermitted = hasOverlay,
                statusMessage = buildStatusMessage(
                    isEnabled = current.isProtectionEnabled,
                    hasOverlay = hasOverlay,
                    isModelLoaded = current.isModelLoaded
                )
            )
        }
    }

    fun updateModelInfo(info: ActiveModelInfo) {
        _telemetry.update { current ->
            current.copy(
                isModelLoaded = info.isLoaded,
                loadedModelName = info.displayName,
                modelInputSpec = info.inputSpecLabel,
                modelOutputClasses = info.outputClassCount,
                modelStatusMessage = info.statusMessage,
                modelErrorMessage = info.errorMessage,
                statusMessage = buildStatusMessage(
                    isEnabled = current.isProtectionEnabled,
                    hasOverlay = current.isOverlayPermitted,
                    isModelLoaded = info.isLoaded
                )
            )
        }
    }

    fun setProtectionEnabled(enabled: Boolean): Boolean {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        if (enabled && !hasOverlay) {
            _telemetry.update {
                it.copy(
                    isProtectionEnabled = false,
                    isOverlayPermitted = false,
                    statusMessage = "Cannot engage: Overlay permission required"
                )
            }
            return false
        }

        _telemetry.update { current ->
            current.copy(
                isProtectionEnabled = enabled,
                isOverlayPermitted = hasOverlay,
                statusMessage = buildStatusMessage(
                    isEnabled = enabled,
                    hasOverlay = hasOverlay,
                    isModelLoaded = current.isModelLoaded
                )
            )
        }
        return true
    }

    fun setServiceRunning(isRunning: Boolean) {
        _telemetry.update { current ->
            current.copy(
                isProtectionEnabled = isRunning,
                statusMessage = buildStatusMessage(
                    isEnabled = isRunning,
                    hasOverlay = current.isOverlayPermitted,
                    isModelLoaded = current.isModelLoaded
                )
            )
        }
    }

    fun updateMediaProjectionState(isActive: Boolean) {
        _telemetry.update { it.copy(isMediaProjectionActive = isActive) }
    }

    fun updateAccessibilityState(isActive: Boolean) {
        _telemetry.update { it.copy(isAccessibilityActive = isActive) }
    }

    fun updateActivePackage(packageName: String?, appLabel: String = "Active Screen") {
        _activeAppLabel.value = appLabel
        _telemetry.update { it.copy(activeMonitoredPackage = packageName) }
    }

    fun recordInspectionMetrics(frameCount: Long, fps: Float) {
        _telemetry.update {
            it.copy(
                framesProcessedCount = frameCount,
                currentInspectionFps = fps
            )
        }
    }

    fun recordDetection(category: ThreatCategory) {
        _telemetry.update {
            it.copy(
                lastDetectionTimestamp = System.currentTimeMillis(),
                lastDetectedCategory = category
            )
        }
    }

    private fun buildStatusMessage(isEnabled: Boolean, hasOverlay: Boolean, isModelLoaded: Boolean): String {
        return when {
            !isEnabled -> "Protection Disengaged (Standby)"
            !hasOverlay -> "Warning: Overlay Permission Missing"
            !isModelLoaded -> "Screen Capture Active • Model Missing (.tflite)"
            else -> "Visual Guardian Armed • AI Scanning Active"
        }
    }
}
