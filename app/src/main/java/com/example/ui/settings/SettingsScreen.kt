package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.SensitivityLevel
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisDangerRed
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
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }
    val masterKeyGate = rememberMasterKeyGate(preferences)

    SheildlenzBackdrop(modifier = Modifier.testTag("settings_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Edits",
                    subtitle = "Model, Guardian & Thresholds"
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
                // 1. Load / Change AI Model Widget (Top)
                item {
                    LuxurySectionHeader(title = "Load / Change AI")
                    Spacer(modifier = Modifier.height(8.dp))
                    ModelManagerCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        onModelUpdated = onModelUpdated,
                        onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                        onRequireAuth = { label, action ->
                            masterKeyGate.request(label) { action() }
                        }
                    )
                }

                // 2. PIN Change of Guardian
                item {
                    LuxurySectionHeader(title = "Guardian PIN")
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = if (hasPin) AegisChampagneGold.copy(alpha = 0.45f) else AegisWarningAmber.copy(alpha = 0.45f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AegisPrimaryCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Guardian PIN",
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (hasPin) "Active • Locks app & admin" else "Not configured",
                                        color = if (hasPin) AegisSuccessGreen else AegisWarningAmber,
                                        fontSize = 12.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    masterKeyGate.request(if (hasPin) "change Guardian PIN" else "set Guardian PIN") {
                                        pinInput = ""
                                        confirmPinInput = ""
                                        pinError = null
                                        showPinDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasPin) AegisSurfaceElevated else AegisPrimaryCyan,
                                    contentColor = if (hasPin) AegisPrimaryCyan else AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .testTag("guardian_pin_change_button")
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

                // 3. Thresholds
                item {
                    LuxurySectionHeader(title = "Thresholds")
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = AegisCardBorder) {
                        Text(
                            text = "Detection Sensitivity",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SensitivityLevel.entries.forEach { level ->
                                val isSelected = (sensitivity == level)
                                val shortCaption = when (level) {
                                    SensitivityLevel.PERMISSIVE -> "Blocks explicit content only"
                                    SensitivityLevel.BALANCED -> "Explicit + high-confidence suggestive"
                                    SensitivityLevel.STRICT -> "Strictest block threshold"
                                }
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
                                        .clickable {
                                            masterKeyGate.request("change sensitivity to ${level.displayName}") {
                                                preferences.setSensitivity(level)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            masterKeyGate.request("change sensitivity to ${level.displayName}") {
                                                preferences.setSensitivity(level)
                                            }
                                        },
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
                                            text = shortCaption,
                                            color = AegisTextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 10.dp))

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
                                    text = "Block immediately on first match",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = isStrictEverything,
                                onCheckedChange = { enabled ->
                                    masterKeyGate.request("${if (enabled) "enable" else "disable"} Strict Everything") {
                                        preferences.setStrictEverything(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = AegisCardBorder
                                )
                            )
                        }
                    }
                }

                // 4. Resistance
                item {
                    LuxurySectionHeader(title = "Resistance", accentColor = AegisDangerRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.30f)) {
                        Text(
                            text = "Lockdown Penalty: $stage1DurationLabel",
                            color = AegisDangerRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 4, 10, 24).forEach { hrs ->
                                val selected = (lockdownDurationMinutes == hrs * 60)
                                OutlinedButton(
                                    onClick = {
                                        masterKeyGate.request("set lockdown to ${hrs}H") {
                                            preferences.setLockdownHours(hrs)
                                        }
                                    },
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

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))

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
                                    text = "$stage1DurationLabel wait • $doubleDurationLabel double-time",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = waitCooldownBeforePin,
                                onCheckedChange = { enabled ->
                                    masterKeyGate.request("change cooldown policy") {
                                        preferences.setWaitCooldownBeforePassword(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = AegisCardBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                    text = "Lock Admin, Accessibility & Time",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = antiTamperEnabled,
                                onCheckedChange = { enabled ->
                                    masterKeyGate.request("change anti-tamper policy") {
                                        preferences.setAntiTamperEnabled(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = AegisCardBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Auto-Start on Boot",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Arm Shield after device reboot",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = isAutoStart,
                                onCheckedChange = { enabled ->
                                    masterKeyGate.request("change boot auto-start") {
                                        preferences.setAutoStart(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = AegisCardBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Maintenance Mode",
                                    color = AegisDangerRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = "Temporarily pause visual blocking",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Switch(
                                checked = isMaintenanceMode,
                                onCheckedChange = { isChecked ->
                                    masterKeyGate.request("${if (isChecked) "enable" else "disable"} Maintenance Mode") {
                                        preferences.setMaintenanceMode(isChecked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisDangerRed,
                                    uncheckedThumbColor = AegisTextMuted,
                                    uncheckedTrackColor = AegisCardBorder
                                )
                            )
                        }

                        if (!isDeviceAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    masterKeyGate.request("activate Device Admin") {
                                        preferences.grantTemporarySettingsAuthorization()
                                        context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                                        isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("ACTIVATE DEVICE ADMIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                            }
                        }
                    }
                }

                // 5. 7-Tap Secret Advanced Precision Lab (when unlocked)
                item {
                    AdvancedPrecisionLabSection(preferences = preferences)
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
                                text = "4 to 8 digit passcode for console & unlock control.",
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        pinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text("New Guardian PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_pin_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = confirmPinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        confirmPinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text("Confirm New PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_pin_confirm_input")
                            )
                            if (pinError != null) {
                                Text(
                                    text = pinError!!,
                                    color = AegisDangerRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                when {
                                    pinInput.length < 4 -> {
                                        pinError = "PIN must be at least 4 digits"
                                    }
                                    pinInput != confirmPinInput -> {
                                        pinError = "PINs do not match"
                                        confirmPinInput = ""
                                    }
                                    else -> {
                                        preferences.setGuardianPin(pinInput)
                                        showPinDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                            modifier = Modifier.testTag("change_pin_save_button")
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
