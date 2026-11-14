package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
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
    val confirmFocusRequester = remember { FocusRequester() }

    val submitGate: () -> Unit = {
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
    }

    SheildlenzBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guardian_gate_screen")
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                CyberTopBar(title = if (hasGuardianPin) "Lock" else "Setup")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AegisShieldGraphic(
                    isProtected = telemetry.isProtectionEnabled,
                    isThreatActive = false,
                    size = 128.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatusPill(
                    label = if (telemetry.isProtectionEnabled) "SHIELD ACTIVE" else "SHIELD OFF",
                    isActive = telemetry.isProtectionEnabled,
                    activeColor = AegisPrimaryCyan,
                    inactiveColor = AegisChampagneGold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (hasGuardianPin) "Guardian Access Only" else "Create Guardian Password",
                    color = AegisTextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (hasGuardianPin) {
                        "Enter Master Key to unlock Sheildlenz."
                    } else {
                        "Set a 4–8 digit Master Key to guard Shield OFF and critical edits."
                    },
                    color = AegisTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontFamily = ClassicalBodyFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                CyberCard(
                    borderColor = AegisLavenderBorder,
                    tone = CyberCardTone.HERO
                ) {
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
                                text = if (hasGuardianPin) "Master Key / PIN" else "New Master Key (4–8 digits)",
                                fontFamily = ClassicalBodyFont
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPurple)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = if (hasGuardianPin) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmFocusRequester.requestFocus() },
                            onDone = { submitGate() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = pinError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPurple,
                            unfocusedBorderColor = AegisCardBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        if (hasGuardianPin) {
                                            submitGate()
                                        } else {
                                            confirmFocusRequester.requestFocus()
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
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
                                Text("Confirm Master Key", fontFamily = ClassicalBodyFont)
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisPurple)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitGate() }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisCardBorder,
                                focusedContainerColor = AegisCardBackground,
                                unfocusedContainerColor = AegisCardBackground,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(confirmFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                        if (event.type == KeyEventType.KeyDown) {
                                            submitGate()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("guardian_gate_confirm_pin_input")
                        )
                    }

                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = AegisWarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = submitGate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPurple,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guardian_gate_unlock_button")
                    ) {
                        Text(
                            text = if (hasGuardianPin) "UNLOCK SHEILDLENZ" else "SET KEY & ENTER",
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
