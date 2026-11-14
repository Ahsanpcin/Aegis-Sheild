package com.example.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
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
    val scheduleEnabled by preferences.scheduleEnabled.collectAsState()
    val scheduleStartMinutes by preferences.scheduleStartMinutes.collectAsState()
    val scheduleEndMinutes by preferences.scheduleEndMinutes.collectAsState()
    val isWithinScheduledWindow = scheduleEnabled && preferences.isWithinScheduledWindow()

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
    val isOn = telemetry.isProtectionEnabled

    // Selective Master Key: turning ON requires NO password; turning OFF requires Master Key.
    val requestShieldToggle: (Boolean) -> Unit = { targetActive ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (targetActive) {
            attemptStartProtection()
        } else {
            masterKeyGate.request("turn Shield OFF") {
                onRequestStopProtection()
            }
        }
    }

    // Eye-comfort animated Shield stage transitions
    val infiniteTransition = rememberInfiniteTransition(label = "HomeShieldMotion")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave1"
    )
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, delayMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave2"
    )
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )
    val auraBreath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraBreath"
    )

    val activeProgress by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "ActiveProgress"
    )
    val heroBorderColor by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.55f) else AegisGoldBorder,
        animationSpec = tween(durationMillis = 480),
        label = "HeroBorderColor"
    )
    val actionBarFill by animateColorAsState(
        targetValue = if (isOn) AegisSkySoft else AegisLavenderSoft,
        animationSpec = tween(durationMillis = 420),
        label = "ActionBarFill"
    )
    val actionBarBorder by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.45f) else AegisLavenderBorder,
        animationSpec = tween(durationMillis = 420),
        label = "ActionBarBorder"
    )
    val powerIconBg by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan else AegisPurple,
        animationSpec = tween(durationMillis = 360),
        label = "PowerIconBg"
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
            cardPulseScale.snapTo(if (isOn) 0.965f else 1.02f)
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
                CyberTopBar(title = "Home")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Eye-Comfort Spacious Animated Shield Widget
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
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatusPill(
                                label = if (isOn) "ACTIVE" else "STANDBY",
                                isActive = isOn,
                                activeColor = AegisPrimaryCyan,
                                inactiveColor = AegisChampagneGold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Powerful animated orbital sonar stage around Shield Crest
                            Box(
                                modifier = Modifier
                                    .size(198.dp)
                                    .clip(CircleShape)
                                    .clickable { requestShieldToggle(!isOn) }
                                    .testTag("shield_graphic_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(198.dp)
                                        .graphicsLayer {
                                            val s = 1f + (auraBreath - 1f) * activeProgress
                                            scaleX = s
                                            scaleY = s
                                        }
                                ) {
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    val maxR = size.minDimension * 0.48f

                                    // Expanding sonar wave pulses when active
                                    if (activeProgress > 0.02f) {
                                        listOf(sonarWave1 to AegisPrimaryCyan, sonarWave2 to AegisPurple).forEach { (wave, waveColor) ->
                                            val r = maxR * (0.58f + 0.40f * wave)
                                            val alpha = (1f - wave) * 0.42f * activeProgress
                                            drawCircle(
                                                color = waveColor.copy(alpha = alpha),
                                                radius = r,
                                                center = c,
                                                style = Stroke(width = (3.5f * (1f - wave * 0.55f)).dp.toPx())
                                            )
                                        }
                                    }

                                    // Rotating 3-color orbital arc ring
                                    val ringR = maxR * 0.84f
                                    val arcTopLeft = Offset(c.x - ringR, c.y - ringR)
                                    val arcSize = Size(ringR * 2f, ringR * 2f)
                                    rotate(degrees = orbitRotation * (0.35f + 0.65f * activeProgress), pivot = c) {
                                        drawArc(
                                            color = AegisPrimaryCyan.copy(alpha = 0.25f + 0.55f * activeProgress),
                                            startAngle = 0f,
                                            sweepAngle = 82f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = AegisPurple.copy(alpha = 0.25f + 0.55f * activeProgress),
                                            startAngle = 120f,
                                            sweepAngle = 82f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = AegisChampagneGold.copy(alpha = 0.35f + 0.45f * activeProgress),
                                            startAngle = 240f,
                                            sweepAngle = 82f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }

                                AegisShieldGraphic(
                                    isProtected = isOn,
                                    isThreatActive = false,
                                    size = 132.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Clean, spacious, high-readability text (minimal words)
                            AnimatedContent(
                                targetState = isOn,
                                transitionSpec = {
                                    (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f))
                                        .togetherWith(fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f))
                                },
                                label = "ShieldCleanStatus"
                            ) { armed ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (armed) "Shield Active" else "Shield Off",
                                        color = AegisTextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (armed) "Screen Guarded" else "Tap to Protect",
                                        color = if (armed) AegisPrimaryCyan else AegisPurple,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Wide Eye-Comfort Interactive Action Pill & Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(actionBarFill)
                                    .border(1.3.dp, actionBarBorder, RoundedCornerShape(18.dp))
                                    .clickable { requestShieldToggle(!isOn) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(powerIconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = "Shield power toggle",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer {
                                                    rotationZ = powerIconRotation
                                                }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Screen Shield",
                                            color = AegisTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = if (isOn) "Tap switch to pause" else "Tap switch to start",
                                            color = AegisTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
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

                // 1b. Scheduled Guard Window Reminder (when schedule is active now while Shield is OFF)
                item {
                    AnimatedVisibility(
                        visible = isWithinScheduledWindow && !isOn,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CyberCard(
                            borderColor = AegisLavenderBorder,
                            tone = CyberCardTone.LAVENDER,
                            modifier = Modifier.testTag("scheduled_guard_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AegisPurple),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Scheduled Guard Active",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Scheduled Guard Window",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = "${AegisPreferences.formatClockTime(scheduleStartMinutes)} – ${AegisPreferences.formatClockTime(scheduleEndMinutes)} active now",
                                            color = AegisPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }

                                Button(
                                    onClick = { attemptStartProtection() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisPrimaryCyan,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("schedule_arm_now_button")
                                ) {
                                    Text(
                                        text = "ARM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. First-Time Model Setup Card (only shown when no .tflite is loaded; no Master Key needed)
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
                            showCloudDownloader = true,
                            onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                            onRequireAuth = { label, action ->
                                masterKeyGate.request(label) { action() }
                            }
                        )
                    }
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
                            "Sheildlenz requires 'Display over other apps' permission to shield the screen when unsafe visual content is detected.",
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
                            "No .tflite classifier is attached yet. Load a compatible .tflite file from phone storage first.",
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
