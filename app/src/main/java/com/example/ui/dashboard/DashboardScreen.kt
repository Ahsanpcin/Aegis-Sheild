package com.example.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.SampleImageTesterCard
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
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

@Composable
fun DashboardScreen(
    protectionRepo: ProtectionRepository,
    modelManager: ModelManager,
    preferences: AegisPreferences,
    onModelUpdated: () -> Unit = {},
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val telemetry by protectionRepo.telemetry.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val isStrictEverything by preferences.strictEverything.collectAsState()

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

    val masterKeyGate = rememberMasterKeyGate(preferences)

    // Animated ON/OFF Hero Card Transitions
    val isOn = telemetry.isProtectionEnabled

    val requestShieldToggle: (Boolean) -> Unit = { targetActive ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val actionLabel = if (targetActive) "turn Shield ON" else "turn Shield OFF"
        masterKeyGate.request(actionLabel) {
            if (targetActive) {
                attemptStartProtection()
            } else {
                onRequestStopProtection()
            }
        }
    }
    val heroBorderColor by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.58f) else AegisChampagneGold.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroBorderColor"
    )
    val heroTintBg by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.11f) else AegisChampagneGold.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroTintBackground"
    )
    val powerIconBg by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan else AegisPurple,
        animationSpec = tween(durationMillis = 360),
        label = "PowerIconBg"
    )
    val powerIconTint by animateColorAsState(
        targetValue = AegisOnPrimaryWhite,
        animationSpec = tween(durationMillis = 360),
        label = "PowerIconTint"
    )
    val powerIconRotation by animateFloatAsState(
        targetValue = if (isOn) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PowerIconRotation"
    )
    val cardPulseScale = remember { Animatable(1f) }
    var previousProtectionState by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isOn) {
        val prev = previousProtectionState
        previousProtectionState = isOn
        if (prev != null && prev != isOn) {
            cardPulseScale.snapTo(if (isOn) 0.975f else 1.015f)
            cardPulseScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("dashboard_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Home",
                    subtitle = "Sheildlenz Vision Guard"
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Compact On/Off Shield Control Widget (top) with smooth ON/OFF animations
                item {
                    CyberCard(
                        borderColor = heroBorderColor,
                        tone = CyberCardTone.HERO,
                        modifier = Modifier.graphicsLayer {
                            scaleX = cardPulseScale.value
                            scaleY = cardPulseScale.value
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(heroTintBg, AegisLavenderSoft.copy(alpha = 0.45f))
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { requestShieldToggle(!isOn) }
                                        .testTag("shield_graphic_toggle"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AegisShieldGraphic(
                                        isProtected = isOn,
                                        isThreatActive = false,
                                        size = 96.dp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    StatusPill(
                                        label = if (isOn) "PROTECTION ACTIVE" else "SHIELD STANDBY",
                                        isActive = isOn,
                                        activeColor = AegisPrimaryCyan,
                                        inactiveColor = AegisChampagneGold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AnimatedContent(
                                        targetState = isOn,
                                        transitionSpec = {
                                            (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                                slideInVertically(tween(260)) { if (targetState) it / 3 else -it / 3 })
                                                .togetherWith(
                                                    fadeOut(tween(180)) +
                                                        slideOutVertically(tween(180)) { if (targetState) -it / 3 else it / 3 }
                                                )
                                        },
                                        label = "ShieldHeadlineTransition"
                                    ) { armed ->
                                        Column {
                                            Text(
                                                text = if (armed) "Visual Guardian Armed" else "Tap Crest or Switch to Arm",
                                                color = AegisTextPrimary,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = ClassicalDisplayFont
                                            )
                                            Text(
                                                text = telemetry.statusMessage,
                                                color = if (armed) AegisSuccessGreen else AegisTextMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                fontFamily = ClassicalBodyFont,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(powerIconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = powerIconTint,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer {
                                                    rotationZ = powerIconRotation
                                                }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Master Screen Shield",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalBodyFont
                                        )
                                        AnimatedContent(
                                            targetState = isOn,
                                            transitionSpec = {
                                                fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                                            },
                                            label = "ShieldSubcaptionTransition"
                                        ) { armed ->
                                            Text(
                                                text = if (armed) "Monitoring frames" else "Tap to arm",
                                                color = if (armed) AegisSuccessGreen else AegisTextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = ClassicalBodyFont
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = isOn,
                                    onCheckedChange = { isChecked -> requestShieldToggle(isChecked) },
                                    modifier = Modifier.testTag("master_protection_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AegisOnPrimaryWhite,
                                        checkedTrackColor = AegisPrimaryCyan,
                                        uncheckedThumbColor = AegisOnPrimaryWhite,
                                        uncheckedTrackColor = AegisLavenderLight.copy(alpha = 0.55f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Load the Model (First-Time Setup Only — hides once loaded; change later in Edits)
                item {
                    AnimatedVisibility(
                        visible = !telemetry.isModelLoaded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ModelManagerCard(
                            telemetry = telemetry,
                            modelManager = modelManager,
                            onModelUpdated = onModelUpdated,
                            cardTone = CyberCardTone.GOLD,
                            onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                            onRequireAuth = { label, action ->
                                masterKeyGate.request(label) { action() }
                            }
                        )
                    }
                }

                // 3. Sample Image Tester Widget (bottom)
                item {
                    SampleImageTesterCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        sensitivity = sensitivity,
                        isStrictEverything = isStrictEverything,
                        cardTone = CyberCardTone.LAVENDER,
                        onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() }
                    )
                }
            }

            if (showOverlayWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showOverlayWarningDialog = false },
                    containerColor = AegisLavenderSoft,
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
                                    preferences.grantTemporarySettingsAuthorization()
                                    context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                                } catch (_: Exception) {
                                    context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                        ) {
                            Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showOverlayWarningDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisCardBackground,
                                contentColor = AegisTextPrimary
                            )
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (showMissingModelDialog) {
                AlertDialog(
                    onDismissRequest = { showMissingModelDialog = false },
                    containerColor = AegisChampagneSoft,
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
                                preferences.beginAuthorizedExternalFlow()
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
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisLavenderSoft,
                                contentColor = AegisPurple
                            )
                        ) {
                            Text("CAPTURE ONLY", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}
