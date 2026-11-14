package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.example.core.models.SensitivityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class LockdownIncidentInfo(
    val appName: String = "Detected App",
    val packageName: String = "",
    val categoryName: String = "Explicit / Inappropriate",
    val details: String = "",
    val detectedAtMillis: Long = 0L
)

enum class PowerMode(
    val displayName: String,
    val caption: String,
    val scanIntervalMs: Int,
    val captureWidthPx: Int,
    val skipDuplicates: Boolean,
    val pauseOnScreenOff: Boolean
) {
    POWER_SAVING(
        displayName = "Power Saving",
        caption = "650ms • Low Battery",
        scanIntervalMs = 650,
        captureWidthPx = 320,
        skipDuplicates = true,
        pauseOnScreenOff = true
    ),
    BALANCED(
        displayName = "Balanced",
        caption = "300ms • Daily Guard",
        scanIntervalMs = 300,
        captureWidthPx = 480,
        skipDuplicates = true,
        pauseOnScreenOff = true
    ),
    EXTREME(
        displayName = "Extreme",
        caption = "120ms • Max Speed",
        scanIntervalMs = 120,
        captureWidthPx = 640,
        skipDuplicates = false,
        pauseOnScreenOff = false
    )
}

class AegisPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)

    private val _sensitivity = MutableStateFlow(getSavedSensitivity())
    val sensitivity: StateFlow<SensitivityLevel> = _sensitivity.asStateFlow()

    private val _strictEverything = MutableStateFlow(prefs.getBoolean(KEY_STRICT_EVERYTHING, false))
    val strictEverything: StateFlow<Boolean> = _strictEverything.asStateFlow()

    private val _autoStart = MutableStateFlow(prefs.getBoolean(KEY_AUTO_START, true))
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _hasGuardianPin = MutableStateFlow(!prefs.getString(KEY_PIN_HASH, "").isNullOrEmpty())
    val hasGuardianPin: StateFlow<Boolean> = _hasGuardianPin.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(prefs.getBoolean(KEY_MAINTENANCE_MODE, false))
    val maintenanceMode: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _importedModelName = MutableStateFlow(prefs.getString(KEY_IMPORTED_MODEL_NAME, null))
    val importedModelName: StateFlow<String?> = _importedModelName.asStateFlow()

    // 10-Hour Whole-Device Lockdown & Anti-Tamper Preferences
    private val _lockdownHours = MutableStateFlow(prefs.getInt(KEY_LOCKDOWN_HOURS, DEFAULT_LOCKDOWN_HOURS))
    val lockdownHours: StateFlow<Int> = _lockdownHours.asStateFlow()

    private val _waitCooldownBeforePassword = MutableStateFlow(
        prefs.getBoolean(KEY_WAIT_COOLDOWN_BEFORE_PIN, true)
    )
    val waitCooldownBeforePassword: StateFlow<Boolean> = _waitCooldownBeforePassword.asStateFlow()

    private val _lockdownUntilMillis = MutableStateFlow(prefs.getLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L))
    val lockdownUntilMillis: StateFlow<Long> = _lockdownUntilMillis.asStateFlow()

    private val _lockdownPendingUnlock = MutableStateFlow(prefs.getBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false))
    val lockdownPendingUnlock: StateFlow<Boolean> = _lockdownPendingUnlock.asStateFlow()

    private val _lockdownStage = MutableStateFlow(prefs.getInt(KEY_LOCKDOWN_STAGE, inferInitialStage()))
    val lockdownStage: StateFlow<Int> = _lockdownStage.asStateFlow()

    private val _isShieldDesiredActive = MutableStateFlow(prefs.getBoolean(KEY_SHIELD_DESIRED_ACTIVE, false))
    val isShieldDesiredActive: StateFlow<Boolean> = _isShieldDesiredActive.asStateFlow()

    private val _lockdownIncidentInfo = MutableStateFlow(readLockdownIncident())
    val lockdownIncidentInfo: StateFlow<LockdownIncidentInfo> = _lockdownIncidentInfo.asStateFlow()

    private val _blockedOffendingPackage = MutableStateFlow(prefs.getString(KEY_BLOCKED_PACKAGE, null))
    val blockedOffendingPackage: StateFlow<String?> = _blockedOffendingPackage.asStateFlow()

    private val _antiTamperEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true))
    val antiTamperEnabled: StateFlow<Boolean> = _antiTamperEnabled.asStateFlow()

    // 7-Tap Hidden Advanced Precision Lab Preferences
    private val _advancedOptionsUnlocked = MutableStateFlow(prefs.getBoolean(KEY_ADVANCED_UNLOCKED, false))
    val advancedOptionsUnlocked: StateFlow<Boolean> = _advancedOptionsUnlocked.asStateFlow()

    private val _lockdownDurationMinutes = MutableStateFlow(
        prefs.getInt(
            KEY_LOCKDOWN_MINUTES,
            prefs.getInt(KEY_LOCKDOWN_HOURS, DEFAULT_LOCKDOWN_HOURS) * 60
        ).coerceIn(1, MAX_LOCKDOWN_MINUTES)
    )
    val lockdownDurationMinutes: StateFlow<Int> = _lockdownDurationMinutes.asStateFlow()

    private val _doubleTimeMultiplier = MutableStateFlow(prefs.getFloat(KEY_DOUBLE_TIME_MULTIPLIER, 2.0f))
    val doubleTimeMultiplier: StateFlow<Float> = _doubleTimeMultiplier.asStateFlow()

    private val _powerMode = MutableStateFlow(getSavedPowerMode())
    val powerMode: StateFlow<PowerMode> = _powerMode.asStateFlow()

    private val _scanIntervalMs = MutableStateFlow(prefs.getInt(KEY_SCAN_INTERVAL_MS, 300).coerceIn(100, 2000))
    val scanIntervalMs: StateFlow<Int> = _scanIntervalMs.asStateFlow()

    private val _captureWidthPx = MutableStateFlow(prefs.getInt(KEY_CAPTURE_WIDTH_PX, 480).coerceIn(240, 1080))
    val captureWidthPx: StateFlow<Int> = _captureWidthPx.asStateFlow()

    private val _skipDuplicateFrames = MutableStateFlow(prefs.getBoolean(KEY_SKIP_DUPLICATE_FRAMES, true))
    val skipDuplicateFrames: StateFlow<Boolean> = _skipDuplicateFrames.asStateFlow()

    private val _pauseWhenScreenOff = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, true))
    val pauseWhenScreenOff: StateFlow<Boolean> = _pauseWhenScreenOff.asStateFlow()

    private val _whitelistedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_WHITELISTED_PACKAGES, emptySet()) ?: emptySet()
    )
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()

    fun setWhitelistedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELISTED_PACKAGES, packages).apply()
        _whitelistedPackages.value = packages
    }

    fun addWhitelistedPackage(packageName: String) {
        if (packageName.isBlank()) return
        val current = _whitelistedPackages.value.toMutableSet()
        if (current.add(packageName.trim())) {
            setWhitelistedPackages(current)
        }
    }

    fun removeWhitelistedPackage(packageName: String) {
        val current = _whitelistedPackages.value.toMutableSet()
        if (current.remove(packageName)) {
            setWhitelistedPackages(current)
        }
    }

    fun isPackageWhitelisted(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return _whitelistedPackages.value.contains(packageName)
    }

    private val _customThresholdsEnabled = MutableStateFlow(prefs.getBoolean(KEY_CUSTOM_THRESHOLDS_ENABLED, false))
    val customThresholdsEnabled: StateFlow<Boolean> = _customThresholdsEnabled.asStateFlow()

    private val _customExplicitThreshold = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_EXPLICIT_THRESHOLD, 0.75f).coerceIn(0.35f, 0.95f))
    val customExplicitThreshold: StateFlow<Float> = _customExplicitThreshold.asStateFlow()

    private val _customSuggestiveThreshold = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_SUGGESTIVE_THRESHOLD, 0.80f).coerceIn(0.35f, 0.95f))
    val customSuggestiveThreshold: StateFlow<Float> = _customSuggestiveThreshold.asStateFlow()

    private val _consecutiveFramesRequired = MutableStateFlow(prefs.getInt(KEY_CONSECUTIVE_FRAMES_REQUIRED, 2).coerceIn(1, 5))
    val consecutiveFramesRequired: StateFlow<Int> = _consecutiveFramesRequired.asStateFlow()

    private val _showEmergencyDialerOnLockdown = MutableStateFlow(prefs.getBoolean(KEY_SHOW_EMERGENCY_DIALER, true))
    val showEmergencyDialerOnLockdown: StateFlow<Boolean> = _showEmergencyDialerOnLockdown.asStateFlow()

    private val _evictAppOnThreat = MutableStateFlow(prefs.getBoolean(KEY_EVICT_APP_ON_THREAT, true))
    val evictAppOnThreat: StateFlow<Boolean> = _evictAppOnThreat.asStateFlow()

    private val _guardianSettingsWindowMinutes = MutableStateFlow(prefs.getInt(KEY_GUARDIAN_WINDOW_MINUTES, 2).coerceIn(1, 15))
    val guardianSettingsWindowMinutes: StateFlow<Int> = _guardianSettingsWindowMinutes.asStateFlow()

    // Session-scoped Guardian authorization (automatically resets when app leaves foreground)
    private val _isGuardianSessionUnlocked = MutableStateFlow(false)
    val isGuardianSessionUnlocked: StateFlow<Boolean> = _isGuardianSessionUnlocked.asStateFlow()

    @Volatile
    private var guardianSettingsBypassUntilRealtime: Long = 0L

    @Volatile
    private var explicitSettingsTripPending: Boolean = false

    @Volatile
    private var authorizedExternalFlowUntilRealtime: Long = 0L

    private fun getSavedSensitivity(): SensitivityLevel {
        val name = prefs.getString(KEY_SENSITIVITY, SensitivityLevel.BALANCED.name)
        return try {
            SensitivityLevel.valueOf(name ?: SensitivityLevel.BALANCED.name)
        } catch (_: Exception) {
            SensitivityLevel.BALANCED
        }
    }

    private fun getSavedPowerMode(): PowerMode {
        val name = prefs.getString(KEY_POWER_MODE, PowerMode.BALANCED.name)
        return try {
            PowerMode.valueOf(name ?: PowerMode.BALANCED.name)
        } catch (_: Exception) {
            PowerMode.BALANCED
        }
    }

    private fun inferInitialStage(): Int {
        val pending = prefs.getBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false)
        val until = prefs.getLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L)
        return if (pending || until > System.currentTimeMillis()) {
            LOCKDOWN_STAGE_INITIAL
        } else {
            LOCKDOWN_STAGE_NONE
        }
    }

    private fun readLockdownIncident(): LockdownIncidentInfo {
        return LockdownIncidentInfo(
            appName = prefs.getString(KEY_LOCKDOWN_APP_NAME, "Detected App") ?: "Detected App",
            packageName = prefs.getString(KEY_LOCKDOWN_PACKAGE, "") ?: "",
            categoryName = prefs.getString(KEY_LOCKDOWN_CATEGORY, "Explicit / Inappropriate") ?: "Explicit / Inappropriate",
            details = prefs.getString(KEY_LOCKDOWN_DETAILS, "") ?: "",
            detectedAtMillis = prefs.getLong(KEY_LOCKDOWN_DETECTED_AT, 0L)
        )
    }

    fun setSensitivity(level: SensitivityLevel) {
        prefs.edit().putString(KEY_SENSITIVITY, level.name).apply()
        _sensitivity.value = level
    }

    fun setStrictEverything(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_EVERYTHING, enabled).apply()
        _strictEverything.value = enabled
    }

    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        _autoStart.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setMaintenanceMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MAINTENANCE_MODE, enabled).apply()
        _maintenanceMode.value = enabled
    }

    fun setImportedModelName(name: String?) {
        if (name == null) {
            prefs.edit().remove(KEY_IMPORTED_MODEL_NAME).apply()
        } else {
            prefs.edit().putString(KEY_IMPORTED_MODEL_NAME, name).apply()
        }
        _importedModelName.value = name
    }

    fun setLockdownHours(hours: Int) {
        val clampedHours = hours.coerceIn(1, 24)
        val totalMinutes = clampedHours * 60
        prefs.edit()
            .putInt(KEY_LOCKDOWN_HOURS, clampedHours)
            .putInt(KEY_LOCKDOWN_MINUTES, totalMinutes)
            .apply()
        _lockdownHours.value = clampedHours
        _lockdownDurationMinutes.value = totalMinutes
    }

    fun setLockdownDurationMinutes(totalMinutes: Int) {
        val clamped = totalMinutes.coerceIn(1, MAX_LOCKDOWN_MINUTES)
        val wholeHours = (clamped / 60).coerceAtLeast(1)
        prefs.edit()
            .putInt(KEY_LOCKDOWN_MINUTES, clamped)
            .putInt(KEY_LOCKDOWN_HOURS, wholeHours)
            .apply()
        _lockdownDurationMinutes.value = clamped
        _lockdownHours.value = wholeHours
    }

    fun setAdvancedOptionsUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_ADVANCED_UNLOCKED, unlocked).apply()
        _advancedOptionsUnlocked.value = unlocked
    }

    fun setDoubleTimeMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(1.0f, 5.0f)
        prefs.edit().putFloat(KEY_DOUBLE_TIME_MULTIPLIER, clamped).apply()
        _doubleTimeMultiplier.value = clamped
    }

    fun setPowerMode(mode: PowerMode) {
        prefs.edit()
            .putString(KEY_POWER_MODE, mode.name)
            .putInt(KEY_SCAN_INTERVAL_MS, mode.scanIntervalMs)
            .putInt(KEY_CAPTURE_WIDTH_PX, mode.captureWidthPx)
            .putBoolean(KEY_SKIP_DUPLICATE_FRAMES, mode.skipDuplicates)
            .putBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, mode.pauseOnScreenOff)
            .apply()
        _powerMode.value = mode
        _scanIntervalMs.value = mode.scanIntervalMs
        _captureWidthPx.value = mode.captureWidthPx
        _skipDuplicateFrames.value = mode.skipDuplicates
        _pauseWhenScreenOff.value = mode.pauseOnScreenOff
    }

    fun setScanIntervalMs(intervalMs: Int) {
        val clamped = intervalMs.coerceIn(100, 2000)
        prefs.edit().putInt(KEY_SCAN_INTERVAL_MS, clamped).apply()
        _scanIntervalMs.value = clamped
    }

    fun setCaptureWidthPx(widthPx: Int) {
        val clamped = widthPx.coerceIn(240, 1080)
        prefs.edit().putInt(KEY_CAPTURE_WIDTH_PX, clamped).apply()
        _captureWidthPx.value = clamped
    }

    fun setSkipDuplicateFrames(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_DUPLICATE_FRAMES, enabled).apply()
        _skipDuplicateFrames.value = enabled
    }

    fun setPauseWhenScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, enabled).apply()
        _pauseWhenScreenOff.value = enabled
    }

    fun setCompactPowerResolution(enabled: Boolean) {
        val targetWidth = if (enabled) 320 else PowerMode.BALANCED.captureWidthPx
        setCaptureWidthPx(targetWidth)
    }

    fun setCustomThresholdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CUSTOM_THRESHOLDS_ENABLED, enabled).apply()
        _customThresholdsEnabled.value = enabled
    }

    fun setCustomExplicitThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.35f, 0.95f)
        prefs.edit().putFloat(KEY_CUSTOM_EXPLICIT_THRESHOLD, clamped).apply()
        _customExplicitThreshold.value = clamped
    }

    fun setCustomSuggestiveThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.35f, 0.95f)
        prefs.edit().putFloat(KEY_CUSTOM_SUGGESTIVE_THRESHOLD, clamped).apply()
        _customSuggestiveThreshold.value = clamped
    }

    fun setConsecutiveFramesRequired(count: Int) {
        val clamped = count.coerceIn(1, 5)
        prefs.edit().putInt(KEY_CONSECUTIVE_FRAMES_REQUIRED, clamped).apply()
        _consecutiveFramesRequired.value = clamped
    }

    fun setShowEmergencyDialerOnLockdown(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EMERGENCY_DIALER, enabled).apply()
        _showEmergencyDialerOnLockdown.value = enabled
    }

    fun setEvictAppOnThreat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EVICT_APP_ON_THREAT, enabled).apply()
        _evictAppOnThreat.value = enabled
    }

    fun setGuardianSettingsWindowMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 15)
        prefs.edit().putInt(KEY_GUARDIAN_WINDOW_MINUTES, clamped).apply()
        _guardianSettingsWindowMinutes.value = clamped
    }

    fun resetAdvancedCustomizations() {
        setLockdownHours(DEFAULT_LOCKDOWN_HOURS)
        setDoubleTimeMultiplier(2.0f)
        setPowerMode(PowerMode.BALANCED)
        setCustomThresholdsEnabled(false)
        setCustomExplicitThreshold(0.75f)
        setCustomSuggestiveThreshold(0.80f)
        setConsecutiveFramesRequired(2)
        setShowEmergencyDialerOnLockdown(true)
        setEvictAppOnThreat(true)
        setGuardianSettingsWindowMinutes(2)
    }

    fun setWaitCooldownBeforePassword(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAIT_COOLDOWN_BEFORE_PIN, enabled).apply()
        _waitCooldownBeforePassword.value = enabled
    }

    fun setAntiTamperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, enabled).apply()
        _antiTamperEnabled.value = enabled
    }

    fun setShieldDesiredActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SHIELD_DESIRED_ACTIVE, active).apply()
        _isShieldDesiredActive.value = active
    }

    fun getDoubleTimeDurationMinutes(): Int =
        (_lockdownDurationMinutes.value * _doubleTimeMultiplier.value).toInt().coerceAtLeast(1)

    fun getDoubleTimeHours(): Int = (getDoubleTimeDurationMinutes() / 60).coerceAtLeast(1)

    fun isInDoubleTimeStage(): Boolean = _lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME

    fun engageFullDeviceLockdown(
        appName: String,
        packageName: String,
        categoryName: String,
        details: String
    ): Long {
        val now = System.currentTimeMillis()
        val durationMs = _lockdownDurationMinutes.value * 60L * 1000L
        val until = now + durationMs

        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, until)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, true)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_INITIAL)
            .putBoolean(KEY_SHIELD_DESIRED_ACTIVE, true)
            .putString(KEY_LOCKDOWN_APP_NAME, appName)
            .putString(KEY_LOCKDOWN_PACKAGE, packageName)
            .putString(KEY_LOCKDOWN_CATEGORY, categoryName)
            .putString(KEY_LOCKDOWN_DETAILS, details)
            .putLong(KEY_LOCKDOWN_DETECTED_AT, now)
            .putString(KEY_BLOCKED_PACKAGE, packageName)
            .apply()

        _lockdownUntilMillis.value = until
        _lockdownPendingUnlock.value = true
        _lockdownStage.value = LOCKDOWN_STAGE_INITIAL
        _isShieldDesiredActive.value = true
        _lockdownIncidentInfo.value = LockdownIncidentInfo(appName, packageName, categoryName, details, now)
        _blockedOffendingPackage.value = packageName
        return until
    }

    /**
     * Stage 2 Double-Time Fallback:
     * If the user does not know the Master Code after Stage 1 finishes, they can wait
     * (lockdownDurationMinutes * doubleTimeMultiplier). When that double-time countdown finishes,
     * the device automatically unlocks while keeping the Sheildlenz Shield ON.
     */
    fun engageDoubleTimeLockdown(): Long {
        val now = System.currentTimeMillis()
        val durationMs = getDoubleTimeDurationMinutes() * 60L * 1000L
        val until = now + durationMs

        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, until)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, true)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_DOUBLE_TIME)
            .putBoolean(KEY_SHIELD_DESIRED_ACTIVE, true)
            .apply()

        _lockdownUntilMillis.value = until
        _lockdownPendingUnlock.value = true
        _lockdownStage.value = LOCKDOWN_STAGE_DOUBLE_TIME
        _isShieldDesiredActive.value = true
        return until
    }

    fun getRemainingLockdownMillis(): Long {
        val until = _lockdownUntilMillis.value
        if (until <= 0L) return 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun completeDoubleTimeIfExpired(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME && getRemainingLockdownMillis() <= 0L) {
            clearLockdown()
            return true
        }
        return false
    }

    fun isLockdownActive(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME) {
            return if (getRemainingLockdownMillis() > 0L) {
                true
            } else {
                clearLockdown()
                false
            }
        }
        return _lockdownPendingUnlock.value || getRemainingLockdownMillis() > 0L
    }

    fun canAcceptPasswordForLockdown(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME) return true
        if (!_waitCooldownBeforePassword.value) return true
        return getRemainingLockdownMillis() <= 0L
    }

    fun clearLockdown() {
        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_NONE)
            .remove(KEY_BLOCKED_PACKAGE)
            .apply()
        _lockdownUntilMillis.value = 0L
        _lockdownPendingUnlock.value = false
        _lockdownStage.value = LOCKDOWN_STAGE_NONE
        _blockedOffendingPackage.value = null
    }

    @VisibleForTesting
    internal fun setLockdownUntilForTesting(untilMillis: Long) {
        prefs.edit().putLong(KEY_LOCKDOWN_UNTIL_MILLIS, untilMillis).apply()
        _lockdownUntilMillis.value = untilMillis
    }

    fun setBlockedOffendingPackage(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            prefs.edit().remove(KEY_BLOCKED_PACKAGE).apply()
        } else {
            prefs.edit().putString(KEY_BLOCKED_PACKAGE, packageName).apply()
        }
        _blockedOffendingPackage.value = packageName
    }

    fun unlockGuardianSession(grantSettingsWindowMinutes: Int = _guardianSettingsWindowMinutes.value) {
        _isGuardianSessionUnlocked.value = true
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (grantSettingsWindowMinutes * 60_000L)
    }

    fun lockGuardianSession() {
        _isGuardianSessionUnlocked.value = false
        guardianSettingsBypassUntilRealtime = 0L
        explicitSettingsTripPending = false
        authorizedExternalFlowUntilRealtime = 0L
    }

    fun beginAuthorizedExternalFlow(windowMillis: Long = 180_000L) {
        authorizedExternalFlowUntilRealtime = SystemClock.elapsedRealtime() + windowMillis
    }

    fun onAppBackgrounded() {
        val now = SystemClock.elapsedRealtime()
        val withinAuthorizedExternalFlow = now <= authorizedExternalFlowUntilRealtime
        authorizedExternalFlowUntilRealtime = 0L
        if (withinAuthorizedExternalFlow) {
            return
        }
        _isGuardianSessionUnlocked.value = false
        if (explicitSettingsTripPending) {
            explicitSettingsTripPending = false
        } else {
            guardianSettingsBypassUntilRealtime = 0L
        }
    }

    fun grantTemporarySettingsAuthorization(minutes: Int = _guardianSettingsWindowMinutes.value) {
        explicitSettingsTripPending = true
        beginAuthorizedExternalFlow(minutes * 60_000L)
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (minutes * 60_000L)
    }

    fun isGuardianAuthorizedForSystemSettings(): Boolean {
        if (!_hasGuardianPin.value) return true
        if (_isGuardianSessionUnlocked.value) return true
        return SystemClock.elapsedRealtime() < guardianSettingsBypassUntilRealtime
    }

    fun verifyGuardianPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash.isNullOrEmpty() || pin.isEmpty()) return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    fun setGuardianPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        _hasGuardianPin.value = true
        unlockGuardianSession()
    }

    fun removeGuardianPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
        _hasGuardianPin.value = false
        _isGuardianSessionUnlocked.value = true
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(("aegis_salt_" + pin).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_LOCKDOWN_HOURS = 10
        const val MAX_LOCKDOWN_MINUTES = 24 * 60
        const val LOCKDOWN_STAGE_NONE = 0
        const val LOCKDOWN_STAGE_INITIAL = 1
        const val LOCKDOWN_STAGE_DOUBLE_TIME = 2

        fun formatMinutesLabel(totalMinutes: Int): String {
            val safe = totalMinutes.coerceAtLeast(1)
            val hrs = safe / 60
            val mins = safe % 60
            return when {
                hrs == 0 -> "${mins}m"
                mins == 0 -> "${hrs}h"
                else -> "${hrs}h ${mins}m"
            }
        }

        private const val KEY_SENSITIVITY = "key_sensitivity"
        private const val KEY_STRICT_EVERYTHING = "key_strict_everything"
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_PIN_HASH = "key_guardian_pin_hash"
        private const val KEY_MAINTENANCE_MODE = "key_maintenance_mode"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_IMPORTED_MODEL_NAME = "key_imported_model_name"

        private const val KEY_LOCKDOWN_HOURS = "key_lockdown_hours"
        private const val KEY_LOCKDOWN_MINUTES = "key_lockdown_minutes"
        private const val KEY_WAIT_COOLDOWN_BEFORE_PIN = "key_wait_cooldown_before_pin"
        private const val KEY_LOCKDOWN_UNTIL_MILLIS = "key_lockdown_until_millis"
        private const val KEY_LOCKDOWN_PENDING_UNLOCK = "key_lockdown_pending_unlock"
        private const val KEY_LOCKDOWN_STAGE = "key_lockdown_stage"
        private const val KEY_SHIELD_DESIRED_ACTIVE = "key_shield_desired_active"
        private const val KEY_LOCKDOWN_APP_NAME = "key_lockdown_app_name"
        private const val KEY_LOCKDOWN_PACKAGE = "key_lockdown_package"
        private const val KEY_LOCKDOWN_CATEGORY = "key_lockdown_category"
        private const val KEY_LOCKDOWN_DETAILS = "key_lockdown_details"
        private const val KEY_LOCKDOWN_DETECTED_AT = "key_lockdown_detected_at"
        private const val KEY_BLOCKED_PACKAGE = "key_blocked_package"
        private const val KEY_ANTI_TAMPER_ENABLED = "key_anti_tamper_enabled"

        private const val KEY_ADVANCED_UNLOCKED = "key_advanced_lab_unlocked"
        private const val KEY_DOUBLE_TIME_MULTIPLIER = "key_double_time_multiplier"
        private const val KEY_POWER_MODE = "key_power_mode"
        private const val KEY_SCAN_INTERVAL_MS = "key_scan_interval_ms"
        private const val KEY_CAPTURE_WIDTH_PX = "key_capture_width_px"
        private const val KEY_SKIP_DUPLICATE_FRAMES = "key_skip_duplicate_frames"
        private const val KEY_PAUSE_WHEN_SCREEN_OFF = "key_pause_when_screen_off"
        private const val KEY_CUSTOM_THRESHOLDS_ENABLED = "key_custom_thresholds_enabled"
        private const val KEY_CUSTOM_EXPLICIT_THRESHOLD = "key_custom_explicit_threshold"
        private const val KEY_CUSTOM_SUGGESTIVE_THRESHOLD = "key_custom_suggestive_threshold"
        private const val KEY_CONSECUTIVE_FRAMES_REQUIRED = "key_consecutive_frames_required"
        private const val KEY_SHOW_EMERGENCY_DIALER = "key_show_emergency_dialer"
        private const val KEY_EVICT_APP_ON_THREAT = "key_evict_app_on_threat"
        private const val KEY_GUARDIAN_WINDOW_MINUTES = "key_guardian_window_minutes"
        private const val KEY_WHITELISTED_PACKAGES = "key_whitelisted_packages"
    }
}
