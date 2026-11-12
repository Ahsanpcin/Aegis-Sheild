package com.example

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.di.AegisContainer
import com.example.services.protection.AegisProtectionService
import com.example.ui.navigation.AegisNavHost
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisVoidBackground

class MainActivity : ComponentActivity() {
    private val container by lazy { AegisContainer.getInstance(this) }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            AegisProtectionService.start(this, result.resultCode, result.data!!)
            container.protectionRepository.setProtectionEnabled(true)
            Toast.makeText(this, "Aegis Shield Armed • Screen Protection Active", Toast.LENGTH_SHORT).show()
        } else {
            container.protectionRepository.setProtectionEnabled(false)
            Toast.makeText(this, "Screen capture permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } else {
            Toast.makeText(this, "MediaProjection service unavailable on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStopProtection() {
        AegisProtectionService.stop(this)
        container.protectionRepository.setProtectionEnabled(false)
        Toast.makeText(this, "Aegis Shield Protection Disengaged.", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        container.protectionRepository.refreshPermissionHealth()
    }
}
