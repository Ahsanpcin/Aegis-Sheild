package com.example.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.BuildConfig
import com.example.data.preferences.AegisPreferences
import com.example.ui.components.CyberCard
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.rememberMasterKeyGate
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppVersionSecretCard(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val isUnlocked by preferences.advancedOptionsUnlocked.collectAsState()
    var versionTapCount by remember { mutableIntStateOf(0) }

    val remainingTaps = (7 - versionTapCount).coerceAtLeast(0)

    CyberCard(
        borderColor = if (isUnlocked) AegisChampagneGold.copy(alpha = 0.65f) else AegisCardBorder,
        modifier = Modifier
            .clickable {
                if (isUnlocked) {
                    Toast.makeText(
                        context,
                        "Advanced Precision Lab is open below (and in Edits).",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    versionTapCount++
                    if (versionTapCount >= 7) {
                        versionTapCount = 0
                        preferences.setAdvancedOptionsUnlocked(true)
                        Toast.makeText(
                            context,
                            "Advanced Precision Lab unlocked!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (versionTapCount >= 3) {
                        val left = 7 - versionTapCount
                        Toast.makeText(
                            context,
                            "Tap $left more time${if (left == 1) "" else "s"} to unlock Advanced Precision Lab",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .testTag("app_version_card")
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
                            if (isUnlocked) AegisChampagneGold.copy(alpha = 0.16f)
                            else AegisPrimaryCyan.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Tune else Icons.Default.Verified,
                        contentDescription = "App Version",
                        tint = if (isUnlocked) AegisChampagneGold else AegisPrimaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = BuildConfig.APPLICATION_ID,
                        color = AegisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when {
                            isUnlocked -> "Precision Lab unlocked"
                            versionTapCount in 1..6 -> "$remainingTaps more tap${if (remainingTaps == 1) "" else "s"} for Precision Lab"
                            else -> "Tap 7 times for Precision Lab"
                        },
                        color = when {
                            isUnlocked -> AegisChampagneGold
                            versionTapCount >= 3 -> AegisPrimaryCyan
                            else -> AegisTextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isUnlocked || versionTapCount >= 3) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = ClassicalBodyFont,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedPrecisionLabSection(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val isUnlocked by preferences.advancedOptionsUnlocked.collectAsState()
    val lockdownDurationMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val doubleTimeMultiplier by preferences.doubleTimeMultiplier.collectAsState()
    val scanIntervalMs by preferences.scanIntervalMs.collectAsState()
    val captureWidthPx by preferences.captureWidthPx.collectAsState()
    val skipDuplicateFrames by preferences.skipDuplicateFrames.collectAsState()
    val customThresholdsEnabled by preferences.customThresholdsEnabled.collectAsState()
    val customExplicitThreshold by preferences.customExplicitThreshold.collectAsState()
    val customSuggestiveThreshold by preferences.customSuggestiveThreshold.collectAsState()
    val consecutiveFramesRequired by preferences.consecutiveFramesRequired.collectAsState()
    val showEmergencyDialer by preferences.showEmergencyDialerOnLockdown.collectAsState()
    val evictAppOnThreat by preferences.evictAppOnThreat.collectAsState()
    val guardianWindowMinutes by preferences.guardianSettingsWindowMinutes.collectAsState()

    val masterKeyGate = rememberMasterKeyGate(preferences)

    AnimatedVisibility(
        visible = isUnlocked,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("advanced_precision_lab_section"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LuxurySectionHeader(title = "Advanced Precision Lab", accentColor = AegisChampagneGold)

            // 1. Exact Lockdown Duration Specificity (Hours + Minutes + Multiplier)
            CyberCard(borderColor = AegisChampagneGold.copy(alpha = 0.50f)) {
                val stage1Formatted = AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)
                val doubleFormatted = AegisPreferences.formatMinutesLabel(
                    (lockdownDurationMinutes * doubleTimeMultiplier).toInt().coerceAtLeast(1)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AegisChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Precision Lockdown Timing",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisSurfaceElevated)
                        .border(1.dp, AegisChampagneGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STAGE 1 WAIT",
                            color = AegisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                        Text(
                            text = stage1Formatted,
                            color = AegisDangerRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DOUBLE-TIME (${formatMultiplier(doubleTimeMultiplier)})",
                            color = AegisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                        Text(
                            text = doubleFormatted,
                            color = AegisWarningAmber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Exact Presets",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(6.dp))

                var draftHours by remember(lockdownDurationMinutes) { mutableIntStateOf(lockdownDurationMinutes / 60) }
                var draftMins by remember(lockdownDurationMinutes) { mutableIntStateOf(lockdownDurationMinutes % 60) }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        1 to "1m",
                        5 to "5m",
                        15 to "15m",
                        30 to "30m",
                        60 to "1h",
                        360 to "6h",
                        600 to "10h",
                        1440 to "24h"
                    ).forEach { (mins, label) ->
                        val active = lockdownDurationMinutes == mins
                        LabChip(
                            label = label,
                            selected = active,
                            onClick = {
                                if (!active) {
                                    masterKeyGate.request("set lockdown duration to $label") {
                                        preferences.setLockdownDurationMinutes(mins)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("lab_duration_preset_$label")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Fine Hours: ${draftHours}h",
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
                Slider(
                    value = draftHours.toFloat(),
                    onValueChange = { newHrs ->
                        val h = newHrs.roundToInt().coerceIn(0, 24)
                        draftHours = h
                        if (h == 24) draftMins = 0
                    },
                    onValueChangeFinished = {
                        val target = (draftHours * 60 + draftMins).coerceAtLeast(1)
                        if (target != lockdownDurationMinutes) {
                            masterKeyGate.request(
                                actionLabel = "set lockdown timer to ${AegisPreferences.formatMinutesLabel(target)}",
                                onCancel = {
                                    draftHours = lockdownDurationMinutes / 60
                                    draftMins = lockdownDurationMinutes % 60
                                }
                            ) {
                                preferences.setLockdownDurationMinutes(target)
                            }
                        }
                    },
                    valueRange = 0f..24f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = AegisPrimaryCyan,
                        activeTrackColor = AegisPrimaryCyan
                    )
                )

                Text(
                    text = "Fine Minutes: ${draftMins}m",
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
                Slider(
                    value = draftMins.toFloat(),
                    onValueChange = { newMins ->
                        val m = newMins.roundToInt().coerceIn(0, 59)
                        draftMins = m
                        if (draftHours >= 24) draftHours = 23
                    },
                    onValueChangeFinished = {
                        val target = (draftHours * 60 + draftMins).coerceAtLeast(1)
                        if (target != lockdownDurationMinutes) {
                            masterKeyGate.request(
                                actionLabel = "set lockdown timer to ${AegisPreferences.formatMinutesLabel(target)}",
                                onCancel = {
                                    draftHours = lockdownDurationMinutes / 60
                                    draftMins = lockdownDurationMinutes % 60
                                }
                            ) {
                                preferences.setLockdownDurationMinutes(target)
                            }
                        }
                    },
                    valueRange = 0f..59f,
                    colors = SliderDefaults.colors(
                        thumbColor = AegisChampagneGold,
                        activeTrackColor = AegisChampagneGold
                    )
                )

                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Double-Time Fallback Multiplier",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.5f, 2.0f, 3.0f, 4.0f).forEach { mult ->
                        val selected = kotlin.math.abs(doubleTimeMultiplier - mult) < 0.05f
                        LabChip(
                            label = formatMultiplier(mult),
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    masterKeyGate.request("set double-time multiplier to ${formatMultiplier(mult)}") {
                                        preferences.setDoubleTimeMultiplier(mult)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. AI Threshold & Consecutive Frame Specificity
            CyberCard(borderColor = AegisPrimaryCyan.copy(alpha = 0.35f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Specificity & Frame Confirmation",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LabSwitchRow(
                    title = "Custom Confidence Override",
                    subtitle = "Set exact Explicit & Suggestive % cutoffs",
                    checked = customThresholdsEnabled,
                    onCheckedChange = { enabled ->
                        masterKeyGate.request("${if (enabled) "enable" else "disable"} custom confidence override") {
                            preferences.setCustomThresholdsEnabled(enabled)
                        }
                    }
                )

                if (customThresholdsEnabled) {
                    var draftExplicit by remember(customExplicitThreshold) { mutableFloatStateOf(customExplicitThreshold) }
                    var draftSuggestive by remember(customSuggestiveThreshold) { mutableFloatStateOf(customSuggestiveThreshold) }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explicit Block Threshold: ${(draftExplicit * 100).roundToInt()}%",
                        color = AegisDangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                    Slider(
                        value = draftExplicit,
                        onValueChange = { draftExplicit = it },
                        onValueChangeFinished = {
                            val target = draftExplicit
                            if (kotlin.math.abs(target - customExplicitThreshold) > 0.001f) {
                                masterKeyGate.request(
                                    actionLabel = "set Explicit threshold to ${(target * 100).roundToInt()}%",
                                    onCancel = { draftExplicit = customExplicitThreshold }
                                ) {
                                    preferences.setCustomExplicitThreshold(target)
                                }
                            }
                        },
                        valueRange = 0.35f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = AegisDangerRed,
                            activeTrackColor = AegisDangerRed
                        )
                    )

                    Text(
                        text = "Suggestive Block Threshold: ${(draftSuggestive * 100).roundToInt()}%",
                        color = AegisWarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                    Slider(
                        value = draftSuggestive,
                        onValueChange = { draftSuggestive = it },
                        onValueChangeFinished = {
                            val target = draftSuggestive
                            if (kotlin.math.abs(target - customSuggestiveThreshold) > 0.001f) {
                                masterKeyGate.request(
                                    actionLabel = "set Suggestive threshold to ${(target * 100).roundToInt()}%",
                                    onCancel = { draftSuggestive = customSuggestiveThreshold }
                                ) {
                                    preferences.setCustomSuggestiveThreshold(target)
                                }
                            }
                        },
                        valueRange = 0.35f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = AegisWarningAmber,
                            activeTrackColor = AegisWarningAmber
                        )
                    )
                }

                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = "Consecutive Suggestive Frames: $consecutiveFramesRequired",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { count ->
                        val selected = consecutiveFramesRequired == count
                        LabChip(
                            label = "${count}F",
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    masterKeyGate.request("set consecutive frames to $count") {
                                        preferences.setConsecutiveFramesRequired(count)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 10.dp))

                LabSwitchRow(
                    title = "Skip Duplicate Static Frames",
                    subtitle = "Save battery on unchanged screens",
                    checked = skipDuplicateFrames,
                    onCheckedChange = { enabled ->
                        masterKeyGate.request("${if (enabled) "enable" else "disable"} duplicate frame skipping") {
                            preferences.setSkipDuplicateFrames(enabled)
                        }
                    }
                )
            }

            // 3. Scan Speed & VirtualDisplay Resolution
            CyberCard(borderColor = AegisCardBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Cadence & Capture Resolution",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                var draftScanInterval by remember(scanIntervalMs) { mutableIntStateOf(scanIntervalMs) }
                val fpsEstimate = 1000f / draftScanInterval.coerceAtLeast(100)
                Text(
                    text = String.format(Locale.US, "Frame Inspection Interval: %d ms (%.1f FPS)", draftScanInterval, fpsEstimate),
                    color = AegisPrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Slider(
                    value = draftScanInterval.toFloat(),
                    onValueChange = { ms ->
                        draftScanInterval = ((ms / 50f).roundToInt() * 50).coerceIn(100, 2000)
                    },
                    onValueChangeFinished = {
                        val snapped = draftScanInterval
                        if (snapped != scanIntervalMs) {
                            masterKeyGate.request(
                                actionLabel = "set scan interval to ${snapped}ms",
                                onCancel = { draftScanInterval = scanIntervalMs }
                            ) {
                                preferences.setScanIntervalMs(snapped)
                            }
                        }
                    },
                    valueRange = 100f..2000f,
                    colors = SliderDefaults.colors(
                        thumbColor = AegisPrimaryCyan,
                        activeTrackColor = AegisPrimaryCyan
                    )
                )

                Text(
                    text = "VirtualDisplay Width (applies on next Shield start): ${captureWidthPx}px",
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(320, 480, 640, 720).forEach { w ->
                        val selected = captureWidthPx == w
                        LabChip(
                            label = "${w}px",
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    masterKeyGate.request("set capture width to ${w}px") {
                                        preferences.setCaptureWidthPx(w)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Lockdown Behavior & Guardian Bypass Window
            CyberCard(borderColor = AegisCardBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardian & Lockdown Hardening",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "System Settings Authorization Window: $guardianWindowMinutes min",
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5, 10).forEach { mins ->
                        val selected = guardianWindowMinutes == mins
                        LabChip(
                            label = "${mins}m",
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    masterKeyGate.request("set authorization window to ${mins}m") {
                                        preferences.setGuardianSettingsWindowMinutes(mins)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 10.dp))

                LabSwitchRow(
                    title = "Evict & Kill Offending App",
                    subtitle = "Send Home & close offending app",
                    checked = evictAppOnThreat,
                    onCheckedChange = { enabled ->
                        masterKeyGate.request("${if (enabled) "enable" else "disable"} app eviction") {
                            preferences.setEvictAppOnThreat(enabled)
                        }
                    }
                )

                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 10.dp))

                LabSwitchRow(
                    title = "Emergency Dialer on Lockscreen",
                    subtitle = "Show Emergency button during lockdown",
                    checked = showEmergencyDialer,
                    onCheckedChange = { enabled ->
                        masterKeyGate.request("${if (enabled) "show" else "hide"} Emergency dialer") {
                            preferences.setShowEmergencyDialerOnLockdown(enabled)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            masterKeyGate.request("reset advanced settings") {
                                preferences.resetAdvancedCustomizations()
                                Toast.makeText(context, "Advanced customizations reset to defaults.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisWarningAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("advanced_lab_reset_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            masterKeyGate.request("hide Precision Lab") {
                                preferences.setAdvancedOptionsUnlocked(false)
                                Toast.makeText(context, "Advanced Precision Lab hidden.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("advanced_lab_hide_button")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HIDE LAB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AegisPrimaryCyan else AegisSurfaceElevated)
            .border(
                1.dp,
                if (selected) AegisPrimaryCyan else AegisCardBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AegisOnPrimaryWhite else AegisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont
        )
    }
}

@Composable
private fun LabSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                lineHeight = 16.sp,
                fontFamily = ClassicalBodyFont
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisOnPrimaryWhite,
                checkedTrackColor = AegisPrimaryCyan,
                uncheckedThumbColor = AegisTextMuted,
                uncheckedTrackColor = AegisCardBorder
            )
        )
    }
}

private fun formatMultiplier(mult: Float): String {
    return if (mult % 1.0f == 0f) "${mult.toInt()}×" else String.format(Locale.US, "%.1f×", mult)
}
