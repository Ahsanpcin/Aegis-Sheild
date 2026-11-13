package com.example.ui.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.SecurityIncident
import com.example.data.repository.SecurityHistoryRepository
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SeverityTag
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.SheildlenzScaffoldBrandBar
import com.example.ui.components.ThreatCategoryBadge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SecurityHistoryScreen(
    historyRepo: SecurityHistoryRepository,
    onNavigateBack: () -> Unit
) {
    val incidents by historyRepo.getIncidents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    SheildlenzBackdrop(modifier = Modifier.testTag("security_history_screen_root")) {
        Scaffold(
            topBar = {
                CyberTopBar(
                    title = "Security History",
                    subtitle = "Detection Audit & Forensics",
                    onNavigateBack = onNavigateBack,
                    actions = {
                        if (incidents.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = AegisDangerRed
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = { SheildlenzScaffoldBrandBar() },
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (incidents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CyberCard(borderColor = AegisChampagneGold.copy(alpha = 0.35f)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(AegisPrimaryCyan.copy(alpha = 0.08f))
                                    .border(1.5.dp, AegisChampagneGold.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = AegisPrimaryCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "No Threats Detected",
                                color = AegisTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your device screen has maintained 100% clean visual compliance. Detected violations and automatic interventions will be logged here.",
                                color = AegisTextMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontFamily = ClassicalBodyFont,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        LuxurySectionHeader(title = "Recorded Incidents (${incidents.size})")
                    }

                    items(incidents, key = { it.id }) { incident ->
                        IncidentItemCard(
                            incident = incident,
                            onToggleFalsePositive = {
                                scope.launch {
                                    historyRepo.markFalsePositive(incident.id, !incident.isReviewedFalsePositive)
                                }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    containerColor = AegisCardBackground,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text(text = "Clear Security Audit Log?", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "This will permanently purge all recorded detection history and threat timestamps from local SQLite storage.",
                            fontFamily = ClassicalBodyFont
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    historyRepo.clearHistory()
                                    showClearDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisDangerRed,
                                contentColor = AegisOnPrimaryWhite
                            )
                        ) {
                            Text(text = "PURGE LOG", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showClearDialog = false },
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
fun IncidentItemCard(
    incident: SecurityIncident,
    onToggleFalsePositive: () -> Unit
) {
    val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        .format(Date(incident.timestampMillis))

    CyberCard(
        borderColor = if (incident.isReviewedFalsePositive) AegisTextMuted.copy(alpha = 0.28f)
        else AegisDangerRed.copy(alpha = 0.30f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.appDisplayName,
                    color = AegisTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = incident.packageName,
                    color = AegisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            SeverityTag(severity = incident.severity)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThreatCategoryBadge(category = incident.category)
            Text(
                text = "Confidence ${(incident.confidenceScore * 100).toInt()}%",
                color = AegisPrimaryCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Action: ${incident.actionTaken}",
            color = AegisTextSecondary,
            fontSize = 12.sp,
            fontFamily = ClassicalBodyFont,
            fontWeight = FontWeight.Medium
        )

        if (incident.diagnosticSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AegisSurfaceElevated)
                    .border(1.dp, AegisCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = incident.diagnosticSummary,
                    color = AegisTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        }

        if (incident.lockdownDurationMinutes > 0) {
            Text(
                text = "Lockdown Penalty Enforced: ${com.example.data.preferences.AegisPreferences.formatMinutesLabel(incident.lockdownDurationMinutes)}",
                color = AegisDangerRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedDate,
                color = AegisTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            OutlinedButton(
                onClick = onToggleFalsePositive,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (incident.isReviewedFalsePositive) AegisSuccessGreen else AegisWarningAmber
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (incident.isReviewedFalsePositive) Icons.Default.Check else Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (incident.isReviewedFalsePositive) "MARKED SAFE" else "FALSE POSITIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
        }
    }
}
