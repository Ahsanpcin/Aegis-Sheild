package com.example.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.core.models.SensitivityLevel
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

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
    val lockdownDurationMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val doubleTimeMultiplier by preferences.doubleTimeMultiplier.collectAsState()
    val waitCooldownBeforePin by preferences.waitCooldownBeforePassword.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()

    val stage1DurationLabel = AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)
    val doubleDurationLabel = AegisPreferences.formatMinutesLabel(
        (lockdownDurationMinutes * doubleTimeMultiplier).toInt().coerceAtLeast(1)
    )

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val hasOverlay = PermissionHelper.hasOverlayPermission(context)
    val hasBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
    val isDeviceOwner = remember(context) { AegisDeviceAdminReceiver.isDeviceOwner(context) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

    SheildlenzBackdrop(modifier = Modifier.testTag("settings_screen_root")) {
        Scaffold(
            topBar = {
                CyberTopBar(
                    title = "Sheildlenz Settings",
                    subtitle = "Security & Guardian Policy",
                    onNavigateBack = onNavigateBack
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
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // Guardian PIN Section
                item {
                    LuxurySectionHeader(title = "Guardian Authority")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(borderColor = if (hasPin) AegisChampagneGold.copy(alpha = 0.45f) else AegisWarningAmber.copy(alpha = 0.45f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AegisPrimaryCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(19.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Guardian Security PIN",
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (hasPin) "Active (Locks console & admin)" else "Not Configured (Recommended)",
                                        color = if (hasPin) AegisSuccessGreen else AegisWarningAmber,
                                        fontSize = 12.sp,
                                        fontFamily = ClassicalBodyFont
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
                                    containerColor = if (hasPin) AegisSurfaceElevated else AegisPrimaryCyan,
                                    contentColor = if (hasPin) AegisPrimaryCyan else AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.border(1.dp, AegisPrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = if (hasPin) "CHANGE" else "SETUP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
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
                    LuxurySectionHeader(title = "AI Detection Thresholds")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(borderColor = AegisCardBorder) {
                        Text(
                            text = "Detection Sensitivity",
                            color = AegisTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SensitivityLevel.entries.forEach { level ->
                                val isSelected = (sensitivity == level)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) AegisPrimaryCyan.copy(alpha = 0.07f) else AegisSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) AegisPrimaryCyan.copy(alpha = 0.45f) else AegisCardBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { preferences.setSensitivity(level) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { preferences.setSensitivity(level) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = AegisPrimaryCyan,
                                            unselectedColor = AegisTextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = level.displayName,
                                            color = if (isSelected) AegisPrimaryCyan else AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalBodyFont
                                        )
                                        Text(
                                            text = level.detail,
                                            color = AegisTextMuted,
                                            fontSize = 12.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Strict Everything Policy & Auto Startup Toggles
                item {
                    CyberCard(borderColor = AegisCardBorder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Strict Everything Policy",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Instantly cast block screen even on marginal adult/provocative scores without waiting for multi-frame confirmation.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = isStrictEverything,
                                onCheckedChange = { preferences.setStrictEverything(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Automatic Protection Startup",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Automatically arm visual guardian service when Android reboots.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = isAutoStart,
                                onCheckedChange = { preferences.setAutoStart(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // 10-Hour Whole-Device Lockdown & Anti-Tamper Enforcement
                item {
                    LuxurySectionHeader(title = "Lockdown & Tamper Resistance", accentColor = AegisDangerRed)
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.30f)) {
                        Text(
                            text = "Full-Device Lockdown Penalty",
                            color = AegisDangerRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duration Before Master Code Unlock: $stage1DurationLabel",
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 4, 10, 24).forEach { hrs ->
                                val selected = (lockdownDurationMinutes == hrs * 60)
                                OutlinedButton(
                                    onClick = { preferences.setLockdownHours(hrs) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AegisPrimaryCyan else AegisSurfaceElevated,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Text("${hrs}H", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                                }
                            }
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Cooldown Before Master Code",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Lock the entire phone for $stage1DurationLabel before Master Code entry. If the Master Code is unknown after time completes, waiting Double Time ($doubleDurationLabel) unlocks the phone while keeping Shield ON.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = waitCooldownBeforePin,
                                onCheckedChange = { preferences.setWaitCooldownBeforePassword(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Settings & Admin Anti-Tamper",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Require Guardian Password before disabling Device Admin, Accessibility, Uninstall, or Date & Time.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = antiTamperEnabled,
                                onCheckedChange = { preferences.setAntiTamperEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // Protection Mode & Device Owner Status
                item {
                    CyberCard(
                        borderColor = if (isDeviceOwner) AegisSuccessGreen.copy(alpha = 0.4f) else AegisElectricBlue.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "App Suspension & Intervention Mode",
                            color = AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isDeviceOwner) "Strict Device Owner Mode" else "Standard Mode (Accessibility & Overlay)",
                            color = if (isDeviceOwner) AegisSuccessGreen else AegisPrimaryCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isDeviceOwner) {
                                "Device Owner privileges verified. Offending applications will be suspended at the OS kernel level via setPackagesSuspended() in addition to instant screen shielding."
                            } else {
                                "Running in Standard Mode. Uses Accessibility eviction (Home + Background kill) and WindowManager Overlay. Provision Device Owner via ADB for kernel-level package suspension and Safe Boot lock."
                            },
                            color = AegisTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = ClassicalBodyFont
                        )

                        if (!isDeviceAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    preferences.grantTemporarySettingsAuthorization()
                                    context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                                    isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("ACTIVATE DEVICE ADMINISTRATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                            }
                        }

                        if (!isDeviceOwner) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "To provision Device Owner (Parental / Enterprise):\nadb shell dpm set-device-owner com.ahsan.tcr.sheildlenz/com.example.security.deviceowner.AegisDeviceAdminReceiver",
                                color = AegisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AegisSurfaceElevated)
                                    .border(1.dp, AegisCardBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            )
                        }
                    }
                }

                // Permission Health Checklist
                item {
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
                                preferences.grantTemporarySettingsAuthorization()
                                context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                            }
                        )
                        PermissionHealthRow(
                            label = "Device Administrator Protection",
                            isHealthy = isDeviceAdmin,
                            onFix = {
                                preferences.grantTemporarySettingsAuthorization()
                                context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                            }
                        )
                        PermissionHealthRow(
                            label = "Battery Optimization Exemption",
                            isHealthy = hasBattery,
                            onFix = {
                                preferences.grantTemporarySettingsAuthorization()
                                context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                            }
                        )
                    }
                }

                // Emergency Maintenance Mode
                item {
                    CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.30f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Emergency Maintenance Mode",
                                    color = AegisDangerRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Temporarily suppresses all visual blocking for maintenance or troubleshooting.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = isMaintenanceMode,
                                onCheckedChange = { isChecked -> preferences.setMaintenanceMode(isChecked) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisDangerRed,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
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
                                text = "Offline Architecture Specification",
                                color = AegisPrimaryCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sheildlenz operates exclusively on-device without remote servers, telemetry brokers, or cloud classifiers. Frame processing is completely volatile and discarded upon classification.",
                            color = AegisTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }

                // 7-Tap Secret Advanced Precision Lab (revealed when App Version is tapped 7 times)
                item {
                    AdvancedPrecisionLabSection(preferences = preferences)
                }

                // App Version Card (Tap 7 times to unlock Advanced Precision Lab)
                item {
                    AppVersionSecretCard(preferences = preferences)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = AegisCardBackground,
                    titleContentColor = AegisTextPrimary,
                    title = {
                        Text(
                            text = if (hasPin) "Change Guardian PIN" else "Set Guardian PIN",
                            fontFamily = ClassicalDisplayFont,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Enter a 4 to 8 digit numerical passcode to safeguard Sheildlenz configuration and control unblocking.",
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) pinInput = it },
                                label = { Text("Guardian PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
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
                                    fontSize = 12.sp,
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
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "FIX", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
            }
        }
    }
}
