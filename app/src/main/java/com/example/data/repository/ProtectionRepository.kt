package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import com.example.core.models.EngineTelemetry
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.detection.classifier.ActiveModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProtectionRepository(private val context: Context) {

    private val _telemetry = MutableStateFlow(createInitialTelemetry(context))
    val telemetry: StateFlow<EngineTelemetry> = _telemetry.asStateFlow()

    private val _activeAppLabel = MutableStateFlow("Active Screen")
    val activeAppLabel: StateFlow<String> = _activeAppLabel.asStateFlow()

    @Volatile
    var lastContextChangeUptimeMs: Long = 0L
        private set

    fun notifyScreenContextChanged() {
        lastContextChangeUptimeMs = SystemClock.uptimeMillis()
    }

    fun refreshPermissionHealth() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode == true
        val (pct, charging) = readBatterySnapshot(context)

        _telemetry.update { current ->
            current.copy(
                isOverlayPermitted = hasOverlay,
                batteryPercent = pct,
                isCharging = charging,
                isPowerSaveModeActive = isPowerSave,
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
        notifyScreenContextChanged()
        _telemetry.update { it.copy(activeMonitoredPackage = packageName) }
    }

    fun recordInspectionMetrics(
        frameCount: Long,
        fps: Float,
        skippedCount: Long? = null,
        effectiveIntervalMs: Int? = null
    ) {
        _telemetry.update { current ->
            current.copy(
                framesProcessedCount = frameCount,
                framesSkippedCount = skippedCount ?: current.framesSkippedCount,
                currentInspectionFps = fps,
                effectiveIntervalMs = effectiveIntervalMs ?: current.effectiveIntervalMs
            )
        }
    }

    fun updateBatteryPowerState(
        batteryPercent: Int,
        isCharging: Boolean,
        isPowerSaveMode: Boolean,
        isLowBatteryThrottled: Boolean,
        effectiveIntervalMs: Int? = null
    ) {
        _telemetry.update { current ->
            current.copy(
                batteryPercent = batteryPercent.coerceIn(0, 100),
                isCharging = isCharging,
                isPowerSaveModeActive = isPowerSaveMode,
                isLowBatteryThrottled = isLowBatteryThrottled,
                effectiveIntervalMs = effectiveIntervalMs ?: current.effectiveIntervalMs
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

    companion object {
        private fun createInitialTelemetry(context: Context): EngineTelemetry {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val (pct, charging) = readBatterySnapshot(context)
            return EngineTelemetry(
                isProtectionEnabled = false,
                isMediaProjectionActive = false,
                isAccessibilityActive = false,
                isOverlayPermitted = PermissionHelper.hasOverlayPermission(context),
                framesProcessedCount = 0L,
                framesSkippedCount = 0L,
                currentInspectionFps = 0.0f,
                batteryPercent = pct,
                isCharging = charging,
                isPowerSaveModeActive = powerManager?.isPowerSaveMode == true,
                isLowBatteryThrottled = !charging && pct <= 19,
                statusMessage = "Protection Disengaged (Standby)"
            )
        }

        fun readBatterySnapshot(context: Context): Pair<Int, Boolean> {
            return try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val statusIntent = context.registerReceiver(null, filter)
                if (statusIntent != null) {
                    val level = statusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = statusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    val pct = if (level >= 0) ((level * 100) / scale).coerceIn(0, 100) else 100
                    val status = statusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    pct to charging
                } else {
                    100 to false
                }
            } catch (_: Exception) {
                100 to false
            }
        }
    }
}
