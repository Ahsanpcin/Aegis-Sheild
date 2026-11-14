package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.permissions.PermissionHelper
import com.example.core.update.UpdateChecker
import com.example.di.AegisContainer
import com.example.services.protection.AegisLockdownService
import com.example.services.protection.AegisProtectionService
import com.example.ui.navigation.AegisNavHost
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisVoidBackground

class MainActivity : ComponentActivity() {
    private val container by lazy { AegisContainer.getInstance(this) }
    private var promptedShieldRearmInSession = false

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            container.preferences.setShieldDesiredActive(true)
            AegisProtectionService.start(this, result.resultCode, result.data!!)
            container.protectionRepository.setProtectionEnabled(true)
            Toast.makeText(this, "Sheildlenz Armed • Screen Protection Active", Toast.LENGTH_SHORT).show()
        } else {
            container.protectionRepository.setProtectionEnabled(false)
            Toast.makeText(this, "Screen capture permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Silently check latest GitHub release vs current app version on startup
        UpdateChecker.startCheckForUpdates(showUpToDatePopup = false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            AegisShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AegisVoidBackground
                ) {
                    AegisNavHost(
                        container = container,
                        onRequestStartProtection = { requestStartProtection() },
                        onRequestStopProtection = { requestStopProtection() }
                    )
                }
            }
        }
    }

    private fun requestStartProtection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            container.preferences.beginAuthorizedExternalFlow()
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } else {
            Toast.makeText(this, "MediaProjection service unavailable on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStopProtection() {
        container.preferences.setShieldDesiredActive(false)
        AegisProtectionService.stop(this)
        container.protectionRepository.setProtectionEnabled(false)
        Toast.makeText(this, "Sheildlenz Protection Disengaged.", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REARM_SHIELD, false)) {
            promptedShieldRearmInSession = false
        }
    }

    override fun onResume() {
        super.onResume()
        container.protectionRepository.refreshPermissionHealth()
        UpdateChecker.retryPendingInstallIfAuthorized(this)
        if (container.preferences.isLockdownActive()) {
            AegisLockdownService.start(this)
        } else if (container.preferences.isShieldDesiredActive.value) {
            val telemetry = container.protectionRepository.telemetry.value
            val needsRearm = !telemetry.isMediaProjectionActive || !telemetry.isProtectionEnabled
            if (needsRearm && PermissionHelper.hasOverlayPermission(this) && !promptedShieldRearmInSession) {
                promptedShieldRearmInSession = true
                requestStartProtection()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Require Guardian re-authentication whenever the app leaves the foreground
        if (!isChangingConfigurations) {
            container.preferences.onAppBackgrounded()
        }
    }

    companion object {
        const val EXTRA_REARM_SHIELD = "extra_rearm_shield"
    }
}
