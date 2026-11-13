package com.example.ui.protection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.MetricTile
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun LiveProtectionScreen(
    protectionRepo: ProtectionRepository,
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val activeAppName by protectionRepo.activeAppLabel.collectAsState()
    val masterKeyGate = rememberMasterKeyGate(preferences)

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasAccessibility by remember {
        mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context, AegisAccessibilityService::class.java))
    }
    var hasBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
                    context,
                    AegisAccessibilityService::class.java
                )
                hasBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                protectionRepo.refreshPermissionHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("live_protection_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Status",
                    subtitle = "AI Classifications, Telemetry & Health"
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top: AI Classifications Widget
                item {
                    LuxurySectionHeader(title = "AI Classifications", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(borderColor = AegisPurple.copy(alpha = 0.28f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AegisPurple.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = AegisPurple, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AI Classification Pipeline",
                                    color = AegisTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                            }

                            StatusPill(
                                label = if (telemetry.isProtectionEnabled) "ENGAGED" else "OFFLINE",
                                isActive = telemetry.isProtectionEnabled,
                                activeColor = AegisSuccessGreen,
                                inactiveColor = AegisTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TelemetryDataRow(label = "Active Monitored App", value = activeAppName)
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
                        TelemetryDataRow(label = "Model Status", value = telemetry.modelStatusMessage)
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
                        TelemetryDataRow(label = "Loaded File", value = telemetry.loadedModelName ?: "None (Load .tflite)")
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
                        TelemetryDataRow(label = "Input Tensor", value = telemetry.modelInputSpec)
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
                        TelemetryDataRow(
                            label = "Output Format",
                            value = if (telemetry.modelOutputClasses > 0) "${telemetry.modelOutputClasses}-Class Auto" else "Unconfigured"
                        )
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
                        TelemetryDataRow(
                            label = "Last Category",
                            value = telemetry.lastDetectedCategory?.displayName ?: "Clean Screen Stream"
                        )
                    }
                }

                // 2. Live Rates & Frames Widgets
                item {
                    LuxurySectionHeader(title = "Live Rates & Frames")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "Inspection Rate",
                            value = if (telemetry.isProtectionEnabled) "%.1f FPS".format(telemetry.currentInspectionFps) else "0.0 FPS",
                            icon = Icons.Default.Speed,
                            accentColor = AegisPrimaryCyan,
                            caption = "Live speed",
                            modifier = Modifier.weight(1f)
                        )

                        MetricTile(
                            title = "Frames Scanned",
                            value = telemetry.framesProcessedCount.toString(),
                            icon = Icons.Default.Camera,
                            accentColor = AegisElectricBlue,
                            caption = "Volatile RAM",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Last: Permissions Widget (Permission Health Audit)
                item {
                    LuxurySectionHeader(title = "Permissions")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(borderColor = AegisCardBorder) {
                        Text(
                            text = "Permission Health Audit",
                            color = AegisTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionHealthRow(
                            label = "System Overlay Permission",
                            isHealthy = hasOverlay,
                            onFix = {
                                masterKeyGate.request("open Overlay settings") {
                                    preferences.grantTemporarySettingsAuthorization()
                                    try {
                                        context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
                        PermissionHealthRow(
                            label = "Accessibility Window Sensor",
                            isHealthy = hasAccessibility,
                            onFix = {
                                masterKeyGate.request("open Accessibility settings") {
                                    preferences.grantTemporarySettingsAuthorization()
                                    try {
                                        context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
                        PermissionHealthRow(
                            label = "Device Administrator Protection",
                            isHealthy = isDeviceAdmin,
                            onFix = {
                                masterKeyGate.request("activate Device Admin") {
                                    preferences.grantTemporarySettingsAuthorization()
                                    try {
                                        context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
                        PermissionHealthRow(
                            label = "Battery Optimization Exemption",
                            isHealthy = hasBattery,
                            onFix = {
                                masterKeyGate.request("open Battery settings") {
                                    preferences.grantTemporarySettingsAuthorization()
                                    try {
                                        context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextSecondary, fontSize = 12.sp, fontFamily = ClassicalBodyFont)
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PermissionHealthRow(
    label: String,
    isHealthy: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (isHealthy) AegisSuccessGreen else AegisWarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isHealthy) AegisTextPrimary else AegisWarningAmber,
                fontSize = 13.sp,
                fontFamily = ClassicalBodyFont,
                fontWeight = FontWeight.Medium
            )
        }

        if (!isHealthy) {
            OutlinedButton(
                onClick = onFix,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisPrimaryCyan),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "FIX", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
            }
        }
    }
}
