package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Stable
class MasterKeyGateState(
    private val preferences: AegisPreferences
) {
    internal var pendingActionLabel by mutableStateOf<String?>(null)
    private var pendingAction: (() -> Unit)? = null
    private var pendingCancel: (() -> Unit)? = null

    fun request(
        actionLabel: String,
        onCancel: () -> Unit = {},
        onAuthorized: () -> Unit
    ) {
        pendingActionLabel = actionLabel
        pendingCancel = onCancel
        pendingAction = onAuthorized
    }

    internal fun dismiss() {
        val cancelCallback = pendingCancel
        pendingActionLabel = null
        pendingAction = null
        pendingCancel = null
        cancelCallback?.invoke()
    }

    internal fun submitKey(keyInput: String): String? {
        val hasPin = preferences.hasGuardianPin.value
        return if (hasPin) {
            if (keyInput.isEmpty() || !preferences.verifyGuardianPin(keyInput)) {
                "Incorrect Master Key"
            } else {
                val action = pendingAction
                pendingActionLabel = null
                pendingAction = null
                pendingCancel = null
                action?.invoke()
                null
            }
        } else {
            if (keyInput.length < 4) {
                "Master Key must be 4–8 digits"
            } else {
                preferences.setGuardianPin(keyInput)
                val action = pendingAction
                pendingActionLabel = null
                pendingAction = null
                pendingCancel = null
                action?.invoke()
                null
            }
        }
    }
}

@Composable
fun rememberMasterKeyGate(preferences: AegisPreferences): MasterKeyGateState {
    val state = remember(preferences) { MasterKeyGateState(preferences) }
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
    val actionLabel = state.pendingActionLabel

    if (actionLabel != null) {
        var keyInput by remember(actionLabel) { mutableStateOf("") }
        var keyError by remember(actionLabel) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { state.dismiss() },
            containerColor = AegisCardBackground,
            titleContentColor = AegisTextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasGuardianPin) "Master Key Required" else "Create Master Key",
                        fontFamily = ClassicalDisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (hasGuardianPin) {
                            "Enter Master Key to $actionLabel."
                        } else {
                            "Set a 4–8 digit Master Key to $actionLabel."
                        },
                        color = AegisTextSecondary,
                        fontSize = 13.sp,
                        fontFamily = ClassicalBodyFont
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                keyInput = it
                                keyError = null
                            }
                        },
                        label = { Text("Master Key (4–8 digits)", fontFamily = ClassicalBodyFont) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = keyError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPrimaryCyan,
                            unfocusedBorderColor = AegisCardBorder,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_key_prompt_input")
                    )
                    if (keyError != null) {
                        Text(
                            text = keyError!!,
                            color = AegisDangerRed,
                            fontSize = 12.sp,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val err = state.submitKey(keyInput)
                        if (err != null) {
                            keyError = err
                            keyInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier.testTag("master_key_prompt_confirm")
                ) {
                    Text(
                        text = if (hasGuardianPin) "VERIFY" else "SET & APPLY",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { state.dismiss() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary),
                    modifier = Modifier.testTag("master_key_prompt_cancel")
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    return state
}
