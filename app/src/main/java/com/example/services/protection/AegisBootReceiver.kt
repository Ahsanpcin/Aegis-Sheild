package com.example.services.protection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.di.AegisContainer

class AegisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = AegisContainer.getInstance(context).preferences
            if (prefs.isLockdownActive()) {
                Log.w(TAG, "Device rebooted with active lockdown — restoring AegisLockdownService.")
                AegisLockdownService.start(context)
            } else if (prefs.isShieldDesiredActive.value && prefs.autoStart.value) {
                Log.i(TAG, "Device rebooted with Sheildlenz Shield armed — notifying to re-arm screen capture.")
                showShieldRecoveryNotification(context)
            }
        }
    }

    private fun showShieldRecoveryNotification(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AegisProtectionService.CHANNEL_ID,
                "Sheildlenz Protection Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when screen capture permission needs quick re-arming after reboot"
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_REARM_SHIELD, true)
        }
        val pi = PendingIntent.getActivity(
            context,
            2001,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, AegisProtectionService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sheildlenz Shield Armed")
            .setContentText("Tap to resume live screen protection after device restart")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(REBOOT_SHIELD_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "AegisBootReceiver"
        private const val REBOOT_SHIELD_NOTIFICATION_ID = 1003
    }
}
