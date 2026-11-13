package com.example.monitoring.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.blocking.overlay.SystemOverlayController
import com.example.di.AegisContainer

class AegisAccessibilityService : AccessibilityService() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController by lazy { SystemOverlayController(this) }
    private var lastTamperInterceptTimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AegisAccessibilityService connected with anti-tamper & app eviction guards.")
        container.protectionRepository.updateAccessibilityState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val pkgName = event.packageName?.toString() ?: return
        val prefs = container.preferences

        // 1. Whole-Device 10-Hour Lockdown Enforcement
        if (prefs.isLockdownActive() && !isAllowedDuringLockdown(pkgName)) {
            Log.w(TAG, "Lockdown active — blocking access to $pkgName and restoring full-screen lock overlay.")
            performGlobalAction(GLOBAL_ACTION_HOME)
            overlayController.showLockdownOverlay(prefs.lockdownIncidentInfo.value)
            return
        }

        // 2. Anti-Tamper Settings Guard (Device Admin, Accessibility, App Info / Uninstall, Date & Time)
        if (prefs.antiTamperEnabled.value &&
            prefs.hasGuardianPin.value &&
            !prefs.isGuardianAuthorizedForSystemSettings() &&
            isSettingsOrInstallerPackage(pkgName)
        ) {
            if (isSensitiveAdminOrAppScreen(event)) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTamperInterceptTimeMs > 800L) {
                    lastTamperInterceptTimeMs = now
                    Log.w(TAG, "Blocked unauthorized tamper access inside $pkgName")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    overlayController.showTamperLockOverlay(
                        reason = "Access to Device Administrator, Accessibility, App Uninstall, Overlay, or Clock Settings is protected. Enter Guardian Password to proceed."
                    )
                }
                return
            }
        }

        // 3. Re-entry / Recents Guard for recently blocked offending package
        val blockedPkg = prefs.blockedOffendingPackage.value
        if (!blockedPkg.isNullOrBlank() && pkgName == blockedPkg && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.w(TAG, "Prevented reopening blocked package $blockedPkg from launcher/Recents.")
            performGlobalAction(GLOBAL_ACTION_HOME)
            killBackgroundApp(this, blockedPkg)
            overlayController.showLockdownOverlay(prefs.lockdownIncidentInfo.value)
            return
        }

        // 4. Track active monitored foreground application
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkgName != packageName &&
            pkgName != "com.android.systemui" &&
            !pkgName.contains("inputmethod")
        ) {
            val appLabel = getAppLabel(pkgName)
            container.protectionRepository.updateActivePackage(pkgName, appLabel)
        }
    }

    private fun isAllowedDuringLockdown(pkgName: String): Boolean {
        return pkgName == packageName ||
            pkgName == "com.android.dialer" ||
            pkgName == "com.google.android.dialer" ||
            pkgName == "com.android.phone" ||
            pkgName == "com.android.incallui" ||
            pkgName == "com.android.emergency"
    }

    private fun isSettingsOrInstallerPackage(pkgName: String): Boolean {
        return pkgName == "com.android.settings" ||
            pkgName == "com.android.packageinstaller" ||
            pkgName == "com.google.android.packageinstaller" ||
            pkgName == "com.android.permissioncontroller" ||
            pkgName == "com.google.android.permissioncontroller"
    }

    private fun isSensitiveAdminOrAppScreen(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString()?.lowercase() ?: ""
        if (className.contains("deviceadmin") ||
            className.contains("installedappdetails") ||
            className.contains("uninstaller") ||
            className.contains("datetimesettings") ||
            className.contains("manageoverlay")
        ) {
            return true
        }

        val eventText = event.text.joinToString(" ").lowercase()
        if (containsProtectedKeyword(eventText)) {
            return true
        }

        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return false

        return try {
            scanNodeForProtectedKeywords(root, depth = 0)
        } finally {
            @Suppress("DEPRECATION")
            try {
                root.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun scanNodeForProtectedKeywords(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 5) return false
        val combined = "${node.text ?: ""} ${node.contentDescription ?: ""}".lowercase()
        if (containsProtectedKeyword(combined)) {
            return true
        }
        val count = node.childCount.coerceAtMost(16)
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val hit = try {
                scanNodeForProtectedKeywords(child, depth + 1)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
            }
            if (hit) return true
        }
        return false
    }

    private fun containsProtectedKeyword(text: String): Boolean {
        if (text.isBlank()) return false
        return PROTECTED_KEYWORDS.any { keyword -> text.contains(keyword) }
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

        private val PROTECTED_KEYWORDS = listOf(
            "aegis shield",
            "device admin",
            "device administrator",
            "device administrators",
            "deactivate",
            "force stop",
            "uninstall",
            "display over other apps",
            "draw over other apps",
            "date & time",
            "set time",
            "automatic date"
        )

        @Volatile
        private var instance: AegisAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

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

        fun closeAndEvictPackage(context: Context, packageName: String) {
            performEmergencyHomeAction()
            killBackgroundApp(context, packageName)
        }

        fun killBackgroundApp(context: Context, packageName: String) {
            if (packageName.isBlank() || packageName == context.packageName) return
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.killBackgroundProcesses(packageName)
                Log.i(TAG, "Terminated background processes for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed killing background processes for $packageName", e)
            }
        }
    }
}
