package com.example.security.deviceowner

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AegisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Aegis Shield Device Administrator privilege enabled.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Aegis Shield Device Administrator privilege disabled.")
    }

    companion object {
        private const val TAG = "AegisDeviceAdmin"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, AegisDeviceAdminReceiver::class.java)
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isDeviceOwnerApp(context.packageName)
        }

        fun isDeviceAdmin(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isAdminActive(getComponentName(context))
        }

        /**
         * Strict Device Owner Mode:
         * Suspends an offending package using DevicePolicyManager.setPackagesSuspended().
         * Only executable if the application is genuinely provisioned as Device Owner via ADB/NFC.
         */
        fun suspendPackageIfDeviceOwner(context: Context, packageName: String): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.d(TAG, "Cannot suspend package: Aegis Shield is not provisioned as Device Owner.")
                return false
            }

            // Never suspend critical system packages or our own app
            if (packageName == context.packageName ||
                packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName == "com.android.settings"
            ) {
                Log.w(TAG, "Exempted critical package from suspension: $packageName")
                return false
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val result = dpm.setPackagesSuspended(getComponentName(context), arrayOf(packageName), true)
                    val suspended = result != null && result.isEmpty() // Empty array means 0 failures
                    Log.i(TAG, "Suspended package $packageName: $suspended")
                    suspended
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to suspend package $packageName", e)
                    false
                }
            } else {
                false
            }
        }
    }
}
