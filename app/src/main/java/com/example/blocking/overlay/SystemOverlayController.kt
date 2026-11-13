package com.example.blocking.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.LockdownIncidentInfo
import com.example.di.AegisContainer
import com.example.services.protection.AegisLockdownService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    private var countdownTitleView: TextView? = null
    private var countdownClockView: TextView? = null
    private var unlockHintView: TextView? = null
    private var pinInputView: EditText? = null
    private var unlockButtonView: Button? = null

    private var activeOverlayMode: OverlayMode = OverlayMode.NONE

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isOverlayVisible || activeOverlayMode != OverlayMode.LOCKDOWN) return
            updateLockdownTimerUi()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    @Volatile
    var isOverlayVisible: Boolean = false
        private set

    fun showLockdownOverlay(incidentInfo: LockdownIncidentInfo) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show lockdown overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            if (isOverlayVisible && overlayView != null && activeOverlayMode == OverlayMode.LOCKDOWN) {
                updateLockdownTimerUi()
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.LOCKDOWN

            try {
                val view = createLockdownLayout(incidentInfo)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                updateLockdownTimerUi()
                mainHandler.removeCallbacks(tickerRunnable)
                mainHandler.postDelayed(tickerRunnable, 1000L)
                Log.i(TAG, "Aegis 10-Hour Full-Device Lockdown Overlay presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Lockdown Overlay", e)
            }
        }
    }

    fun showTamperLockOverlay(reason: String) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show tamper overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }

            if (isOverlayVisible && overlayView != null && activeOverlayMode == OverlayMode.TAMPER) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.TAMPER

            try {
                val view = createTamperLayout(reason)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Aegis Anti-Tamper Guardian Overlay presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Anti-Tamper Overlay", e)
            }
        }
    }

    fun showBlockOverlay(
        category: ThreatCategory,
        appName: String,
        packageName: String,
        reason: String
    ) {
        val prefs = AegisContainer.getInstance(context).preferences
        if (prefs.isLockdownActive()) {
            showLockdownOverlay(prefs.lockdownIncidentInfo.value)
        } else {
            showLockdownOverlay(
                LockdownIncidentInfo(
                    appName = appName,
                    packageName = packageName,
                    categoryName = category.displayName,
                    details = reason,
                    detectedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun dismissBlockOverlay() {
        mainHandler.post {
            mainHandler.removeCallbacks(tickerRunnable)
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.NONE
        }
    }

    private fun removeCurrentViewImmediate() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            } finally {
                overlayView = null
                isOverlayVisible = false
            }
        }
    }

    private fun buildFullscreenLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            format = PixelFormat.TRANSLUCENT
            flags = (
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                )
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
    }

    private fun createLockdownLayout(incident: LockdownIncidentInfo): View {
        val prefs = AegisContainer.getInstance(context).preferences

        val scroll = ScrollView(context).apply {
            setBackgroundColor(Color.parseColor("#F5F7FB")) // Cool Classical Porcelain
            isFillViewport = true
            isClickable = true
            isFocusable = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 84, 48, 64)
        }

        val badge = TextView(context).apply {
            text = "AEGIS SHIELD • FULL DEVICE LOCKDOWN"
            setTextColor(Color.parseColor("#C62828"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundedBox("#FEE2E2", "#F5B7B1", 40f)
            setPadding(32, 14, 32, 14)
        }
        root.addView(badge)

        val title = TextView(context).apply {
            text = "Device Locked for Safety"
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 25f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 6)
        }
        root.addView(title)

        val subTitle = TextView(context).apply {
            text = "Inappropriate visual content was detected and blocked automatically."
            setTextColor(Color.parseColor("#475569"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subTitle)

        // Countdown Card
        val timerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBox("#FFFFFF", "#D6E0EE", 28f)
            setPadding(36, 28, 36, 28)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        countdownTitleView = TextView(context).apply {
            text = "REMAINING LOCKDOWN PENALTY"
            setTextColor(Color.parseColor("#1B3B6F"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        timerCard.addView(countdownTitleView)

        countdownClockView = TextView(context).apply {
            text = "10:00:00"
            setTextColor(Color.parseColor("#C62828"))
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        timerCard.addView(countdownClockView)

        unlockHintView = TextView(context).apply {
            text = "Guardian password prompt unlocks after countdown completes."
            setTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            gravity = Gravity.CENTER
        }
        timerCard.addView(unlockHintView)
        root.addView(timerCard)

        root.addView(verticalSpacer(24))

        // Detailed Forensics Card: Tell EVERYTHING that was detected
        val timestampText = if (incident.detectedAtMillis > 0L) {
            SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault())
                .format(Date(incident.detectedAtMillis))
        } else {
            "Just now"
        }

        val forensicsCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#FFFFFF", "#D6E0EE", 28f)
            setPadding(32, 26, 32, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val forensicsHeading = TextView(context).apply {
            text = "INCIDENT FORENSICS — WHAT WAS DETECTED"
            setTextColor(Color.parseColor("#1B3B6F"))
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        forensicsCard.addView(forensicsHeading)

        val forensicsBody = TextView(context).apply {
            val pkgDisplay = if (incident.packageName.isNotBlank()) " (${incident.packageName})" else ""
            text = buildString {
                append("• Application: ${incident.appName}$pkgDisplay\n")
                append("• Threat Category: ${incident.categoryName}\n")
                append("• Detected At: $timestampText\n")
                append("• Evidence / AI Scores:\n${incident.details.ifBlank { "Threshold violated on screen frame" }}\n")
                append("• Actions Taken: Offending app closed from screen & Recents guard armed, background process stopped, ${prefs.lockdownHours.value}-hour device lock engaged.")
            }
            setTextColor(Color.parseColor("#334155"))
            textSize = 12f
            setLineSpacing(6f, 1.0f)
        }
        forensicsCard.addView(forensicsBody)
        root.addView(forensicsCard)

        root.addView(verticalSpacer(28))

        // Guardian Password Unlock Container
        val pinCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#FFFFFF", "#D6E0EE", 28f)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val pinLabel = TextView(context).apply {
            text = "GUARDIAN PASSWORD UNLOCK"
            setTextColor(Color.parseColor("#1B3B6F"))
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        pinCard.addView(pinLabel)

        val pinField = EditText(context).apply {
            hint = "Enter Guardian Password / PIN"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor("#0F172A"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = roundedBox("#F1F5FB", "#CBD5E1", 18f)
            setPadding(24, 20, 24, 20)
        }
        pinInputView = pinField
        pinCard.addView(pinField)

        pinCard.addView(verticalSpacer(14))

        val unlockBtn = Button(context).apply {
            text = "VERIFY GUARDIAN PASSWORD"
            setTextColor(Color.WHITE)
            background = roundedBox("#1B3B6F", "#1B3B6F", 18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                if (!prefs.canAcceptPasswordForLockdown()) {
                    val rem = formatDuration(prefs.getRemainingLockdownMillis())
                    Toast.makeText(
                        context,
                        "Device is locked. Password unlock available in $rem.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val entered = pinField.text.toString()
                if (prefs.verifyGuardianPin(entered)) {
                    prefs.clearLockdown()
                    prefs.unlockGuardianSession()
                    AegisLockdownService.stop(context)
                    dismissBlockOverlay()
                    Toast.makeText(context, "Guardian verified. Device unlocked.", Toast.LENGTH_SHORT).show()
                } else {
                    pinField.setText("")
                    Toast.makeText(context, "Incorrect Guardian Password.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        unlockButtonView = unlockBtn
        pinCard.addView(unlockBtn)
        root.addView(pinCard)

        root.addView(verticalSpacer(18))

        val emergencyBtn = Button(context).apply {
            text = "EMERGENCY DIALER"
            setTextColor(Color.parseColor("#C62828"))
            background = roundedBox("#FFFFFF", "#F5B7B1", 18f)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(dialIntent)
                } catch (_: Exception) {
                }
            }
        }
        root.addView(emergencyBtn)

        scroll.addView(root)
        return scroll
    }

    private fun createTamperLayout(reason: String): View {
        val prefs = AegisContainer.getInstance(context).preferences

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F5F7FB"))
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        val badge = TextView(context).apply {
            text = "ANTI-TAMPER PROTECTION ACTIVE"
            setTextColor(Color.parseColor("#B45309"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = roundedBox("#FEF3C7", "#FCD34D", 40f)
            setPadding(28, 12, 28, 12)
        }
        root.addView(badge)

        val title = TextView(context).apply {
            text = "Guardian Authorization Required"
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 26, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = reason
            setTextColor(Color.parseColor("#475569"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(24, 16, 24, 24)
            background = roundedBox("#FFFFFF", "#D6E0EE", 24f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(22))

        val pinField = EditText(context).apply {
            hint = "Enter Guardian Password / PIN"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor("#0F172A"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = roundedBox("#FFFFFF", "#CBD5E1", 18f)
            setPadding(28, 22, 28, 22)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(pinField)

        root.addView(verticalSpacer(16))

        val verifyBtn = Button(context).apply {
            text = "AUTHORIZE GUARDIAN (2 MIN WINDOW)"
            setTextColor(Color.WHITE)
            background = roundedBox("#1B3B6F", "#1B3B6F", 18f)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val entered = pinField.text.toString()
                if (prefs.verifyGuardianPin(entered)) {
                    prefs.unlockGuardianSession(grantSettingsWindowMinutes = 2)
                    dismissBlockOverlay()
                    Toast.makeText(context, "Guardian verified. System settings authorized for 2 minutes.", Toast.LENGTH_SHORT).show()
                } else {
                    pinField.setText("")
                    Toast.makeText(context, "Incorrect Guardian Password.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(verifyBtn)

        root.addView(verticalSpacer(14))

        val homeBtn = Button(context).apply {
            text = "RETURN TO HOME SCREEN"
            setTextColor(Color.parseColor("#1B3B6F"))
            background = roundedBox("#FFFFFF", "#D6E0EE", 18f)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                dismissBlockOverlay()
            }
        }
        root.addView(homeBtn)

        return root
    }

    private fun updateLockdownTimerUi() {
        val prefs = AegisContainer.getInstance(context).preferences
        val remainingMs = prefs.getRemainingLockdownMillis()
        val canEnterPassword = prefs.canAcceptPasswordForLockdown()

        countdownClockView?.text = formatDuration(remainingMs)

        if (remainingMs > 0L) {
            countdownTitleView?.text = "REMAINING LOCKDOWN PENALTY"
            countdownClockView?.setTextColor(Color.parseColor("#C62828"))
            if (!canEnterPassword) {
                unlockHintView?.text = "Whole device is locked. Guardian password input activates when the timer reaches 00:00:00."
                pinInputView?.visibility = View.GONE
                unlockButtonView?.isEnabled = false
                unlockButtonView?.text = "PASSWORD LOCKED (${formatDuration(remainingMs)})"
                unlockButtonView?.background = roundedBox("#CBD5E1", "#CBD5E1", 18f)
                unlockButtonView?.setTextColor(Color.parseColor("#475569"))
            } else {
                unlockHintView?.text = "Lockdown active. Only Guardian Password can unlock early."
                pinInputView?.visibility = View.VISIBLE
                unlockButtonView?.isEnabled = true
                unlockButtonView?.text = "VERIFY GUARDIAN PASSWORD"
                unlockButtonView?.background = roundedBox("#1B3B6F", "#1B3B6F", 18f)
                unlockButtonView?.setTextColor(Color.WHITE)
            }
        } else {
            countdownTitleView?.text = "COOLDOWN COMPLETE • PASSWORD REQUIRED"
            countdownClockView?.text = "00:00:00"
            countdownClockView?.setTextColor(Color.parseColor("#0F7B4D"))
            unlockHintView?.text = "Enter the Guardian Password below to restore access to this device."
            pinInputView?.visibility = View.VISIBLE
            unlockButtonView?.isEnabled = true
            unlockButtonView?.text = "UNLOCK DEVICE WITH PASSWORD"
            unlockButtonView?.background = roundedBox("#1B3B6F", "#1B3B6F", 18f)
            unlockButtonView?.setTextColor(Color.WHITE)
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun verticalSpacer(heightPx: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, heightPx)
        }
    }

    private fun roundedBox(fillHex: String, strokeHex: String, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            setStroke(2, Color.parseColor(strokeHex))
            cornerRadius = radiusPx
        }
    }

    private enum class OverlayMode {
        NONE,
        LOCKDOWN,
        TAMPER
    }

    companion object {
        private const val TAG = "SystemOverlay"
    }
}
