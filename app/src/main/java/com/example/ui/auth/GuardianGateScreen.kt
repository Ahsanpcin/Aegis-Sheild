package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun GuardianGateScreen(
    preferences: AegisPreferences,
    protectionRepo: ProtectionRepository,
    onUnlocked: () -> Unit
) {
    val telemetry by protectionRepo.telemetry.collectAsState()
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
    var pinInput by remember(hasGuardianPin) { mutableStateOf("") }
    var confirmPinInput by remember(hasGuardianPin) { mutableStateOf("") }
    var pinError by remember(hasGuardianPin) { mutableStateOf<String?>(null) }

    SheildlenzBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guardian_gate_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AegisShieldGraphic(
                    isProtected = telemetry.isProtectionEnabled,
                    isThreatActive = false,
                    size = 140.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                StatusPill(
                    label = if (telemetry.isProtectionEnabled) "PROTECTION ARMED" else "SHIELD STANDBY",
                    isActive = telemetry.isProtectionEnabled,
                    activeColor = AegisSuccessGreen,
                    inactiveColor = AegisTextMuted
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (hasGuardianPin) "Guardian Access Only" else "Create Guardian Password",
                    color = AegisTextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (hasGuardianPin) {
                        "Enter Guardian Password to open Sheildlenz."
                    } else {
                        "Set a 4–8 digit password required at start, PIN change, and Shield toggle."
                    },
                    color = AegisTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = ClassicalBodyFont,
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
                        label = {
                            Text(
                                text = if (hasGuardianPin) "Guardian Password / PIN" else "New Guardian Password (4–8 digits)",
                                fontFamily = ClassicalBodyFont
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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

                    if (!hasGuardianPin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = {
                                if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                    confirmPinInput = it
                                    pinError = null
                                }
                            },
                            label = {
                                Text("Confirm Guardian Password", fontFamily = ClassicalBodyFont)
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPrimaryCyan,
                                unfocusedBorderColor = AegisCardBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guardian_gate_confirm_pin_input")
                        )
                    }

                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = AegisDangerRed,
                            fontSize = 12.sp,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (hasGuardianPin) {
                                if (pinInput.isNotEmpty() && preferences.verifyGuardianPin(pinInput)) {
                                    preferences.unlockGuardianSession()
                                    onUnlocked()
                                } else {
                                    pinError = "Incorrect Guardian Password"
                                    pinInput = ""
                                }
                            } else {
                                when {
                                    pinInput.length < 4 -> {
                                        pinError = "Password must be 4–8 digits"
                                    }
                                    pinInput != confirmPinInput -> {
                                        pinError = "Passwords do not match"
                                        confirmPinInput = ""
                                    }
                                    else -> {
                                        preferences.setGuardianPin(pinInput)
                                        onUnlocked()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPrimaryCyan,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guardian_gate_unlock_button")
                    ) {
                        Text(
                            text = if (hasGuardianPin) "UNLOCK GUARDIAN CONSOLE" else "SET PASSWORD & ENTER",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}
