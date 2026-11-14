package com.example.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.example.di.AegisContainer

/**
 * Handles Android PackageInstaller session callbacks so self-updates complete smoothly
 * without triggering Sheildlenz's Anti-Tamper Password lock.
 */
class ApkInstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(TAG, "PackageInstaller callback status=$status msg=$statusMessage")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    try {
                        AegisContainer.getInstance(context.applicationContext)
                            .preferences
                            .grantTemporarySettingsAuthorization()
                    } catch (_: Exception) {
                    }
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch confirm intent; trying fallback viewer", e)
                        UpdateChecker.fallbackViewInstallIfPending(context.applicationContext)
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                UpdateChecker.onPackageInstallSucceeded(context.applicationContext)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                UpdateChecker.onPackageInstallFailed("Update installation cancelled.")
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                AegisBackgroundDownloadService.stop(context.applicationContext)
                UpdateChecker.onPackageInstallFailed(
                    "Existing app uses a different signing key. Uninstall the old Sheildlenz build once, then install this update."
                )
            }
            else -> {
                Log.w(TAG, "PackageInstaller session error ($status): $statusMessage")
                val recovered = UpdateChecker.fallbackViewInstallIfPending(context.applicationContext)
                if (!recovered) {
                    UpdateChecker.onPackageInstallFailed(
                        statusMessage ?: "Auto-install failed (code $status)."
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "ApkInstallReceiver"
    }
}
