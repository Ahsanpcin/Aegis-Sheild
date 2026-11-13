package com.example.ui.protection

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.SecurityIncident
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ModelManager
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.MetricTile
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SampleImageTesterCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.SheildlenzScaffoldBrandBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
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
import kotlinx.coroutines.launch

@Composable
fun LiveProtectionScreen(
    protectionRepo: ProtectionRepository,
    historyRepo: SecurityHistoryRepository,
    modelManager: ModelManager,
    preferences: AegisPreferences,
    onNavigateBack: () -> Unit,
    onTriggerBlockOverlay: () -> Unit,
    onModelUpdated: () -> Unit = {}
) {
    val telemetry by protectionRepo.telemetry.collectAsState()
    val activeAppName by protectionRepo.activeAppLabel.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val isStrictEverything by preferences.strictEverything.collectAsState()
    val scope = rememberCoroutineScope()

    SheildlenzBackdrop(modifier = Modifier.testTag("live_protection_screen_root")) {
        Scaffold(
            topBar = {
                CyberTopBar(
                    title = "Live Telemetry",
                    subtitle = "Real-Time Optical HUD",
                    onNavigateBack = onNavigateBack
                )
            },
            bottomBar = { SheildlenzScaffoldBrandBar() },
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

                item {
                    CyberCard(
                        borderColor = if (telemetry.isProtectionEnabled) AegisSuccessGreen.copy(alpha = 0.35f) else AegisCardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MONITORING SUBSYSTEM",
                                    color = AegisTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont,
                                    letterSpacing = 0.9.sp
                                )
                                val subtitleState = when {
                                    telemetry.isProtectionEnabled && telemetry.isModelLoaded -> "Armed & Scanning"
                                    telemetry.isProtectionEnabled -> "Capture Active (No AI Model)"
                                    else -> "Standby (Disengaged)"
                                }
                                val subtitleColor = when {
                                    telemetry.isProtectionEnabled && telemetry.isModelLoaded -> AegisSuccessGreen
                                    telemetry.isProtectionEnabled -> AegisWarningAmber
                                    else -> AegisTextMuted
                                }
                                Text(
                                    text = subtitleState,
                                    color = subtitleColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            StatusPill(
                                label = if (telemetry.isProtectionEnabled) "ENGAGED" else "OFFLINE",
                                isActive = telemetry.isProtectionEnabled,
                                activeColor = AegisSuccessGreen,
                                inactiveColor = AegisTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = telemetry.statusMessage,
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "Inspection Rate",
                            value = if (telemetry.isProtectionEnabled) "%.1f FPS".format(telemetry.currentInspectionFps) else "0.0 FPS",
                            icon = Icons.Default.Speed,
                            accentColor = AegisPrimaryCyan,
                            caption = "Subsampled rate",
                            modifier = Modifier.weight(1f)
                        )

                        MetricTile(
                            title = "Frames Scanned",
                            value = telemetry.framesProcessedCount.toString(),
                            icon = Icons.Default.Camera,
                            accentColor = AegisElectricBlue,
                            caption = "Volatile RAM cycle",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    LuxurySectionHeader(title = "TFLite Model & Verification")
                }

                item {
                    ModelManagerCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        onModelUpdated = onModelUpdated
                    )
                }

                item {
                    SampleImageTesterCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        sensitivity = sensitivity,
                        isStrictEverything = isStrictEverything
                    )
                }

                item {
                    CyberCard(borderColor = AegisPurple.copy(alpha = 0.28f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                item {
                    CyberCard(borderColor = AegisCardBorder) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Strict Zero-Image Retention",
                                    color = AegisPrimaryCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Text(
                                    text = "In compliance with Sheildlenz privacy architecture, screen frames are never converted to persisted JPEG/PNG files. Only numerical confidence scores and security incident metadata are recorded in the local log.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(top = 4.dp),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                item {
                    CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.3f)) {
                        Text(
                            text = "Security Testing & Simulation",
                            color = AegisDangerRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Simulate an explicit content detection event to verify the local database audit log and preview the emergency blocking interface.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = ClassicalBodyFont
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val simulatedIncident = SecurityIncident(
                                        timestampMillis = System.currentTimeMillis(),
                                        packageName = "com.android.chrome",
                                        appDisplayName = "Chrome Browser",
                                        category = ThreatCategory.EXPLICIT_ADULT,
                                        severity = ThreatSeverity.CRITICAL,
                                        confidenceScore = 0.96f,
                                        actionTaken = "Simulation Preview • System Shield Cast",
                                        diagnosticSummary = "Simulated Explicit Detection (96% confidence) • Preview mode does not engage 10h device lock."
                                    )
                                    historyRepo.logIncident(simulatedIncident)
                                    protectionRepo.recordDetection(ThreatCategory.EXPLICIT_ADULT)
                                    onTriggerBlockOverlay()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisDangerRed.copy(alpha = 0.10f),
                                contentColor = AegisDangerRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(1.dp, AegisDangerRed.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .testTag("simulate_threat_button")
                        ) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "SIMULATE THREAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont, letterSpacing = 0.6.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
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
