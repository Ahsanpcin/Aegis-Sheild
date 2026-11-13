package com.example.ui.blocking

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.ThreatCategory
import com.example.data.preferences.AegisPreferences
import com.example.services.protection.AegisLockdownService
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun BlockOverlayScreen(
    category: ThreatCategory = ThreatCategory.EXPLICIT_ADULT,
    targetAppName: String = "Active Screen",
    preferences: AegisPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val doubleTimeMinutes = remember(preferences) { preferences.getDoubleTimeDurationMinutes() }
    val doubleTimeLabel = remember(doubleTimeMinutes) { AegisPreferences.formatMinutesLabel(doubleTimeMinutes) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        showPinDialog = true
    }

    val pulseTransition = rememberInfiniteTransition(label = "WarningPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AegisVoidBackground,
                        AegisLavenderSoft,
                        AegisChampagneSoft
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("block_overlay_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AegisDangerRed.copy(alpha = 0.12f * pulseAlpha))
                    .border(1.dp, AegisDangerRed.copy(alpha = 0.70f * pulseAlpha), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AegisDangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHEILDLENZ INTERVENTION ACTIVE",
                        color = AegisDangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            AegisShieldGraphic(
                isProtected = true,
                isThreatActive = true,
                size = 172.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Content Blocked",
                color = AegisTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )

            Text(
                text = "Inappropriate Visual Content Detected",
                color = AegisDangerRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            CyberCard(
                borderColor = AegisGoldBorder,
                tone = CyberCardTone.GOLD
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visual content matching ${category.displayName} was identified on the active display. The screen has been obscured to uphold protection policies.",
                        color = AegisTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        fontFamily = ClassicalBodyFont
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "TARGET: $targetAppName • ACTION: DISPLAY SHIELDED",
                        color = AegisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "POLICY: Master Code or ${doubleTimeLabel.uppercase()} Double-Time Wait • Shield Remains ON",
                        color = AegisPrimaryCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("block_screen_return_home_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RETURN TO HOME SCREEN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    pinInput = ""
                    pinError = null
                    showPinDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("block_screen_guardian_override_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisLavenderSoft,
                    contentColor = AegisPurple
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MASTER CODE OVERRIDE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = AegisLavenderSoft,
                titleContentColor = AegisTextPrimary,
                title = { Text("Master Code Verification", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Enter the Guardian Master Code to continue. If you do not know the Master Code, start a $doubleTimeLabel Double-Time Wait. Sheildlenz Shield remains ON after both.",
                            color = AegisTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = ClassicalBodyFont
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pinInput = it },
                            label = { Text("Master Code / PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(12.dp),
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPrimaryCyan,
                                unfocusedBorderColor = AegisCardBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = AegisDangerRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                preferences.engageFullDeviceLockdown(
                                    appName = targetAppName,
                                    packageName = "",
                                    categoryName = category.displayName,
                                    details = "Double-Time Wait ($doubleTimeLabel) selected because Master Code was unknown."
                                )
                                preferences.engageDoubleTimeLockdown()
                                AegisLockdownService.start(context)
                                showPinDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisChampagneGold.copy(alpha = 0.12f),
                                contentColor = AegisWarningAmber
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("block_screen_double_time_button")
                        ) {
                            Text(
                                text = "DON'T KNOW CODE? WAIT DOUBLE TIME (${doubleTimeLabel.uppercase()})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (preferences.verifyGuardianPin(pinInput)) {
                                preferences.clearLockdown()
                                preferences.setShieldDesiredActive(true)
                                showPinDialog = false
                                onDismiss()
                            } else {
                                pinError = "Incorrect Master Code. Use Double-Time Wait if unknown."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPrimaryCyan,
                            contentColor = AegisOnPrimaryWhite
                        )
                    ) {
                        Text("VERIFY (SHIELD ON)", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showPinDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary)
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}
