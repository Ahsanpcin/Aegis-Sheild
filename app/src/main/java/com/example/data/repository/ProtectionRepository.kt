package com.example.data.repository

import android.content.Context
import com.example.core.models.EngineTelemetry
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
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
                statusMessage = if (current.isProtectionEnabled) {
                    if (hasOverlay) "Protection Armed • Scanning Active" else "Warning: Overlay Permission Missing"
                } else {
                    "Protection Disengaged (Standby)"
                }
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
                statusMessage = if (enabled) {
                    "Protection Armed • Monitoring Ready"
                } else {
                    "Protection Disengaged (Standby)"
                }
            )
        }
        return true
    }

    fun setServiceRunning(isRunning: Boolean) {
        _telemetry.update { current ->
            current.copy(
                isProtectionEnabled = isRunning,
                statusMessage = if (isRunning) "Visual Guardian Armed • Scanning Stream" else "Protection Disengaged (Standby)"
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
}
