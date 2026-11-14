package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.permissions.PermissionHelper
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasBatteryOpt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

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

    SheildlenzBackdrop(modifier = Modifier.testTag("onboarding_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Setup"
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    LuxurySectionHeader(title = "Required Permissions")
                }

                item {
                    PermissionExplainerCard(
                        title = "System Overlay",
                        icon = Icons.Default.Layers,
                        accentColor = AegisPrimaryCyan,
                        isGranted = hasOverlay,
                        explanation = "Required to display the block shield over other apps.",
                        actionLabel = if (hasOverlay) "Granted" else "Grant Overlay",
                        onAction = {
                            try {
                                context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                            } catch (_: Exception) {
                                context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                            }
                        }
                    )
                }

                item {
                    PermissionExplainerCard(
                        title = "Accessibility Sensor",
                        icon = Icons.Default.Visibility,
                        accentColor = AegisPurple,
                        isGranted = false,
                        explanation = "Identifies foreground apps and enforces anti-tamper.",
                        actionLabel = "Open Accessibility",
                        onAction = {
                            try {
                                context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                            } catch (_: Exception) {
                            }
                        }
                    )
                }

                item {
                    PermissionExplainerCard(
                        title = "Battery Exemption",
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = AegisSuccessGreen,
                        isGranted = hasBatteryOpt,
                        explanation = "Keeps the shield watchdog active in background.",
                        actionLabel = if (hasBatteryOpt) "Excluded" else "Allow Unrestricted",
                        onAction = {
                            try {
                                context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                            } catch (_: Exception) {
                                context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                            }
                        }
                    )
                }

                item {
                    PermissionExplainerCard(
                        title = "Device Administrator",
                        icon = Icons.Default.AdminPanelSettings,
                        accentColor = AegisDangerRed,
                        isGranted = isDeviceAdmin,
                        explanation = "Locks uninstall and deactivation behind Guardian PIN.",
                        actionLabel = if (isDeviceAdmin) "Active" else "Activate Admin",
                        onAction = {
                            try {
                                context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                            } catch (_: Exception) {
                                context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                            }
                        }
                    )
                }

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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "ENTER SHEILDLENZ HOME",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.9.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
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
    val cardTone = when (accentColor) {
        AegisPurple -> CyberCardTone.LAVENDER
        AegisChampagneGold, AegisDangerRed -> CyberCardTone.GOLD
        else -> CyberCardTone.SKY
    }
    val cardBorder = when (cardTone) {
        CyberCardTone.LAVENDER -> AegisLavenderBorder
        CyberCardTone.GOLD -> AegisGoldBorder
        else -> AegisCardBorder
    }

    CyberCard(
        borderColor = cardBorder,
        tone = cardTone
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.72f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = AegisOnPrimaryWhite, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = AegisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = AegisPrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = explanation,
            color = AegisTextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = ClassicalBodyFont
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AegisPrimaryCyan.copy(alpha = 0.14f))
                    .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ACTIVE & CONFIGURED",
                    color = AegisPrimaryCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.8.sp
                )
            }
        } else if (isInformationalOnly) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel.uppercase(),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.8.sp
                )
            }
        } else {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = AegisOnPrimaryWhite
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = actionLabel.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}
