package com.example.monitoring.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.di.AegisContainer

class AegisAccessibilityService : AccessibilityService() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController get() = container.overlayController
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

        // 2. Anti-Tamper Settings Guard — ONLY when tampering with Sheildlenz itself
        if (prefs.antiTamperEnabled.value &&
            prefs.hasGuardianPin.value &&
            !prefs.isGuardianAuthorizedForSystemSettings() &&
            isSettingsOrInstallerPackage(pkgName)
        ) {
            if (isSheildlenzTamperScreen(pkgName, event)) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTamperInterceptTimeMs > 800L) {
                    lastTamperInterceptTimeMs = now
                    Log.w(TAG, "Blocked unauthorized tamper access targeting Sheildlenz inside $pkgName")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    overlayController.showTamperLockOverlay(
                        reason = "Modifying or uninstalling Sheildlenz (App Info, Device Admin, Accessibility, or Overlay) requires Guardian Password verification."
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

        // 4. Track active monitored foreground application & wake adaptive scan cadence
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            container.protectionRepository.notifyScreenContextChanged()
            if (pkgName != packageName &&
                pkgName != "com.android.systemui" &&
                !pkgName.contains("inputmethod")
            ) {
                val appLabel = getAppLabel(pkgName)
                container.protectionRepository.updateActivePackage(pkgName, appLabel)
            }
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

    private fun isSheildlenzTamperScreen(pkgName: String, event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        val builder = StringBuilder(event.text.joinToString(" "))

        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }

        if (root != null) {
            try {
                collectWindowText(root, depth = 0, out = builder)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    root.recycle()
                } catch (_: Exception) {
                }
            }
        }

        return shouldTriggerAntiTamper(
            packageName = pkgName,
            className = className,
            screenText = builder.toString()
        )
    }

    private fun collectWindowText(node: AccessibilityNodeInfo, depth: Int, out: StringBuilder) {
        if (depth > 6 || out.length >= MAX_INSPECTED_TEXT_CHARS) return
        val nodeText = node.text?.toString()
        if (!nodeText.isNullOrBlank()) {
            out.append(' ').append(nodeText)
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            out.append(' ').append(desc)
        }
        val count = node.childCount.coerceAtMost(24)
        for (i in 0 until count) {
            if (out.length >= MAX_INSPECTED_TEXT_CHARS) break
            val child = node.getChild(i) ?: continue
            try {
                collectWindowText(child, depth + 1, out)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
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
        private const val MAX_INSPECTED_TEXT_CHARS = 4096

        private val SHEILDLENZ_IDENTIFIERS = listOf(
            "sheildlenz",
            "shieldlenz",
            "com.ahsan.tcr.sheildlenz"
        )

        private val TAMPER_ACTION_KEYWORDS = listOf(
            "uninstall",
            "force stop",
            "deactivate",
            "disable",
            "device admin",
            "device administrator",
            "use sheildlenz",
            "sheildlenz shortcut",
            "allow display over other apps",
            "allow draw over other apps",
            "permit drawing over other apps",
            "clear storage",
            "clear data"
        )

        fun isSettingsOrInstallerPackage(pkgName: String): Boolean {
            val lower = pkgName.lowercase()
            return lower == "com.android.settings" ||
                lower.contains("packageinstaller") ||
                lower.contains("permissioncontroller")
        }

        fun shouldTriggerAntiTamper(
            packageName: String,
            className: String,
            screenText: String
        ): Boolean {
            val pkg = packageName.lowercase()
            val cls = className.lowercase()
            val text = screenText.lowercase()

            if (!isSettingsOrInstallerPackage(pkg) && !cls.contains("uninstaller") && !cls.contains("deviceadminadd")) {
                return false
            }

            // Password is ONLY required when the active screen targets Sheildlenz itself.
            val targetsSheildlenz = SHEILDLENZ_IDENTIFIERS.any { id -> text.contains(id) }
            if (!targetsSheildlenz) {
                return false
            }

            // On dedicated installer/uninstaller or Device Admin confirmation screens, targeting Sheildlenz is unambiguous.
            if (pkg.contains("packageinstaller") ||
                cls.contains("uninstaller") ||
                cls.contains("deviceadminadd")
            ) {
                return true
            }

            // On Settings/Permission screens (e.g., App Info vs Apps List), require a tamper action keyword alongside Sheildlenz.
            return TAMPER_ACTION_KEYWORDS.any { keyword -> text.contains(keyword) }
        }

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
