package com.example.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledAppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

private fun drawableToScaledBitmap(drawable: Drawable, sizePx: Int = 72): ImageBitmap? {
    return try {
        val raw = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val bmp = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        Bitmap.createScaledBitmap(raw, sizePx, sizePx, true).asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

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
    val waitCooldownBeforePin by preferences.waitCooldownBeforePassword.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()
    val powerMode by preferences.powerMode.collectAsState()
    val skipDuplicateFrames by preferences.skipDuplicateFrames.collectAsState()
    val captureWidthPx by preferences.captureWidthPx.collectAsState()
    val pauseWhenScreenOff by preferences.pauseWhenScreenOff.collectAsState()
    val adaptiveCadenceEnabled by preferences.adaptiveCadenceEnabled.collectAsState()
    val autoThrottleLowBattery by preferences.autoThrottleLowBattery.collectAsState()

    val stage1DurationLabel = AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)

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
                CyberTopBar(title = "Edits")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // 1. AI Model Engine
                item {
                    LuxurySectionHeader(title = "AI Model", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
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

                // 2. Compact Power Mode & Battery Controls
                item {
                    LuxurySectionHeader(title = "Power Mode", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = if (telemetry.isLowBatteryThrottled) AegisGoldBorder else AegisCardBorder,
                        tone = if (telemetry.isLowBatteryThrottled) CyberCardTone.GOLD else CyberCardTone.SKY,
                        modifier = Modifier.testTag("power_mode_widget")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = powerMode.displayName,
                                color = AegisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            if (telemetry.isLowBatteryThrottled) {
                                StatusPill(
                                    label = "THROTTLED",
                                    isActive = false,
                                    activeColor = AegisPrimaryCyan,
                                    inactiveColor = AegisChampagneGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PowerModeTile(
                                mode = PowerMode.ADAPTIVE_ECO,
                                icon = Icons.Default.AutoAwesome,
                                accentColor = AegisPrimaryCyan,
                                selected = powerMode == PowerMode.ADAPTIVE_ECO,
                                onClick = { preferences.setPowerMode(PowerMode.ADAPTIVE_ECO) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_adaptive_eco")
                            )
                            PowerModeTile(
                                mode = PowerMode.POWER_SAVING,
                                icon = Icons.Default.BatterySaver,
                                accentColor = AegisPurple,
                                selected = powerMode == PowerMode.POWER_SAVING,
                                onClick = { preferences.setPowerMode(PowerMode.POWER_SAVING) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_power_saving")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                            color = AegisCardBorder.copy(alpha = 0.55f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        CompactSwitchRow(
                            title = "Auto-Throttle (<20% Battery)",
                            checked = autoThrottleLowBattery,
                            onCheckedChange = { preferences.setAutoThrottleLowBattery(it) },
                            testTag = "power_opt_low_battery_throttle"
                        )
                        CompactSwitchRow(
                            title = "Adaptive Idle Backoff",
                            checked = adaptiveCadenceEnabled,
                            onCheckedChange = { preferences.setAdaptiveCadenceEnabled(it) },
                            testTag = "power_opt_adaptive_cadence"
                        )
                        CompactSwitchRow(
                            title = "Skip Static Frames",
                            checked = skipDuplicateFrames,
                            onCheckedChange = { preferences.setSkipDuplicateFrames(it) },
                            testTag = "power_opt_skip_duplicates"
                        )
                        CompactSwitchRow(
                            title = "Compact Capture (256px)",
                            checked = captureWidthPx <= 320,
                            onCheckedChange = { preferences.setCompactPowerResolution(it) },
                            testTag = "power_opt_compact_capture"
                        )
                        CompactSwitchRow(
                            title = "Pause on Screen Off",
                            checked = pauseWhenScreenOff,
                            onCheckedChange = { preferences.setPauseWhenScreenOff(it) },
                            testTag = "power_opt_pause_screen_off"
                        )
                    }
                }

                // 3. Excluded Apps (Tap to pick installed apps)
                item {
                    WhitelistedAppsSection(preferences = preferences)
                }

                // 4. Compact Scheduled Guard
                item {
                    ScheduledProtectionSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate
                    )
                }

                // 5. Compact Guardian PIN
                item {
                    LuxurySectionHeader(title = "Guardian PIN", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(6.dp))
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
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(AegisChampagneGold, AegisGoldBright)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisOnPrimaryWhite, modifier = Modifier.size(17.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Guardian PIN",
                                        color = AegisTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (hasPin) "Active" else "Not set",
                                        color = if (hasPin) AegisPrimaryCyan else AegisWarningAmber,
                                        fontSize = 11.sp,
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
                                modifier = Modifier
                                    .height(48.dp)
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

                // 6. Compact Thresholds
                item {
                    LuxurySectionHeader(title = "Thresholds", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(borderColor = AegisCardBorder, tone = CyberCardTone.SKY) {
                        Text(
                            text = "Sensitivity",
                            color = AegisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SensitivityLevel.entries.forEach { level ->
                                val isSelected = (sensitivity == level)
                                val shortTitle = when (level) {
                                    SensitivityLevel.STRICT -> "Strict"
                                    SensitivityLevel.BALANCED -> "Balanced"
                                    SensitivityLevel.PERMISSIVE -> "Permissive"
                                }
                                val levelAccent = when (level) {
                                    SensitivityLevel.PERMISSIVE -> AegisPrimaryCyan
                                    SensitivityLevel.BALANCED -> AegisPurple
                                    SensitivityLevel.STRICT -> AegisChampagneGold
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) levelAccent else AegisCardBackground.copy(alpha = 0.8f))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) levelAccent else AegisCardBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            masterKeyGate.request("change sensitivity to ${level.displayName}") {
                                                preferences.setSensitivity(level)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortTitle,
                                        color = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.55f), modifier = Modifier.padding(vertical = 8.dp))

                        CompactSwitchRow(
                            title = "Strict Everything Mode",
                            checked = isStrictEverything,
                            onCheckedChange = { enabled ->
                                masterKeyGate.request("${if (enabled) "enable" else "disable"} Strict Everything") {
                                    preferences.setStrictEverything(enabled)
                                }
                            },
                            testTag = "strict_everything_switch"
                        )
                    }
                }

                // 7. Compact Resistance
                item {
                    LuxurySectionHeader(title = "Resistance", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(borderColor = AegisGoldBorder, tone = CyberCardTone.GOLD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lockdown Penalty",
                                color = AegisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Text(
                                text = stage1DurationLabel,
                                color = AegisWarningAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 4, 10, 24).forEach { hrs ->
                                val selected = (lockdownDurationMinutes == hrs * 60)
                                OutlinedButton(
                                    onClick = {
                                        masterKeyGate.request("set lockdown to ${hrs}H") {
                                            preferences.setLockdownHours(hrs)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AegisChampagneGold else AegisCardBackground,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("${hrs}H", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                                }
                            }
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.55f), modifier = Modifier.padding(vertical = 8.dp))

                        CompactSwitchRow(
                            title = "Cooldown Before PIN",
                            checked = waitCooldownBeforePin,
                            onCheckedChange = {
                                masterKeyGate.request("change cooldown policy") {
                                    preferences.setWaitCooldownBeforePassword(it)
                                }
                            },
                            testTag = "cooldown_before_pin_switch"
                        )
                        CompactSwitchRow(
                            title = "Anti-Tamper Lock",
                            checked = antiTamperEnabled,
                            onCheckedChange = {
                                masterKeyGate.request("change anti-tamper policy") {
                                    preferences.setAntiTamperEnabled(it)
                                }
                            },
                            testTag = "anti_tamper_switch"
                        )
                        CompactSwitchRow(
                            title = "Auto-Start on Boot",
                            checked = isAutoStart,
                            onCheckedChange = {
                                masterKeyGate.request("change boot auto-start") {
                                    preferences.setAutoStart(it)
                                }
                            },
                            testTag = "auto_start_boot_switch"
                        )
                        CompactSwitchRow(
                            title = "Maintenance Mode",
                            checked = isMaintenanceMode,
                            titleColor = AegisDangerRed,
                            activeColor = AegisDangerRed,
                            onCheckedChange = { isChecked ->
                                masterKeyGate.request("${if (isChecked) "enable" else "disable"} Maintenance Mode") {
                                    preferences.setMaintenanceMode(isChecked)
                                }
                            },
                            testTag = "maintenance_mode_switch"
                        )

                        if (!isDeviceAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("ACTIVATE DEVICE ADMIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                            }
                        }
                    }
                }

                // 8. 7-Tap Secret Advanced Precision Lab (when unlocked)
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
        animationSpec = tween(durationMillis = 260),
        label = "PowerModeFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else AegisCardBorder,
        animationSpec = tween(durationMillis = 260),
        label = "PowerModeBorder"
    )
    val contentTint = if (selected) AegisOnPrimaryWhite else accentColor
    val titleColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
    val subColor = if (selected) AegisOnPrimaryWhite.copy(alpha = 0.88f) else AegisTextMuted

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerFill)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.displayName,
            tint = contentTint,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.displayName,
                color = titleColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mode.scanIntervalMs}ms",
                color = subColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    titleColor: Color = AegisTextPrimary,
    activeColor: Color = AegisPrimaryCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ClassicalBodyFont,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisOnPrimaryWhite,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = AegisTextMuted,
                uncheckedTrackColor = AegisCardBorder
            )
        )
    }
}

@Composable
fun WhitelistedAppsSection(preferences: AegisPreferences) {
    val whitelisted by preferences.whitelistedPackages.collectAsState()
    var showAppsPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Resolve installed launchable apps asynchronously when picker opens
    LaunchedEffect(showAppsPicker) {
        if (showAppsPicker && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = pm.queryIntentActivities(intent, 0)
                activities
                    .mapNotNull { resolveInfo ->
                        val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                        if (pkg == context.packageName) return@mapNotNull null
                        val label = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                        val icon = try {
                            drawableToScaledBitmap(resolveInfo.loadIcon(pm))
                        } catch (_: Exception) {
                            null
                        }
                        InstalledAppEntry(packageName = pkg, label = label, icon = icon)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            isLoadingApps = false
        }
    }

    LuxurySectionHeader(title = "Excluded Apps", accentColor = AegisPrimaryCyan)
    Spacer(modifier = Modifier.height(6.dp))
    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.SKY,
        onClick = {
            searchQuery = ""
            showAppsPicker = true
        },
        modifier = Modifier.testTag("whitelisted_apps_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Select apps to exclude",
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Excluded Apps (${whitelisted.size})",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "Tap to choose installed apps",
                        color = AegisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Button(
                onClick = {
                    searchQuery = ""
                    showAppsPicker = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_whitelist_button")
            ) {
                Text("SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (whitelisted.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                whitelisted.forEach { pkg ->
                    val resolvedLabel = remember(pkg, installedApps) {
                        installedApps.firstOrNull { it.packageName == pkg }?.label ?: try {
                            val ai = context.packageManager.getApplicationInfo(pkg, 0)
                            context.packageManager.getApplicationLabel(ai).toString()
                        } catch (_: Exception) {
                            pkg
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(AegisSurfaceElevated)
                            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(50))
                            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = resolvedLabel,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (resolvedLabel != pkg) {
                                Text(
                                    text = pkg,
                                    color = AegisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                preferences.removeWhitelistedPackage(pkg)
                                Toast.makeText(context, "Removed $resolvedLabel", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $resolvedLabel",
                                tint = AegisWarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAppsPicker) {
        val filteredApps = remember(installedApps, searchQuery, whitelisted) {
            val q = searchQuery.trim().lowercase()
            val list = if (q.isEmpty()) installedApps else installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
            list.sortedByDescending { whitelisted.contains(it.packageName) }
        }

        AlertDialog(
            onDismissRequest = { showAppsPicker = false },
            containerColor = AegisLavenderSoft,
            modifier = Modifier.testTag("installed_apps_picker_dialog"),
            title = {
                Text(
                    text = "Tap Apps to Exclude",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AegisPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPrimaryCyan,
                            unfocusedBorderColor = AegisLavenderBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("installed_apps_search_input")
                    )

                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AegisPurple)
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching installed apps found",
                                color = AegisTextMuted,
                                fontSize = 12.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val isExcluded = whitelisted.contains(app.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isExcluded) AegisPrimaryCyan.copy(alpha = 0.14f) else AegisCardBackground)
                                        .border(
                                            width = if (isExcluded) 1.4.dp else 1.dp,
                                            color = if (isExcluded) AegisPrimaryCyan else AegisLavenderBorder.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            if (isExcluded) {
                                                preferences.removeWhitelistedPackage(app.packageName)
                                            } else {
                                                preferences.addWhitelistedPackage(app.packageName)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(AegisPurple.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.label.take(1).uppercase(),
                                                    color = AegisPurple,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = ClassicalBodyFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.packageName,
                                                color = AegisTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = if (isExcluded) "Excluded" else "Not excluded",
                                        tint = if (isExcluded) AegisPrimaryCyan else AegisCardBorder,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppsPicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
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

    LuxurySectionHeader(title = "Scheduled Guard", accentColor = AegisPurple)
    Spacer(modifier = Modifier.height(6.dp))
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
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Quiet Hours",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "${AegisPreferences.formatClockTime(startMinutes)} → ${AegisPreferences.formatClockTime(endMinutes)}",
                        color = if (scheduleEnabled) AegisPrimaryCyan else AegisTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Switch(
                checked = scheduleEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        masterKeyGate.request("disable Scheduled Guard") {
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

        Spacer(modifier = Modifier.height(8.dp))

        val presets = listOf(
            Triple("9P–7A", 21 * 60, 7 * 60),
            Triple("10P–6A", 22 * 60, 6 * 60),
            Triple("2P–6P", 14 * 60, 18 * 60)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (label, pStart, pEnd) ->
                val isSelected = startMinutes == pStart && endMinutes == pEnd
                OutlinedButton(
                    onClick = {
                        preferences.setScheduleWindow(pStart, pEnd)
                        if (!scheduleEnabled) preferences.setScheduleEnabled(true)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScheduleHourStepper(
                label = "Start",
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
                label = "End",
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
            .clip(RoundedCornerShape(10.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label: ${AegisPreferences.formatClockTime(minuteOfDay)}",
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onMinusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPlusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
