package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun GuardianGateScreen(
    preferences: AegisPreferences,
    protectionRepo: ProtectionRepository,
    onUnlocked: () -> Unit
) {
    val telemetry by protectionRepo.telemetry.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AegisVoidBackground, AegisSecondaryBackground, AegisVoidBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("guardian_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AegisShieldGraphic(
                isProtected = telemetry.isProtectionEnabled,
                isThreatActive = false,
                size = 132.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatusPill(
                label = if (telemetry.isProtectionEnabled) "PROTECTION ARMED" else "SHIELD STANDBY",
                isActive = telemetry.isProtectionEnabled,
                activeColor = AegisSuccessGreen,
                inactiveColor = AegisTextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Guardian Access Only",
                color = AegisTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Only the Guardian may open Aegis Shield to adjust policies, load models, view logs, or alter protection.",
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            CyberCard(borderColor = AegisPrimaryCyan.copy(alpha = 0.25f)) {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                            pinInput = it
                            pinError = null
                        }
                    },
                    label = { Text("Guardian Password / PIN") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = pinError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AegisPrimaryCyan,
                        unfocusedBorderColor = AegisCardBorder,
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guardian_gate_pin_input")
                )

                if (pinError != null) {
                    Text(
                        text = pinError!!,
                        color = AegisDangerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (preferences.verifyGuardianPin(pinInput)) {
                            preferences.unlockGuardianSession()
                            onUnlocked()
                        } else {
                            pinError = "Incorrect Guardian Password"
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guardian_gate_unlock_button")
                ) {
                    Text(
                        text = "UNLOCK GUARDIAN CONSOLE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
