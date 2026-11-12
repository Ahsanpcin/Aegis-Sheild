package com.example.monitoring.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.di.AegisContainer

class AegisAccessibilityService : AccessibilityService() {

    private val container by lazy { AegisContainer.getInstance(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AegisAccessibilityService connected and monitoring window transitions.")
        container.protectionRepository.updateAccessibilityState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return

            // Filter out system UI, keyboards, and our own app
            if (pkgName != packageName &&
                pkgName != "com.android.systemui" &&
                !pkgName.contains("inputmethod")
            ) {
                val appLabel = getAppLabel(pkgName)
                Log.d(TAG, "Foreground application shifted: $pkgName ($appLabel)")
                container.protectionRepository.updateActivePackage(pkgName, appLabel)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AegisAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        container.protectionRepository.updateAccessibilityState(false)
        Log.i(TAG, "AegisAccessibilityService destroyed.")
    }

    private fun getAppLabel(pkgName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkgName
        }
    }

    companion object {
        private const val TAG = "AegisAccessibility"

        @Volatile
        private var instance: AegisAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

        /**
         * Triggers Android's official GLOBAL_ACTION_HOME to instantly dismiss the offending application
         * without attempting unsupported force-stop APIs.
         */
        fun performEmergencyHomeAction(): Boolean {
            val service = instance
            if (service != null) {
                val success = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.i(TAG, "Executed emergency GLOBAL_ACTION_HOME: $success")
                return success
            }
            Log.w(TAG, "Cannot execute emergency home: AccessibilityService not connected.")
            return false
        }
    }
}
