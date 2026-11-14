package com.example.ui.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.SensitivityLevel
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.PowerMode
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.MasterKeyGateState
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
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
    val powerMode by preferences.powerMode.collectAsState()
    val skipDuplicateFrames by preferences.skipDuplicateFrames.collectAsState()
    val captureWidthPx by preferences.captureWidthPx.collectAsState()
    val pauseWhenScreenOff by preferences.pauseWhenScreenOff.collectAsState()

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
                    LuxurySectionHeader(title = "Load / Change AI", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(8.dp))
                    ModelManagerCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        onModelUpdated = onModelUpdated,
                        cardTone = CyberCardTone.LAVENDER,
                        showCloudDownloader = true,
                        onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                        onRequireAuth = { label, action ->
                            masterKeyGate.request(label) { action() }
                        }
                    )
                }

                // 2. Power & Performance Mode Widget + Power-Saving Options
                item {
                    LuxurySectionHeader(title = "Power & Performance Mode", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(
                        borderColor = AegisCardBorder,
                        tone = CyberCardTone.SKY,
                        modifier = Modifier.testTag("power_mode_widget")
                    ) {
                        Text(
                            text = "Shield Mode",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = powerMode.caption,
                            color = AegisPrimaryCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PowerModeTile(
                                mode = PowerMode.POWER_SAVING,
                                icon = Icons.Default.BatterySaver,
                                accentColor = AegisPrimaryCyan,
                                selected = powerMode == PowerMode.POWER_SAVING,
                                onClick = { preferences.setPowerMode(PowerMode.POWER_SAVING) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_power_saving")
                            )
                            PowerModeTile(
                                mode = PowerMode.BALANCED,
                                icon = Icons.Default.Tune,
                                accentColor = AegisPurple,
                                selected = powerMode == PowerMode.BALANCED,
                                onClick = { preferences.setPowerMode(PowerMode.BALANCED) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_balanced")
                            )
                            PowerModeTile(
                                mode = PowerMode.EXTREME,
                                icon = Icons.Default.Bolt,
                                accentColor = AegisChampagneGold,
                                selected = powerMode == PowerMode.EXTREME,
                                onClick = { preferences.setPowerMode(PowerMode.EXTREME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_extreme")
                            )
                        }

                        HorizontalDivider(
                            color = AegisCardBorder.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Text(
                            text = "Power-Saving Options",
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PowerOptionSwitchRow(
                            title = "Skip Static Frames",
                            subtitle = "Save battery on unchanged screens",
                            checked = skipDuplicateFrames,
                            onCheckedChange = { preferences.setSkipDuplicateFrames(it) },
                            testTag = "power_opt_skip_duplicates"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PowerOptionSwitchRow(
                            title = "Compact Capture (320px)",
                            subtitle = "Lower resolution saves GPU & RAM",
                            checked = captureWidthPx <= 320,
                            onCheckedChange = { preferences.setCompactPowerResolution(it) },
                            testTag = "power_opt_compact_capture"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PowerOptionSwitchRow(
                            title = "Pause While Screen Off",
                            subtitle = "Sleep frame scanner when locked",
                            checked = pauseWhenScreenOff,
                            onCheckedChange = { preferences.setPauseWhenScreenOff(it) },
                            testTag = "power_opt_pause_screen_off"
                        )
                    }
                }

                // App Whitelisting & Exclusion Rules
                item {
                    WhitelistedAppsSection(preferences = preferences)
                }

                // Scheduled Active Protection Hours ("Bedtime / Study Guard")
                item {
                    ScheduledProtectionSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate
                    )
                }

                // 2. PIN Change of Guardian
                item {
                    LuxurySectionHeader(title = "Guardian PIN", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(
                        borderColor = AegisGoldBorder,
                        tone = CyberCardTone.GOLD
                    ) {
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
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(AegisChampagneGold, AegisGoldBright)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisOnPrimaryWhite, modifier = Modifier.size(19.dp))
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
                                        color = if (hasPin) AegisPrimaryCyan else AegisWarningAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
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
                                    containerColor = if (hasPin) AegisPurple else AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("guardian_pin_change_button")
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
                    LuxurySectionHeader(title = "Thresholds", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = AegisCardBorder, tone = CyberCardTone.SKY) {
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
                                val levelAccent = when (level) {
                                    SensitivityLevel.PERMISSIVE -> AegisPrimaryCyan
                                    SensitivityLevel.BALANCED -> AegisPurple
                                    SensitivityLevel.STRICT -> AegisChampagneGold
                                }
                                val selectedFill = when (level) {
                                    SensitivityLevel.PERMISSIVE -> AegisSurfaceElevated
                                    SensitivityLevel.BALANCED -> AegisLavenderSoft
                                    SensitivityLevel.STRICT -> AegisChampagneSoft
                                }
                                val shortCaption = when (level) {
                                    SensitivityLevel.PERMISSIVE -> "Blocks explicit content only"
                                    SensitivityLevel.BALANCED -> "Explicit + high-confidence suggestive"
                                    SensitivityLevel.STRICT -> "Strictest block threshold"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) selectedFill else AegisCardBackground.copy(alpha = 0.72f))
                                        .border(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) levelAccent else AegisCardBorder,
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
                    LuxurySectionHeader(title = "Resistance", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = AegisGoldBorder, tone = CyberCardTone.GOLD) {
                        Text(
                            text = "Lockdown Penalty: $stage1DurationLabel",
                            color = AegisWarningAmber,
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
                                        containerColor = if (selected) AegisChampagneGold else AegisCardBackground,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
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
                val confirmFocusRequester = remember { FocusRequester() }
                val saveNewPin: () -> Unit = {
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
                }
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = AegisLavenderSoft,
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { confirmFocusRequester.requestFocus() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPurple,
                                    unfocusedBorderColor = AegisLavenderBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                confirmFocusRequester.requestFocus()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { saveNewPin() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPurple,
                                    unfocusedBorderColor = AegisLavenderBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(confirmFocusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                saveNewPin()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("change_pin_confirm_input")
                            )
                            if (pinError != null) {
                                Text(
                                    text = pinError!!,
                                    color = AegisWarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = saveNewPin,
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                            modifier = Modifier.testTag("change_pin_save_button")
                        ) {
                            Text(text = "SAVE PIN", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showPinDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisChampagneSoft,
                                contentColor = AegisTextPrimary
                            )
                        ) {
                            Text(text = "CANCEL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PowerModeTile(
    mode: PowerMode,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerFill by animateColorAsState(
        targetValue = if (selected) accentColor else AegisCardBackground.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 280),
        label = "PowerModeFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else AegisCardBorder,
        animationSpec = tween(durationMillis = 280),
        label = "PowerModeBorder"
    )
    val contentTint = if (selected) AegisOnPrimaryWhite else accentColor
    val titleColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
    val subColor = if (selected) AegisOnPrimaryWhite.copy(alpha = 0.90f) else AegisTextMuted

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerFill)
            .border(if (selected) 1.6.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.displayName,
            tint = contentTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mode.displayName,
            color = titleColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${mode.scanIntervalMs}ms",
            color = subColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ClassicalBodyFont,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PowerOptionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
            )
            Text(
                text = subtitle,
                color = AegisTextMuted,
                fontSize = 11.sp,
                fontFamily = ClassicalBodyFont
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisOnPrimaryWhite,
                checkedTrackColor = AegisPrimaryCyan,
                uncheckedThumbColor = AegisTextMuted,
                uncheckedTrackColor = AegisCardBorder
            )
        )
    }
}

@Composable
fun WhitelistedAppsSection(preferences: AegisPreferences) {
    val whitelisted by preferences.whitelistedPackages.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var packageInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    LuxurySectionHeader(title = "App Whitelisting & Exclusion Rules", accentColor = AegisPrimaryCyan)
    Spacer(modifier = Modifier.height(8.dp))
    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("whitelisted_apps_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Excluded Apps (${whitelisted.size})",
                    color = AegisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Bypass AI screen guard & blocking for trusted apps",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Button(
                onClick = {
                    packageInput = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_whitelist_button")
            ) {
                Text("ADD APP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (whitelisted.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                whitelisted.forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AegisSurfaceElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg,
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                preferences.removeWhitelistedPackage(pkg)
                                Toast.makeText(context, "Removed $pkg from whitelist", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Remove", fontSize = 10.sp, color = AegisWarningAmber)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AegisLavenderSoft,
            title = { Text("Whitelist App Package", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the exact package name to exclude from scanning (e.g. com.whatsapp, com.google.android.youtube):", fontSize = 12.sp, fontFamily = ClassicalBodyFont)
                    OutlinedTextField(
                        value = packageInput,
                        onValueChange = { packageInput = it },
                        label = { Text("Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Quick suggestions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AegisPurple)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("com.whatsapp", "com.google.android.youtube", "com.android.chrome", "com.google.android.gm").forEach { suggestion ->
                            OutlinedButton(
                                onClick = { packageInput = suggestion },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(suggestion.substringAfterLast('.'), fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (packageInput.isNotBlank()) {
                            preferences.addWhitelistedPackage(packageInput.trim())
                            Toast.makeText(context, "Whitelisted ${packageInput.trim()}", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan)
                ) {
                    Text("ADD TO WHITELIST", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun ScheduledProtectionSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState
) {
    val scheduleEnabled by preferences.scheduleEnabled.collectAsState()
    val startMinutes by preferences.scheduleStartMinutes.collectAsState()
    val endMinutes by preferences.scheduleEndMinutes.collectAsState()
    val isActiveNow = scheduleEnabled && preferences.isWithinScheduledWindow()

    LuxurySectionHeader(title = "Scheduled Protection Hours", accentColor = AegisPurple)
    Spacer(modifier = Modifier.height(8.dp))
    CyberCard(
        borderColor = if (isActiveNow) AegisPrimaryCyan.copy(alpha = 0.55f) else AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("scheduled_protection_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisPurple, AegisPrimaryCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Scheduled protection hours",
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bedtime & Study Guard",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "${AegisPreferences.formatClockTime(startMinutes)} → ${AegisPreferences.formatClockTime(endMinutes)}",
                        color = if (scheduleEnabled) AegisPrimaryCyan else AegisTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Switch(
                checked = scheduleEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        masterKeyGate.request("disable Scheduled Protection Hours") {
                            preferences.setScheduleEnabled(false)
                        }
                    } else {
                        preferences.setScheduleEnabled(true)
                    }
                },
                modifier = Modifier.testTag("schedule_enabled_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisPurple,
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
            Text(
                text = if (isActiveNow) "Active right now • Quiet hours enforcement" else if (scheduleEnabled) "Standby until next window" else "Reminder window disabled",
                color = AegisTextSecondary,
                fontSize = 11.sp,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = when {
                    isActiveNow -> "ACTIVE NOW"
                    scheduleEnabled -> "ARMED"
                    else -> "OFF"
                },
                isActive = isActiveNow,
                activeColor = AegisPrimaryCyan,
                inactiveColor = if (scheduleEnabled) AegisPurple else AegisTextMuted
            )
        }

        HorizontalDivider(
            color = AegisLavenderBorder.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "Quick Guard Presets",
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont
        )
        Spacer(modifier = Modifier.height(8.dp))

        val presets = listOf(
            Triple("Night 9P–7A", 21 * 60, 7 * 60),
            Triple("Sleep 10P–6A", 22 * 60, 6 * 60),
            Triple("Study 2P–6P", 14 * 60, 18 * 60)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (label, pStart, pEnd) ->
                val isSelected = startMinutes == pStart && endMinutes == pEnd
                OutlinedButton(
                    onClick = {
                        preferences.setScheduleWindow(pStart, pEnd)
                        if (!scheduleEnabled) preferences.setScheduleEnabled(true)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) AegisPurple else AegisCardBackground,
                        contentColor = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScheduleHourStepper(
                label = "Start Time",
                minuteOfDay = startMinutes,
                onMinusHour = {
                    val next = (startMinutes - 60 + 1440) % 1440
                    preferences.setScheduleWindow(next, endMinutes)
                },
                onPlusHour = {
                    val next = (startMinutes + 60) % 1440
                    preferences.setScheduleWindow(next, endMinutes)
                },
                modifier = Modifier.weight(1f)
            )
            ScheduleHourStepper(
                label = "End Time",
                minuteOfDay = endMinutes,
                onMinusHour = {
                    val next = (endMinutes - 60 + 1440) % 1440
                    preferences.setScheduleWindow(startMinutes, next)
                },
                onPlusHour = {
                    val next = (endMinutes + 60) % 1440
                    preferences.setScheduleWindow(startMinutes, next)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScheduleHourStepper(
    label: String,
    minuteOfDay: Int,
    onMinusHour: () -> Unit,
    onPlusHour: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = AegisTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = AegisPreferences.formatClockTime(minuteOfDay),
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onMinusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPlusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

