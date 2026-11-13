package com.example.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.MetricTile
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.ThreatCategoryBadge
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
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
    preferences: AegisPreferences,
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
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
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

    SheildlenzBackdrop(modifier = Modifier.testTag("dashboard_screen_root")) {
        Scaffold(
            topBar = {
                CyberTopBar(
                    title = "Sheildlenz",
                    subtitle = "Vision Protection Console",
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
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // Luxury Sanctuary Shield Hero Card
                item {
                    CyberCard(
                        borderColor = if (telemetry.isProtectionEnabled) {
                            AegisSuccessGreen.copy(alpha = 0.35f)
                        } else {
                            AegisChampagneGold.copy(alpha = 0.35f)
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusPill(
                                label = if (telemetry.isProtectionEnabled) "PROTECTION ACTIVE" else "SHIELD STANDBY",
                                isActive = telemetry.isProtectionEnabled,
                                activeColor = AegisSuccessGreen,
                                inactiveColor = AegisTextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
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
                                    size = 186.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (telemetry.isProtectionEnabled) "Visual Guardian Armed" else "Tap Crest to Arm",
                                color = AegisTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )

                            Text(
                                text = telemetry.statusMessage,
                                color = if (telemetry.isProtectionEnabled) AegisSuccessGreen else AegisTextMuted,
                                fontSize = 12.sp,
                                fontFamily = ClassicalBodyFont,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (telemetry.isProtectionEnabled) AegisPrimaryCyan.copy(alpha = 0.12f)
                                                else AegisTextMuted.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = if (telemetry.isProtectionEnabled) AegisPrimaryCyan else AegisTextMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Master Screen Shield",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalBodyFont
                                        )
                                        Text(
                                            text = if (telemetry.isProtectionEnabled) "Monitoring display frames live" else "Ready to engage capture",
                                            color = AegisTextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont
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
                                        checkedThumbColor = AegisOnPrimaryWhite,
                                        checkedTrackColor = AegisPrimaryCyan,
                                        uncheckedThumbColor = AegisTextMuted,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }
                    }
                }

                if (!hasGuardianPin) {
                    item {
                        CyberCard(
                            borderColor = AegisWarningAmber.copy(alpha = 0.45f),
                            onClick = onNavigateToSettings
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AegisWarningAmber.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisWarningAmber, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Configure Guardian Password",
                                        color = AegisWarningAmber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                    Text(
                                        text = "Tap to set a Guardian PIN so only the Guardian can open/edit the app or disable Device Admin.",
                                        color = AegisTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }
                    }
                }

                // Engine & Monitoring Bento Row
                item {
                    LuxurySectionHeader(title = "Core Subsystems")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val aiHeadline = when {
                            telemetry.modelErrorMessage != null -> "Error"
                            telemetry.isModelLoaded && telemetry.isProtectionEnabled -> "TFLite Live"
                            telemetry.isModelLoaded -> "Ready"
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
                            borderColor = aiColor.copy(alpha = 0.30f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(aiColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = aiColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "AI ENGINE", color = AegisTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont, letterSpacing = 0.8.sp)
                            }
                            Text(
                                text = aiHeadline,
                                color = aiColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = telemetry.loadedModelName ?: "Load .tflite file",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = ClassicalBodyFont
                            )
                        }

                        CyberCard(
                            modifier = Modifier.weight(1f),
                            borderColor = AegisElectricBlue.copy(alpha = 0.28f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AegisElectricBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = AegisElectricBlue, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "MONITORING", color = AegisTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont, letterSpacing = 0.8.sp)
                            }
                            Text(
                                text = if (telemetry.isProtectionEnabled) "VirtualDisplay" else "Standby",
                                color = if (telemetry.isProtectionEnabled) AegisElectricBlue else AegisTextMuted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Zero Cloud Leakage",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = ClassicalBodyFont
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
                    LuxurySectionHeader(title = "Protection Ledger")
                    Spacer(modifier = Modifier.height(10.dp))
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
                            color = AegisChampagneGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont,
                            letterSpacing = 1.sp
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = incident.appDisplayName,
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
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
                            if (incident.diagnosticSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = incident.diagnosticSummary,
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AegisTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zero security incidents recorded. System clear.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont
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
                                containerColor = AegisPrimaryCyan,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("dashboard_live_hud_button")
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "LIVE TELEMETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont, letterSpacing = 0.6.sp)
                        }

                        Button(
                            onClick = onNavigateToBlockScreen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisCardBackground,
                                contentColor = AegisDangerRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .border(1.dp, AegisDangerRed.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .testTag("dashboard_test_block_screen_button")
                        ) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "TEST SHIELD", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont, letterSpacing = 0.6.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (showOverlayWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showOverlayWarningDialog = false },
                    containerColor = AegisCardBackground,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text("Overlay Permission Required", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Sheildlenz requires the 'Display over other apps' permission to cast the protection barrier over browsers, apps, and video players when inappropriate visual content is detected.",
                            fontFamily = ClassicalBodyFont
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
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
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
                    containerColor = AegisCardBackground,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text("No AI Model Loaded", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "No .tflite classifier is attached yet. Load a compatible .tflite file from phone storage first so Sheildlenz can detect inappropriate content.",
                            fontFamily = ClassicalBodyFont
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMissingModelDialog = false
                                quickModelPickerLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
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
}
