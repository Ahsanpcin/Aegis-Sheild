package com.example.services.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.blocking.overlay.SystemOverlayController
import com.example.di.AegisContainer

class AegisLockdownService : Service() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController by lazy { SystemOverlayController(this) }
    private val watchdogHandler = Handler(Looper.getMainLooper())

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (container.preferences.isLockdownActive()) {
                overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
                watchdogHandler.postDelayed(this, 2000L)
            } else {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LOCKDOWN -> {
                watchdogHandler.removeCallbacks(watchdogRunnable)
                overlayController.dismissBlockOverlay()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!container.preferences.isLockdownActive()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
                watchdogHandler.removeCallbacks(watchdogRunnable)
                watchdogHandler.postDelayed(watchdogRunnable, 2000L)
                Log.i(TAG, "AegisLockdownService foreground watchdog engaged.")
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogHandler.removeCallbacks(watchdogRunnable)
        overlayController.dismissBlockOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aegis Shield Device Lockdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enforces active Aegis Shield device lockdown timer across restarts"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aegis Shield Lockdown Active")
            .setContentText("Device locked due to detected content violation")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AegisLockdownService"
        const val CHANNEL_ID = "aegis_lockdown_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START_LOCKDOWN = "com.example.action.START_LOCKDOWN"
        const val ACTION_STOP_LOCKDOWN = "com.example.action.STOP_LOCKDOWN"

        fun start(context: Context) {
            val intent = Intent(context, AegisLockdownService::class.java).apply {
                action = ACTION_START_LOCKDOWN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AegisLockdownService::class.java).apply {
                action = ACTION_STOP_LOCKDOWN
            }
            context.startService(intent)
        }
    }
}
