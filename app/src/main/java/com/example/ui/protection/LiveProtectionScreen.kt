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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.components.MetricTile
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SampleImageTesterCard
import com.example.ui.components.StatusPill
import com.example.ui.components.ThreatCategoryBadge
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.AegisWarningAmber
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

    Scaffold(
        topBar = {
            CyberTopBar(
                title = "Live Protection HUD",
                subtitle = "Real-Time Telemetry & Scanning",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = AegisVoidBackground,
        modifier = Modifier.testTag("live_protection_screen_root")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Current Service Status Hero
            item {
                CyberCard(
                    borderColor = if (telemetry.isProtectionEnabled) AegisSuccessGreen.copy(alpha = 0.4f) else AegisCardBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONITORING SUBSYSTEM",
                                color = AegisTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = telemetry.statusMessage,
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Real-Time Diagnostic Meters (FPS & Frames)
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
                        title = "Frames Evaluated",
                        value = telemetry.framesProcessedCount.toString(),
                        icon = Icons.Default.Camera,
                        accentColor = AegisElectricBlue,
                        caption = "Volatile memory cycle",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // In-App TFLite Model Manager & Sample Image Diagnostic Tester
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

            // AI Scanning Details
            item {
                CyberCard(borderColor = AegisPurple.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AegisPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = AegisPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI CLASSIFICATION PIPELINE",
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TelemetryDataRow(label = "Active Monitored App", value = activeAppName)
                    TelemetryDataRow(label = "Model Status", value = telemetry.modelStatusMessage)
                    TelemetryDataRow(label = "Loaded File", value = telemetry.loadedModelName ?: "None (Load .tflite)")
                    TelemetryDataRow(label = "Input Tensor Dimension", value = telemetry.modelInputSpec)
                    TelemetryDataRow(
                        label = "Output Format",
                        value = if (telemetry.modelOutputClasses > 0) "${telemetry.modelOutputClasses}-Class Auto" else "Unconfigured"
                    )
                    TelemetryDataRow(
                        label = "Active Detection Category",
                        value = telemetry.lastDetectedCategory?.displayName ?: "None (Clean Screen Stream)"
                    )
                }
            }

            // Privacy Assurance Note: No Saved Screenshots
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "STRICT ZERO-IMAGE RETENTION",
                                color = AegisPrimaryCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "In compliance with Aegis privacy architecture, screen frames are never converted to persisted JPEG/PNG files. Only numerical confidence scores and security incident metadata are recorded in the local log.",
                                color = AegisTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Test Threat Simulation Trigger (For Developer & User Testing)
            item {
                CyberCard(borderColor = AegisDangerRed.copy(alpha = 0.3f)) {
                    Text(
                        text = "SECURITY TESTING & SIMULATION",
                        color = AegisDangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulate an explicit content detection event to verify the local database audit log and test the emergency full-screen blocking overlay.",
                        color = AegisTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                containerColor = AegisDangerRed.copy(alpha = 0.12f),
                                contentColor = AegisDangerRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, AegisDangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .testTag("simulate_threat_button")
                        ) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "SIMULATE THREAT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TelemetryDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
