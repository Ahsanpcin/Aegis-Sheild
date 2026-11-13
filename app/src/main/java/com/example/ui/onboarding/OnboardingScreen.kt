package com.example.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.AdminPanelSettings
import com.example.core.permissions.PermissionHelper
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasBatteryOpt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

    // Re-check permissions when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasBatteryOpt = PermissionHelper.isIgnoringBatteryOptimizations(context)
                isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CyberTopBar(
                title = "Aegis Onboarding",
                subtitle = "Security & Permission Setup"
            )
        },
        containerColor = AegisVoidBackground,
        modifier = Modifier.testTag("onboarding_screen_root")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                CyberCard(borderColor = AegisPrimaryCyan.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AegisPrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AegisPrimaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ZERO-CLOUD PRIVACY SHIELD",
                                color = AegisPrimaryCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "All visual analysis occurs strictly in volatile device RAM. No images, screenshots, or logs are ever transmitted to any remote server or stored to disk.",
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Permission 1: Overlay Permission
            item {
                PermissionExplainerCard(
                    title = "System Overlay Permission",
                    icon = Icons.Default.Layers,
                    accentColor = AegisPrimaryCyan,
                    isGranted = hasOverlay,
                    explanation = "Allows Aegis Shield to instantly cast an emergency protective barrier over inappropriate content when detected in web browsers, apps, or local video players.",
                    actionLabel = if (hasOverlay) "Permission Granted" else "Grant Overlay Permission",
                    onAction = {
                        try {
                            context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                        } catch (_: Exception) {
                            context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                        }
                    }
                )
            }

            // Permission 2: MediaProjection / Screen Capture
            item {
                PermissionExplainerCard(
                    title = "Screen Monitoring (MediaProjection)",
                    icon = Icons.Default.ScreenShare,
                    accentColor = AegisElectricBlue,
                    isGranted = false, // Requested dynamically at runtime when protection is armed
                    explanation = "Android's official display-capture API allows the on-device AI model to inspect screen frames in real time. Prompted by the OS when master protection is engaged.",
                    actionLabel = "System Prompt Upon Arming",
                    onAction = { /* Activated when toggle is clicked */ },
                    isInformationalOnly = true
                )
            }

            // Permission 3: Accessibility Service
            item {
                PermissionExplainerCard(
                    title = "Accessibility Window Sensor",
                    icon = Icons.Default.Visibility,
                    accentColor = AegisPurple,
                    isGranted = false,
                    explanation = "Detects which application is currently active in the foreground and enables the emergency 'Dismiss / Home' action when offensive content is flagged.",
                    actionLabel = "Configure Accessibility",
                    onAction = {
                        try {
                            context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                        } catch (_: Exception) {
                            // Fallback
                        }
                    }
                )
            }

            // Permission 4: Battery Optimization & Foreground Service
            item {
                PermissionExplainerCard(
                    title = "Background Service & Battery Optimization",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = AegisSuccessGreen,
                    isGranted = hasBatteryOpt,
                    explanation = "Prevents aggressive Android battery management from killing the protection watchdog while you browse or watch videos.",
                    actionLabel = if (hasBatteryOpt) "Optimization Disabled" else "Exclude from Battery Saver",
                    onAction = {
                        try {
                            context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                        } catch (_: Exception) {
                            context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                        }
                    }
                )
            }

            // Permission 5: Device Administrator Anti-Tamper
            item {
                PermissionExplainerCard(
                    title = "Device Administrator (Anti-Tamper)",
                    icon = Icons.Default.AdminPanelSettings,
                    accentColor = AegisDangerRed,
                    isGranted = isDeviceAdmin,
                    explanation = "Prevents unauthorized uninstalling or disabling of Aegis Shield. Deactivation is intercepted and gated by the Guardian Password.",
                    actionLabel = if (isDeviceAdmin) "Device Admin Active" else "Activate Device Admin",
                    onAction = {
                        try {
                            context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                        } catch (_: Exception) {
                            context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                        }
                    }
                )
            }

            // Limitations & Privacy Card
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Text(
                        text = "IMPORTANT LIMITATIONS & ARCHITECTURE",
                        color = AegisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• DRM-Protected Content: Some encrypted streaming platforms (e.g., Netflix) restrict screen capture at the hardware level.\n• Local Video Players: Local media players (MP4/MKV) and web browsers are fully supported.\n• Power Usage: The AI pipeline subsamples frames dynamically to maximize battery efficiency.",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Complete Onboarding Button
            item {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_continue_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ENTER AEGIS DASHBOARD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PermissionExplainerCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isGranted: Boolean,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit,
    isInformationalOnly: Boolean = false
) {
    CyberCard(borderColor = if (isGranted) AegisSuccessGreen.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.25f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = AegisTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = AegisSuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = explanation,
            color = AegisTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AegisSuccessGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ACTIVE & CONFIGURED",
                    color = AegisSuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else if (isInformationalOnly) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel.uppercase(),
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionLabel.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
