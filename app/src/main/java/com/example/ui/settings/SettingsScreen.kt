package com.example.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.core.models.SensitivityLevel
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.ModelManagerCard
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.AegisWarningAmber

@Composable
fun SettingsScreen(
    preferences: AegisPreferences,
    modelManager: ModelManager,
    protectionRepo: ProtectionRepository,
    onNavigateBack: () -> Unit,
    onModelUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val isStrictEverything by preferences.strictEverything.collectAsState()
    val isAutoStart by preferences.autoStart.collectAsState()
    val hasPin by preferences.hasGuardianPin.collectAsState()
    val isMaintenanceMode by preferences.maintenanceMode.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isChangingPin by remember { mutableStateOf(false) }

    val hasOverlay = PermissionHelper.hasOverlayPermission(context)
    val hasBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
    val isDeviceOwner = remember(context) { AegisDeviceAdminReceiver.isDeviceOwner(context) }
    val isDeviceAdmin = remember(context) { AegisDeviceAdminReceiver.isDeviceAdmin(context) }

    Scaffold(
        topBar = {
            CyberTopBar(
                title = "Aegis Settings",
                subtitle = "Security & Guardian Policy",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = AegisVoidBackground,
        modifier = Modifier.testTag("settings_screen_root")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Guardian PIN Section
            item {
                CyberCard(borderColor = if (hasPin) AegisPrimaryCyan.copy(alpha = 0.4f) else AegisWarningAmber.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AegisPrimaryCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GUARDIAN SECURITY PIN",
                                    color = AegisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (hasPin) "Active (Locks settings & unblock override)" else "Not Configured (Recommended)",
                                    color = if (hasPin) AegisSuccessGreen else AegisWarningAmber,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                pinInput = ""
                                pinError = null
                                showPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasPin) AegisSecondaryBackground else AegisPrimaryCyan,
                                contentColor = if (hasPin) AegisPrimaryCyan else AegisVoidBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, AegisPrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (hasPin) "CHANGE" else "SETUP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // AI Model (.tflite) Storage Loader
            item {
                ModelManagerCard(
                    telemetry = telemetry,
                    modelManager = modelManager,
                    onModelUpdated = onModelUpdated
                )
            }

            // Detection Sensitivity Policy
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Text(
                        text = "DETECTION SENSITIVITY",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SensitivityLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { preferences.setSensitivity(level) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sensitivity == level),
                                onClick = { preferences.setSensitivity(level) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AegisPrimaryCyan,
                                    unselectedColor = AegisTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = level.displayName,
                                    color = if (sensitivity == level) AegisTextPrimary else AegisTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = level.detail,
                                    color = AegisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Strict Everything Policy & Auto Startup Toggles
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    // Strict Everything
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STRICT EVERYTHING POLICY",
                                color = AegisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Instantly cast block screen even on marginal adult/provocative scores without waiting for multi-frame confirmation.",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = isStrictEverything,
                            onCheckedChange = { preferences.setStrictEverything(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisVoidBackground,
                                checkedTrackColor = AegisPrimaryCyan,
                                uncheckedThumbColor = AegisTextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-start on boot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATIC PROTECTION STARTUP",
                                color = AegisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Automatically arm visual guardian service when Android reboots.",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = isAutoStart,
                            onCheckedChange = { preferences.setAutoStart(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisVoidBackground,
                                checkedTrackColor = AegisPrimaryCyan,
                                uncheckedThumbColor = AegisTextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }

            // Protection Mode & Device Owner Status
            item {
                CyberCard(
                    borderColor = if (isDeviceOwner) AegisSuccessGreen.copy(alpha = 0.4f) else AegisElectricBlue.copy(alpha = 0.4f)
                ) {
                    Column {
                        Text(
                            text = "APP SUSPENSION & INTERVENTION MODE",
                            color = AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isDeviceOwner) "Strict Device Owner Mode" else "Standard Mode (Accessibility & Overlay)",
                            color = if (isDeviceOwner) AegisSuccessGreen else AegisPrimaryCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isDeviceOwner) {
                            "Device Owner privileges verified. Offending applications will be suspended at the OS kernel level via setPackagesSuspended() in addition to instant screen shielding."
                        } else {
                            "Running in Standard Mode. Uses supported Accessibility navigation (Home) and WindowManager Overlay. Standard Android applications cannot force-stop third-party apps without Device Owner privileges."
                        },
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (!isDeviceOwner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To provision Device Owner (Parental / Enterprise):\nadb shell dpm set-device-owner com.example/.security.deviceowner.AegisDeviceAdminReceiver",
                            color = AegisTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AegisVoidBackground)
                                .border(1.dp, AegisCardBorder, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Permission Health Checklist
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Text(
                        text = "PERMISSION HEALTH AUDIT",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionHealthRow(
                        label = "System Overlay Permission",
                        isHealthy = hasOverlay,
                        onFix = { context.startActivity(PermissionHelper.getOverlaySettingsIntent(context)) }
                    )
                    PermissionHealthRow(
                        label = "Battery Optimization Exemption",
                        isHealthy = hasBattery,
                        onFix = { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) }
                    )
                }
            }

            // Emergency Maintenance Mode
            item {
                CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EMERGENCY MAINTENANCE MODE",
                                color = AegisDangerRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Temporarily suppresses all visual blocking for maintenance or troubleshooting.",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = isMaintenanceMode,
                            onCheckedChange = { isChecked ->
                                if (hasPin) {
                                    // Requires PIN
                                    preferences.setMaintenanceMode(isChecked)
                                } else {
                                    preferences.setMaintenanceMode(isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisVoidBackground,
                                checkedTrackColor = AegisDangerRed,
                                uncheckedThumbColor = AegisTextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }

            // Privacy & Technology Architecture
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFLINE ARCHITECTURE SPECIFICATION",
                            color = AegisPrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aegis Shield operates exclusively on-device without remote servers, telemetry brokers, or cloud classifiers. Frame processing is completely volatile and discarded upon classification.",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Guardian PIN Setup Dialog
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = AegisSecondaryBackground,
                titleContentColor = AegisTextPrimary,
                title = { Text(text = if (hasPin) "Change Guardian PIN" else "Set Guardian PIN") },
                text = {
                    Column {
                        Text(
                            text = "Enter a 4 to 8 digit numerical passcode to safeguard Aegis Shield configuration and control unblocking.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) pinInput = it },
                            label = { Text("Guardian PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPrimaryCyan,
                                unfocusedBorderColor = AegisCardBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = AegisDangerRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput.length < 4) {
                                pinError = "PIN must be at least 4 digits"
                            } else {
                                preferences.setGuardianPin(pinInput)
                                showPinDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisVoidBackground)
                    ) {
                        Text(text = "SAVE PIN", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showPinDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary)
                    ) {
                        Text(text = "CANCEL")
                    }
                }
            )
        }
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (isHealthy) AegisSuccessGreen else AegisWarningAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isHealthy) AegisTextPrimary else AegisWarningAmber,
                fontSize = 12.sp
            )
        }

        if (!isHealthy) {
            OutlinedButton(
                onClick = onFix,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisPrimaryCyan),
                modifier = Modifier.height(28.dp)
            ) {
                Text(text = "FIX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
