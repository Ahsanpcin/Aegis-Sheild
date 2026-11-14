package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.EngineTelemetry
import com.example.data.preferences.AegisPreferences
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
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
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

data class SecurityHealthReport(
    val score: Int,
    val statusTitle: String,
    val statusDescription: String,
    val isShieldRunning: Boolean,
    val hasOverlay: Boolean,
    val hasAccessibility: Boolean,
    val isDeviceAdmin: Boolean,
    val hasGuardianPin: Boolean,
    val hasBatteryExemption: Boolean,
    val isModelLoaded: Boolean
) {
    companion object {
        fun compute(
            isShieldRunning: Boolean,
            hasOverlay: Boolean,
            hasAccessibility: Boolean,
            isDeviceAdmin: Boolean,
            hasGuardianPin: Boolean,
            hasBatteryExemption: Boolean,
            isModelLoaded: Boolean
        ): SecurityHealthReport {
            var total = 0
            if (isShieldRunning) total += 35
            if (hasAccessibility) total += 20
            if (isDeviceAdmin) total += 15
            if (hasGuardianPin) total += 15
            if (hasBatteryExemption) total += 10
            if (isModelLoaded) total += 5

            val (title, desc) = when {
                total >= 100 -> Pair(
                    "Immune • All Guards Armed",
                    "Every hardware and system defense is locked and actively protecting this device."
                )
                total >= 70 -> Pair(
                    "Guarded • Action Recommended",
                    "Protection is active, but enabling additional defense layers will improve resilience."
                )
                total >= 40 -> Pair(
                    "Partially Guarded",
                    "Core shield or key anti-tamper permissions are currently inactive."
                )
                else -> Pair(
                    "Vulnerable • Protection Off",
                    "Device is unguarded. Arm the shield to activate real-time screening."
                )
            }

            return SecurityHealthReport(
                score = total,
                statusTitle = title,
                statusDescription = desc,
                isShieldRunning = isShieldRunning,
                hasOverlay = hasOverlay,
                hasAccessibility = hasAccessibility,
                isDeviceAdmin = isDeviceAdmin,
                hasGuardianPin = hasGuardianPin,
                hasBatteryExemption = hasBatteryExemption,
                isModelLoaded = isModelLoaded
            )
        }
    }
}

@Composable
fun SecurityHealthGaugeCard(
    report: SecurityHealthReport,
    preferences: AegisPreferences,
    onRequestArmShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedScore by animateFloatAsState(
        targetValue = report.score.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "health_gauge_score"
    )

    CyberCard(
        borderColor = when {
            report.score >= 90 -> AegisLavenderBorder
            report.score >= 70 -> AegisCardBorder
            else -> AegisGoldBorder
        },
        tone = when {
            report.score >= 90 -> CyberCardTone.LAVENDER
            report.score >= 70 -> CyberCardTone.SKY
            else -> CyberCardTone.GOLD
        },
        modifier = modifier.testTag("security_health_gauge_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Arc Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp).padding(8.dp)
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val startAngle = 140f
                    val sweepAngle = 260f

                    // Background Track
                    drawArc(
                        color = AegisCardBorder.copy(alpha = 0.35f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Score Arc
                    val progressSweep = (animatedScore / 100f) * sweepAngle
                    if (progressSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    AegisPurple,
                                    AegisPrimaryCyan,
                                    AegisChampagneGold,
                                    AegisPurple
                                )
                            ),
                            startAngle = startAngle,
                            sweepAngle = progressSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedScore.toInt()}%",
                        color = AegisTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "HEALTH SCORE",
                        color = AegisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Score Status Banner
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            report.score >= 90 -> AegisPurple.copy(alpha = 0.12f)
                            report.score >= 70 -> AegisPrimaryCyan.copy(alpha = 0.12f)
                            else -> AegisChampagneGold.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            report.score >= 90 -> AegisPurple.copy(alpha = 0.45f)
                            report.score >= 70 -> AegisPrimaryCyan.copy(alpha = 0.45f)
                            else -> AegisChampagneGold.copy(alpha = 0.45f)
                        },
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = report.statusTitle,
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.statusDescription,
                color = AegisTextSecondary,
                fontSize = 11.sp,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6-Layer Security Health Matrix
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefenseLayerChip(
                        title = "Screen Shield",
                        active = report.isShieldRunning,
                        icon = Icons.Default.Shield,
                        onClick = onRequestArmShield,
                        modifier = Modifier.weight(1f)
                    )
                    DefenseLayerChip(
                        title = "Accessibility",
                        active = report.hasAccessibility,
                        icon = Icons.Default.AccessibilityNew,
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefenseLayerChip(
                        title = "Device Admin",
                        active = report.isDeviceAdmin,
                        icon = Icons.Default.Security,
                        onClick = {
                            if (!report.isDeviceAdmin) {
                                try {
                                    val intent = AegisDeviceAdminReceiver.createActivateAdminIntent(context).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DefenseLayerChip(
                        title = "Guardian PIN",
                        active = report.hasGuardianPin,
                        icon = Icons.Default.Lock,
                        onClick = {
                            // Handled in settings
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefenseLayerChip(
                        title = "Battery Exemption",
                        active = report.hasBatteryExemption,
                        icon = Icons.Default.BatterySaver,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DefenseLayerChip(
                        title = "AI Model",
                        active = report.isModelLoaded,
                        icon = Icons.Default.Memory,
                        onClick = {
                            // Handled in edits
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefenseLayerChip(
    title: String,
    active: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (active) AegisPrimaryCyan else AegisWarningAmber
    val bg = if (active) AegisSurfaceElevated else AegisChampagneSoft

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (active) AegisCardBorder else AegisGoldBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                maxLines = 1
            )
        }
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (active) "Active" else "Warning",
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun LiveScannerTelemetryCard(
    telemetry: EngineTelemetry,
    preferences: AegisPreferences,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val powerMode = preferences.powerMode.value
    val scanIntervalMs = preferences.scanIntervalMs.value
    val skipDuplicates = preferences.skipDuplicateFrames.value

    CyberCard(
        borderColor = AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = modifier.testTag("live_scanner_telemetry_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isMediaProjectionActive) AegisPrimaryCyan else AegisWarningAmber)
                        .padding(2.dp)
                ) {
                    if (telemetry.isMediaProjectionActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(AegisOnPrimaryWhite.copy(alpha = 0.8f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (telemetry.isMediaProjectionActive) "SCANNER LIVE" else "SCANNER STANDBY",
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AegisPrimaryCyan.copy(alpha = 0.12f))
                    .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${powerMode.displayName} • ${scanIntervalMs}ms",
                    color = AegisPrimaryCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetricBox(
                label = "INSPECTION FPS",
                value = String.format("%.1f", telemetry.currentInspectionFps),
                icon = Icons.Default.Speed,
                accentColor = AegisPrimaryCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricBox(
                label = "PROCESSED FRAMES",
                value = "${telemetry.framesProcessedCount}",
                icon = Icons.Default.Bolt,
                accentColor = AegisPurple,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricBox(
                label = "BATTERY SAVER",
                value = if (skipDuplicates) "ACTIVE" else "OFF",
                icon = Icons.Default.BatterySaver,
                accentColor = AegisChampagneGold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryMetricBox(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisCardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = AegisTextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
    }
}
