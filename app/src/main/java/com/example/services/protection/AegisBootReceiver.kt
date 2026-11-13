package com.example.services.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
            }
        }
    }

    companion object {
        private const val TAG = "AegisBootReceiver"
    }
}
