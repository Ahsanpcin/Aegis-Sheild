package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.core.permissions.PermissionHelper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.EngineTelemetry
import com.example.core.models.SecurityIncident
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.MetricTile
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.ThreatCategoryBadge
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    protectionRepo: ProtectionRepository,
    historyRepo: SecurityHistoryRepository,
    modelManager: ModelManager,
    onNavigateToLive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlockScreen: () -> Unit,
    onModelUpdated: () -> Unit = {},
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val telemetry by protectionRepo.telemetry.collectAsState()
    val totalDetections by historyRepo.getTotalDetectionsCount().collectAsState(initial = 0)
    val blockedAppCount by historyRepo.getBlockedAppCount().collectAsState(initial = 0)
    val latestIncident by historyRepo.getLatestIncident().collectAsState(initial = null)

    var showOverlayWarningDialog by remember { mutableStateOf(false) }
    var showMissingModelDialog by remember { mutableStateOf(false) }

    val quickModelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) { modelManager.importModelFromUri(uri) }
                onModelUpdated()
                if (res is ModelImportResult.Success && telemetry.isOverlayPermitted && !telemetry.isProtectionEnabled) {
                    onRequestStartProtection()
                }
            }
        }
    }

    val attemptStartProtection = {
        when {
            !telemetry.isOverlayPermitted -> showOverlayWarningDialog = true
            !telemetry.isModelLoaded -> showMissingModelDialog = true
            else -> onRequestStartProtection()
        }
    }

    Scaffold(
        topBar = {
            CyberTopBar(
                title = "AEGIS SHIELD",
                subtitle = "Security Command Console",
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("dashboard_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "History Log", tint = AegisPrimaryCyan)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = AegisPrimaryCyan)
                    }
                }
            )
        },
        containerColor = AegisVoidBackground,
        modifier = Modifier.testTag("dashboard_screen_root")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Center Large Protection Shield with Glow & State
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                if (telemetry.isProtectionEnabled) {
                                    onRequestStopProtection()
                                } else {
                                    attemptStartProtection()
                                }
                            }
                            .testTag("shield_graphic_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        AegisShieldGraphic(
                            isProtected = telemetry.isProtectionEnabled,
                            isThreatActive = false,
                            size = 190.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    StatusPill(
                        label = if (telemetry.isProtectionEnabled) "PROTECTION ACTIVE" else "SHIELD DISENGAGED",
                        isActive = telemetry.isProtectionEnabled,
                        activeColor = AegisSuccessGreen,
                        inactiveColor = AegisTextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (telemetry.isProtectionEnabled)
                            "Real-time visual guardian is armed"
                        else
                            "Tap shield to arm content guardian",
                        color = AegisTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Master Protection Toggle Card
            item {
                CyberCard(
                    borderColor = if (telemetry.isProtectionEnabled) AegisPrimaryCyan.copy(alpha = 0.5f) else AegisCardBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (telemetry.isProtectionEnabled) AegisPrimaryCyan.copy(alpha = 0.2f)
                                        else AegisTextMuted.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (telemetry.isProtectionEnabled) AegisPrimaryCyan else AegisTextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MASTER SHIELD",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = telemetry.statusMessage,
                                    color = if (telemetry.isProtectionEnabled) AegisSuccessGreen else AegisTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = telemetry.isProtectionEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    attemptStartProtection()
                                } else {
                                    onRequestStopProtection()
                                }
                            },
                            modifier = Modifier.testTag("master_protection_switch"),
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

            // Quick Diagnostic Status (AI Engine & Screen Monitoring)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val aiHeadline = when {
                        telemetry.modelErrorMessage != null -> "Model Error"
                        telemetry.isModelLoaded && telemetry.isProtectionEnabled -> "TFLite Active"
                        telemetry.isModelLoaded -> "Model Ready"
                        else -> "No Model"
                    }
                    val aiColor = when {
                        telemetry.modelErrorMessage != null -> AegisDangerRed
                        telemetry.isModelLoaded && telemetry.isProtectionEnabled -> AegisSuccessGreen
                        telemetry.isModelLoaded -> AegisPrimaryCyan
                        else -> AegisWarningAmber
                    }

                    CyberCard(
                        modifier = Modifier.weight(1f),
                        borderColor = aiColor.copy(alpha = 0.35f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = aiColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AI ENGINE", color = AegisTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = aiHeadline,
                            color = aiColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = telemetry.loadedModelName ?: "Load .tflite file",
                            color = AegisTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    CyberCard(
                        modifier = Modifier.weight(1f),
                        borderColor = AegisElectricBlue.copy(alpha = 0.3f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = AegisElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "MONITORING", color = AegisTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = if (telemetry.isProtectionEnabled) "VirtualDisplay" else "Disengaged",
                            color = if (telemetry.isProtectionEnabled) AegisElectricBlue else AegisTextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = "Zero Cloud Leakage",
                            color = AegisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // In-App TFLite Model Loader Card
            item {
                ModelManagerCard(
                    telemetry = telemetry,
                    modelManager = modelManager,
                    onModelUpdated = onModelUpdated
                )
            }

            // Metrics: Total Detections & Blocked Applications
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Detections",
                        value = totalDetections.toString(),
                        icon = Icons.Default.Security,
                        accentColor = if (totalDetections > 0) AegisDangerRed else AegisPrimaryCyan,
                        caption = "Total blocked events",
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Blocked Apps",
                        value = blockedAppCount.toString(),
                        icon = Icons.Default.AppBlocking,
                        accentColor = AegisPurple,
                        caption = "Unique applications",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Last Detection Information
            item {
                CyberCard(borderColor = AegisCardBorder) {
                    Text(
                        text = "LAST DETECTION LOG",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (latestIncident != null) {
                        val incident = latestIncident!!
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
                            .format(Date(incident.timestampMillis))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = incident.appDisplayName,
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formattedDate,
                                    color = AegisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            ThreatCategoryBadge(category = incident.category)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AegisTextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero security incidents recorded. System clear.",
                                color = AegisTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons (Live HUD & Block Screen Test)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToLive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisSecondaryBackground,
                            contentColor = AegisPrimaryCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("dashboard_live_hud_button")
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "LIVE TELEMETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = onNavigateToBlockScreen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisSecondaryBackground,
                            contentColor = AegisDangerRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, AegisDangerRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("dashboard_test_block_screen_button")
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "TEST SHIELD", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showOverlayWarningDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayWarningDialog = false },
                containerColor = AegisSecondaryBackground,
                titleContentColor = AegisTextPrimary,
                textContentColor = AegisTextSecondary,
                title = { Text("Overlay Permission Required") },
                text = {
                    Text(
                        "Aegis Shield requires the 'Display over other apps' permission to cast the protection barrier over browsers, apps, and video players when inappropriate visual content is detected."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOverlayWarningDialog = false
                            try {
                                context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                            } catch (_: Exception) {
                                context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisVoidBackground)
                    ) {
                        Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showOverlayWarningDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary)
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }

        if (showMissingModelDialog) {
            AlertDialog(
                onDismissRequest = { showMissingModelDialog = false },
                containerColor = AegisSecondaryBackground,
                titleContentColor = AegisTextPrimary,
                textContentColor = AegisTextSecondary,
                title = { Text("No AI Model Loaded") },
                text = {
                    Text(
                        "No .tflite classifier is attached yet. Load a compatible .tflite file from phone storage first so Aegis Shield can detect inappropriate content."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMissingModelDialog = false
                            quickModelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisVoidBackground)
                    ) {
                        Text("LOAD .TFLITE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showMissingModelDialog = false
                            onRequestStartProtection()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisWarningAmber)
                    ) {
                        Text("CAPTURE ONLY")
                    }
                }
            )
        }
    }
}
