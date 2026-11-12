package com.example.blocking.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
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
import android.widget.TextView
import android.widget.Toast
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService

class SystemOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    @Volatile
    var isOverlayVisible: Boolean = false
        private set

    /**
     * Injects the full-screen Aegis Shield protection barrier into the Android WindowManager.
     * Guaranteed to overlay all apps including local video players and third-party browsers.
     */
    fun showBlockOverlay(
        category: ThreatCategory,
        appName: String,
        packageName: String,
        reason: String
    ) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            if (isOverlayVisible && overlayView != null) {
                Log.d(TAG, "Overlay is already active.")
                return@post
            }

            try {
                val layoutParams = WindowManager.LayoutParams().apply {
                    type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                    format = PixelFormat.TRANSLUCENT
                    flags = (
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    )
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    gravity = Gravity.CENTER
                }

                val view = createOverlayLayout(category, appName, packageName, reason)
                windowManager?.addView(view, layoutParams)
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Aegis System Window Overlay successfully presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject System Window Overlay into WindowManager", e)
            }
        }
    }

    fun dismissBlockOverlay() {
        mainHandler.post {
            if (overlayView != null) {
                try {
                    windowManager?.removeView(overlayView)
                    Log.i(TAG, "Aegis System Window Overlay dismissed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing overlay view", e)
                } finally {
                    overlayView = null
                    isOverlayVisible = false
                }
            }
        }
    }

    private fun createOverlayLayout(
        category: ThreatCategory,
        appName: String,
        packageName: String,
        reason: String
    ): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F7080B14")) // 97% deep void blackout
            setPadding(48, 64, 48, 64)
            isClickable = true
            isFocusable = true
        }

        // Warning Badge
        val badge = TextView(context).apply {
            text = "⚠ AEGIS INTERVENTION ACTIVE"
            setTextColor(Color.parseColor("#FF3355"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#33FF3355"))
            setPadding(32, 16, 32, 16)
        }
        root.addView(badge)

        // Large Shield Title
        val title = TextView(context).apply {
            text = "CONTENT BLOCKED"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 8)
        }
        root.addView(title)

        // Category Tag
        val categoryText = TextView(context).apply {
            text = category.displayName.uppercase()
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(categoryText)

        // Target Info Box
        val infoBox = TextView(context).apply {
            text = "Target: $appName ($packageName)\n\n$reason\n\nThe screen has been shielded to uphold Aegis Content Protection Policies."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.parseColor("#151C2C"))
        }
        root.addView(infoBox)

        // Spacer
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, 48)
        }
        root.addView(spacer)

        // Return to Home Screen Button
        val homeButton = Button(context).apply {
            text = "RETURN TO HOME SCREEN"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF3355"))
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setOnClickListener {
                // 1. Accessibility Global Action Home
                AegisAccessibilityService.performEmergencyHomeAction()

                // 2. Fallback Home Intent
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)

                // Dismiss overlay once safely on home screen
                dismissBlockOverlay()
            }
        }
        root.addView(homeButton)

        // Guardian Override PIN Layout
        val pinContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        val pinInput = EditText(context).apply {
            hint = "Enter Guardian PIN to Override"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
            textSize = 12f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(24, 16, 24, 16)
        }
        pinContainer.addView(pinInput)

        val overrideButton = Button(context).apply {
            text = "GUARDIAN UNBLOCK"
            setTextColor(Color.parseColor("#00E5FF"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setOnClickListener {
                val enteredPin = pinInput.text.toString()
                val prefs = AegisContainer.getInstance(context).preferences
                if (prefs.verifyGuardianPin(enteredPin)) {
                    Toast.makeText(context, "Guardian PIN Verified. Unblocking.", Toast.LENGTH_SHORT).show()
                    dismissBlockOverlay()
                } else {
                    Toast.makeText(context, "Incorrect Guardian PIN.", Toast.LENGTH_SHORT).show()
                    pinInput.setText("")
                }
            }
        }
        pinContainer.addView(overrideButton)

        root.addView(pinContainer)

        return root
    }

    companion object {
        private const val TAG = "SystemOverlay"
    }
}
