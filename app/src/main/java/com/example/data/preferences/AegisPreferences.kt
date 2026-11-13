package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
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

    private val _lockdownIncidentInfo = MutableStateFlow(readLockdownIncident())
    val lockdownIncidentInfo: StateFlow<LockdownIncidentInfo> = _lockdownIncidentInfo.asStateFlow()

    private val _blockedOffendingPackage = MutableStateFlow(prefs.getString(KEY_BLOCKED_PACKAGE, null))
    val blockedOffendingPackage: StateFlow<String?> = _blockedOffendingPackage.asStateFlow()

    private val _antiTamperEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true))
    val antiTamperEnabled: StateFlow<Boolean> = _antiTamperEnabled.asStateFlow()

    // Session-scoped Guardian authorization (automatically resets when app leaves foreground)
    private val _isGuardianSessionUnlocked = MutableStateFlow(false)
    val isGuardianSessionUnlocked: StateFlow<Boolean> = _isGuardianSessionUnlocked.asStateFlow()

    @Volatile
    private var guardianSettingsBypassUntilRealtime: Long = 0L

    @Volatile
    private var explicitSettingsTripPending: Boolean = false

    private fun getSavedSensitivity(): SensitivityLevel {
        val name = prefs.getString(KEY_SENSITIVITY, SensitivityLevel.BALANCED.name)
        return try {
            SensitivityLevel.valueOf(name ?: SensitivityLevel.BALANCED.name)
        } catch (_: Exception) {
            SensitivityLevel.BALANCED
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
        val clamped = hours.coerceIn(1, 24)
        prefs.edit().putInt(KEY_LOCKDOWN_HOURS, clamped).apply()
        _lockdownHours.value = clamped
    }

    fun setWaitCooldownBeforePassword(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAIT_COOLDOWN_BEFORE_PIN, enabled).apply()
        _waitCooldownBeforePassword.value = enabled
    }

    fun setAntiTamperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, enabled).apply()
        _antiTamperEnabled.value = enabled
    }

    fun engageFullDeviceLockdown(
        appName: String,
        packageName: String,
        categoryName: String,
        details: String
    ): Long {
        val now = System.currentTimeMillis()
        val durationMs = _lockdownHours.value * 60L * 60L * 1000L
        val until = now + durationMs

        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, until)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, true)
            .putString(KEY_LOCKDOWN_APP_NAME, appName)
            .putString(KEY_LOCKDOWN_PACKAGE, packageName)
            .putString(KEY_LOCKDOWN_CATEGORY, categoryName)
            .putString(KEY_LOCKDOWN_DETAILS, details)
            .putLong(KEY_LOCKDOWN_DETECTED_AT, now)
            .putString(KEY_BLOCKED_PACKAGE, packageName)
            .apply()

        _lockdownUntilMillis.value = until
        _lockdownPendingUnlock.value = true
        _lockdownIncidentInfo.value = LockdownIncidentInfo(appName, packageName, categoryName, details, now)
        _blockedOffendingPackage.value = packageName
        return until
    }

    fun getRemainingLockdownMillis(): Long {
        val until = _lockdownUntilMillis.value
        if (until <= 0L) return 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isLockdownActive(): Boolean {
        return _lockdownPendingUnlock.value || getRemainingLockdownMillis() > 0L
    }

    fun canAcceptPasswordForLockdown(): Boolean {
        if (!_waitCooldownBeforePassword.value) return true
        return getRemainingLockdownMillis() <= 0L
    }

    fun clearLockdown() {
        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false)
            .remove(KEY_BLOCKED_PACKAGE)
            .apply()
        _lockdownUntilMillis.value = 0L
        _lockdownPendingUnlock.value = false
        _blockedOffendingPackage.value = null
    }

    fun setBlockedOffendingPackage(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            prefs.edit().remove(KEY_BLOCKED_PACKAGE).apply()
        } else {
            prefs.edit().putString(KEY_BLOCKED_PACKAGE, packageName).apply()
        }
        _blockedOffendingPackage.value = packageName
    }

    fun unlockGuardianSession(grantSettingsWindowMinutes: Int = 2) {
        _isGuardianSessionUnlocked.value = true
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (grantSettingsWindowMinutes * 60_000L)
    }

    fun lockGuardianSession() {
        _isGuardianSessionUnlocked.value = false
        guardianSettingsBypassUntilRealtime = 0L
        explicitSettingsTripPending = false
    }

    fun onAppBackgrounded() {
        _isGuardianSessionUnlocked.value = false
        if (explicitSettingsTripPending) {
            explicitSettingsTripPending = false
        } else {
            guardianSettingsBypassUntilRealtime = 0L
        }
    }

    fun grantTemporarySettingsAuthorization(minutes: Int = 2) {
        explicitSettingsTripPending = true
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (minutes * 60_000L)
    }

    fun isGuardianAuthorizedForSystemSettings(): Boolean {
        if (!_hasGuardianPin.value) return true
        if (_isGuardianSessionUnlocked.value) return true
        return SystemClock.elapsedRealtime() < guardianSettingsBypassUntilRealtime
    }

    fun verifyGuardianPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
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

        private const val KEY_SENSITIVITY = "key_sensitivity"
        private const val KEY_STRICT_EVERYTHING = "key_strict_everything"
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_PIN_HASH = "key_guardian_pin_hash"
        private const val KEY_MAINTENANCE_MODE = "key_maintenance_mode"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_IMPORTED_MODEL_NAME = "key_imported_model_name"

        private const val KEY_LOCKDOWN_HOURS = "key_lockdown_hours"
        private const val KEY_WAIT_COOLDOWN_BEFORE_PIN = "key_wait_cooldown_before_pin"
        private const val KEY_LOCKDOWN_UNTIL_MILLIS = "key_lockdown_until_millis"
        private const val KEY_LOCKDOWN_PENDING_UNLOCK = "key_lockdown_pending_unlock"
        private const val KEY_LOCKDOWN_APP_NAME = "key_lockdown_app_name"
        private const val KEY_LOCKDOWN_PACKAGE = "key_lockdown_package"
        private const val KEY_LOCKDOWN_CATEGORY = "key_lockdown_category"
        private const val KEY_LOCKDOWN_DETAILS = "key_lockdown_details"
        private const val KEY_LOCKDOWN_DETECTED_AT = "key_lockdown_detected_at"
        private const val KEY_BLOCKED_PACKAGE = "key_blocked_package"
        private const val KEY_ANTI_TAMPER_ENABLED = "key_anti_tamper_enabled"
    }
}
