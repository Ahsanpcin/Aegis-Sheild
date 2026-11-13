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
import androidx.core.content.res.ResourcesCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.LockdownIncidentInfo
import com.example.di.AegisContainer
import com.example.services.protection.AegisLockdownService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val displayTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.SANS_SERIF
    }
    private var overlayView: View? = null

    private var countdownTitleView: TextView? = null
    private var countdownClockView: TextView? = null
    private var unlockHintView: TextView? = null
    private var pinInputView: EditText? = null
    private var unlockButtonView: Button? = null
    private var doubleTimeButtonView: Button? = null
    private var shieldNoteView: TextView? = null

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
                countdownTitleView = null
                countdownClockView = null
                unlockHintView = null
                pinInputView = null
                unlockButtonView = null
                doubleTimeButtonView = null
                shieldNoteView = null
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
            setBackgroundColor(Color.parseColor("#F7FAFF")) // Airy Pearl-Sky Canvas
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
            text = "SHEILDLENZ • FULL DEVICE LOCKDOWN"
            setTextColor(Color.parseColor("#D99B26"))
            textSize = 11f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundedBox("#FFF8E7", "#E8C374", 50f)
            setPadding(36, 14, 36, 14)
        }
        root.addView(badge)

        val title = TextView(context).apply {
            text = "Device Locked for Safety"
            setTextColor(Color.parseColor("#1E4494"))
            textSize = 26f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 6)
        }
        root.addView(title)

        val subTitle = TextView(context).apply {
            text = "Inappropriate visual content was detected and blocked automatically."
            setTextColor(Color.parseColor("#5272B3"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subTitle)

        // Countdown Card
        val timerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBox("#FFFFFF", "#D6E4FA", 36f)
            setPadding(36, 30, 36, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        countdownTitleView = TextView(context).apply {
            text = "REMAINING LOCKDOWN PENALTY"
            setTextColor(Color.parseColor("#2B6CE6"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        timerCard.addView(countdownTitleView)

        countdownClockView = TextView(context).apply {
            text = "10:00:00"
            setTextColor(Color.parseColor("#D99B26"))
            textSize = 38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        timerCard.addView(countdownClockView)

        unlockHintView = TextView(context).apply {
            text = "Guardian password prompt unlocks after countdown completes."
            setTextColor(Color.parseColor("#728FC7"))
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
            background = roundedBox("#FFFFFF", "#D6E4FA", 36f)
            setPadding(34, 28, 34, 28)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val forensicsHeading = TextView(context).apply {
            text = "INCIDENT FORENSICS — WHAT WAS DETECTED"
            setTextColor(Color.parseColor("#2B6CE6"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
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
                val stage1Label = AegisPreferences.formatMinutesLabel(prefs.lockdownDurationMinutes.value)
                val doubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
                append("• Actions Taken: Offending app closed & Recents guard armed, background process stopped, $stage1Label lock (or $doubleLabel Double-Time wait without Master Code). Sheildlenz Shield remains ON after unlock.")
            }
            setTextColor(Color.parseColor("#5272B3"))
            textSize = 12f
            setLineSpacing(6f, 1.0f)
        }
        forensicsCard.addView(forensicsBody)
        root.addView(forensicsCard)

        root.addView(verticalSpacer(28))

        // Master Code & Double-Time Unlock Container
        val pinCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#FFFFFF", "#D6E4FA", 36f)
            setPadding(34, 26, 34, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val pinLabel = TextView(context).apply {
            text = "MASTER CODE / DOUBLE-TIME UNLOCK"
            setTextColor(Color.parseColor("#2B6CE6"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        pinCard.addView(pinLabel)

        val pinField = EditText(context).apply {
            hint = "Enter Guardian Master Code / PIN"
            setHintTextColor(Color.parseColor("#728FC7"))
            setTextColor(Color.parseColor("#1E4494"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = roundedBox("#F3F8FF", "#D6E4FA", 22f)
            setPadding(24, 22, 24, 22)
        }
        pinInputView = pinField
        pinCard.addView(pinField)

        pinCard.addView(verticalSpacer(14))

        val unlockBtn = Button(context).apply {
            text = "UNLOCK WITH MASTER CODE"
            setTextColor(Color.WHITE)
            background = roundedBox("#2B6CE6", "#2B6CE6", 24f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                if (!prefs.canAcceptPasswordForLockdown()) {
                    val rem = formatDuration(prefs.getRemainingLockdownMillis())
                    Toast.makeText(
                        context,
                        "Device is locked. Master Code entry opens in $rem.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val entered = pinField.text.toString()
                if (prefs.verifyGuardianPin(entered)) {
                    prefs.clearLockdown()
                    prefs.setShieldDesiredActive(true)
                    prefs.unlockGuardianSession()
                    AegisLockdownService.stop(context)
                    dismissBlockOverlay()
                    Toast.makeText(
                        context,
                        "Master Code verified. Device unlocked • Sheildlenz Shield remains ON.",
                        Toast.LENGTH_LONG
                    ).show()
                    ensureShieldRemainsOnAfterUnlock()
                } else {
                    pinField.setText("")
                    val dblLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes()).uppercase(Locale.US)
                    val fallbackHint = if (!prefs.isInDoubleTimeStage()) {
                        "Incorrect Master Code. Tap 'Wait Double Time ($dblLabel)' if you don't know the code."
                    } else {
                        "Incorrect Master Code. Wait for the Double-Time countdown to reach 00:00:00."
                    }
                    Toast.makeText(context, fallbackHint, Toast.LENGTH_LONG).show()
                }
            }
        }
        unlockButtonView = unlockBtn
        pinCard.addView(unlockBtn)

        pinCard.addView(verticalSpacer(12))

        val initialDoubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes()).uppercase(Locale.US)
        val doubleTimeBtn = Button(context).apply {
            text = "DON'T KNOW CODE? WAIT DOUBLE TIME ($initialDoubleLabel)"
            setTextColor(Color.parseColor("#D99B26"))
            background = roundedBox("#FFF8E7", "#E8C374", 24f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            setPadding(20, 22, 20, 22)
            visibility = View.GONE
            setOnClickListener {
                prefs.engageDoubleTimeLockdown()
                AegisLockdownService.start(context)
                updateLockdownTimerUi()
                val dblLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
                Toast.makeText(
                    context,
                    "Double-Time Wait ($dblLabel) engaged. Shield stays ON after unlock.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        doubleTimeButtonView = doubleTimeBtn
        pinCard.addView(doubleTimeBtn)

        pinCard.addView(verticalSpacer(12))

        val shieldNote = TextView(context).apply {
            text = "SHIELD CONTINUITY: Sheildlenz visual shield stays ON after both Master Code unlock and Double-Time completion."
            setTextColor(Color.parseColor("#2B6CE6"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        shieldNoteView = shieldNote
        pinCard.addView(shieldNote)

        root.addView(pinCard)

        root.addView(verticalSpacer(18))

        val emergencyBtn = Button(context).apply {
            text = "EMERGENCY DIALER"
            setTextColor(Color.parseColor("#D99B26"))
            background = roundedBox("#FFF8E7", "#E8C374", 18f)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            visibility = if (prefs.showEmergencyDialerOnLockdown.value) View.VISIBLE else View.GONE
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

        root.addView(verticalSpacer(22))
        root.addView(createOverlayBrandingView(allowOpenLink = false))

        scroll.addView(root)
        return scroll
    }

    private fun createTamperLayout(reason: String): View {
        val prefs = AegisContainer.getInstance(context).preferences

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F7FAFF"))
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        val badge = TextView(context).apply {
            text = "ANTI-TAMPER PROTECTION ACTIVE"
            setTextColor(Color.parseColor("#D99B26"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundedBox("#FFF8E7", "#E8C374", 50f)
            setPadding(32, 12, 32, 12)
        }
        root.addView(badge)

        val title = TextView(context).apply {
            text = "Guardian Authorization Required"
            setTextColor(Color.parseColor("#1E4494"))
            textSize = 23f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 26, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = reason
            setTextColor(Color.parseColor("#5272B3"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 20, 28, 24)
            background = roundedBox("#FFFFFF", "#D6E4FA", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(22))

        val pinField = EditText(context).apply {
            hint = "Enter Guardian Password / PIN"
            setHintTextColor(Color.parseColor("#728FC7"))
            setTextColor(Color.parseColor("#1E4494"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = roundedBox("#FFFFFF", "#D6E4FA", 22f)
            setPadding(28, 22, 28, 22)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(pinField)

        root.addView(verticalSpacer(16))

        val windowMins = prefs.guardianSettingsWindowMinutes.value
        val verifyBtn = Button(context).apply {
            text = "AUTHORIZE GUARDIAN ($windowMins MIN WINDOW)"
            setTextColor(Color.WHITE)
            background = roundedBox("#2B6CE6", "#2B6CE6", 24f)
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
                    val mins = prefs.guardianSettingsWindowMinutes.value
                    prefs.unlockGuardianSession(grantSettingsWindowMinutes = mins)
                    dismissBlockOverlay()
                    Toast.makeText(context, "Guardian verified. System settings authorized for $mins minute(s).", Toast.LENGTH_SHORT).show()
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
            setTextColor(Color.parseColor("#2B6CE6"))
            background = roundedBox("#FFFFFF", "#D6E4FA", 24f)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
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

        root.addView(verticalSpacer(24))
        root.addView(createOverlayBrandingView(allowOpenLink = true))

        return root
    }

    private fun createOverlayBrandingView(allowOpenLink: Boolean): View {
        return TextView(context).apply {
            text = "Built By AhsanTCr  •  ahsantcr.lovable.app"
            setTextColor(Color.parseColor("#1E4494"))
            textSize = 11f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundedBox("#FFFFFF", "#E8C374", 50f)
            setPadding(34, 16, 34, 16)
            if (allowOpenLink) {
                setOnClickListener {
                    dismissBlockOverlay()
                    com.example.ui.components.openAhsanTcrContact(context)
                }
            }
        }
    }

    private fun updateLockdownTimerUi() {
        val prefs = AegisContainer.getInstance(context).preferences

        if (prefs.completeDoubleTimeIfExpired()) {
            onDoubleTimeUnlockCompleted()
            return
        }

        val remainingMs = prefs.getRemainingLockdownMillis()
        val canEnterPassword = prefs.canAcceptPasswordForLockdown()
        val isDoubleTime = prefs.isInDoubleTimeStage()
        val stage1Label = AegisPreferences.formatMinutesLabel(prefs.lockdownDurationMinutes.value)
        val stage1LabelUpper = stage1Label.uppercase(Locale.US)
        val doubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
        val doubleLabelUpper = doubleLabel.uppercase(Locale.US)

        countdownClockView?.text = formatDuration(remainingMs)

        when {
            isDoubleTime && remainingMs > 0L -> {
                countdownTitleView?.text = "STAGE 2 • DOUBLE-TIME WAIT ($doubleLabelUpper)"
                countdownClockView?.setTextColor(Color.parseColor("#D99B26"))
                unlockHintView?.text = "Double-time wait active. Device opens automatically when timer hits 00:00:00 (or earlier with Guardian Master Code). Sheildlenz Shield remains ON."
                pinInputView?.visibility = View.VISIBLE
                unlockButtonView?.isEnabled = true
                unlockButtonView?.text = "UNLOCK WITH MASTER CODE"
                unlockButtonView?.background = roundedBox("#2B6CE6", "#2B6CE6", 24f)
                unlockButtonView?.setTextColor(Color.WHITE)
                doubleTimeButtonView?.visibility = View.GONE
            }

            !isDoubleTime && remainingMs > 0L -> {
                countdownTitleView?.text = "STAGE 1 • LOCKDOWN TIMER ($stage1LabelUpper)"
                countdownClockView?.setTextColor(Color.parseColor("#D99B26"))
                if (!canEnterPassword) {
                    unlockHintView?.text = "Whole device is locked. After countdown completes: enter Master Code, or wait Double Time ($doubleLabel). Shield stays ON."
                    pinInputView?.visibility = View.GONE
                    unlockButtonView?.isEnabled = false
                    unlockButtonView?.text = "MASTER CODE LOCKED (${formatDuration(remainingMs)})"
                    unlockButtonView?.background = roundedBox("#EEF4FF", "#D6E4FA", 24f)
                    unlockButtonView?.setTextColor(Color.parseColor("#5272B3"))
                    doubleTimeButtonView?.visibility = View.GONE
                } else {
                    unlockHintView?.text = "Enter Master Code to unlock, or tap Wait Double Time ($doubleLabel) if you do not know the code. Shield remains ON."
                    pinInputView?.visibility = View.VISIBLE
                    unlockButtonView?.isEnabled = true
                    unlockButtonView?.text = "UNLOCK WITH MASTER CODE"
                    unlockButtonView?.background = roundedBox("#2B6CE6", "#2B6CE6", 24f)
                    unlockButtonView?.setTextColor(Color.WHITE)
                    doubleTimeButtonView?.visibility = View.VISIBLE
                    doubleTimeButtonView?.text = "DON'T KNOW CODE? WAIT DOUBLE TIME ($doubleLabelUpper)"
                }
            }

            else -> {
                countdownTitleView?.text = "TIME COMPLETE • MASTER CODE REQUIRED"
                countdownClockView?.text = "00:00:00"
                countdownClockView?.setTextColor(Color.parseColor("#2B6CE6"))
                unlockHintView?.text = "Initial timer finished. Enter Master Code to open now, or wait Double Time ($doubleLabel) if you don't know the code. Shield remains ON after both."
                pinInputView?.visibility = View.VISIBLE
                unlockButtonView?.isEnabled = true
                unlockButtonView?.text = "UNLOCK WITH MASTER CODE"
                unlockButtonView?.background = roundedBox("#2B6CE6", "#2B6CE6", 24f)
                unlockButtonView?.setTextColor(Color.WHITE)
                doubleTimeButtonView?.visibility = View.VISIBLE
                doubleTimeButtonView?.text = "DON'T KNOW MASTER CODE? WAIT $doubleLabelUpper"
            }
        }
    }

    private fun onDoubleTimeUnlockCompleted() {
        val container = AegisContainer.getInstance(context)
        container.preferences.setShieldDesiredActive(true)
        AegisLockdownService.stop(context)
        dismissBlockOverlay()
        Toast.makeText(
            context,
            "Double-time wait complete. Device unlocked • Sheildlenz Shield remains ON.",
            Toast.LENGTH_LONG
        ).show()
        ensureShieldRemainsOnAfterUnlock()
    }

    private fun ensureShieldRemainsOnAfterUnlock() {
        val container = AegisContainer.getInstance(context)
        container.preferences.setShieldDesiredActive(true)
        val telemetry = container.protectionRepository.telemetry.value
        if (!telemetry.isMediaProjectionActive || !telemetry.isProtectionEnabled) {
            val rearmIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_REARM_SHIELD, true)
            }
            try {
                context.startActivity(rearmIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to launch MainActivity to re-arm Sheildlenz Shield", e)
            }
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
