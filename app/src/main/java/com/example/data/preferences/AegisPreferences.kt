package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.core.models.SensitivityLevel
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private fun getSavedSensitivity(): SensitivityLevel {
        val name = prefs.getString(KEY_SENSITIVITY, SensitivityLevel.BALANCED.name)
        return try {
            SensitivityLevel.valueOf(name ?: SensitivityLevel.BALANCED.name)
        } catch (_: Exception) {
            SensitivityLevel.BALANCED
        }
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

    fun verifyGuardianPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true // If not set, allow
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    fun setGuardianPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        _hasGuardianPin.value = true
    }

    fun removeGuardianPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
        _hasGuardianPin.value = false
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(("aegis_salt_" + pin).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_SENSITIVITY = "key_sensitivity"
        private const val KEY_STRICT_EVERYTHING = "key_strict_everything"
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_PIN_HASH = "key_guardian_pin_hash"
        private const val KEY_MAINTENANCE_MODE = "key_maintenance_mode"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
    }
}
