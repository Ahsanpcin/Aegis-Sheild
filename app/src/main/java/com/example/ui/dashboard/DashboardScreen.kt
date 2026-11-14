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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.example.BuildConfig
import com.example.core.permissions.PermissionHelper
import com.example.core.update.ApkDownloadState
import com.example.core.update.UpdateChecker
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
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

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

    val cardPulseScale = remember { Animatable(1f) }
    val buttonBounceScale = remember { Animatable(1f) }
    var previousProtectionState by remember { mutableStateOf<Boolean?>(null) }

    val requestShieldToggle: (Boolean) -> Unit = { targetActive ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            buttonBounceScale.snapTo(0.94f)
            buttonBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        if (targetActive) {
            attemptStartProtection()
        } else {
            masterKeyGate.request("turn Shield OFF") {
                onRequestStopProtection()
            }
        }
    }

    LaunchedEffect(isOn) {
        val prev = previousProtectionState
        previousProtectionState = isOn
        if (prev != null && prev != isOn) {
            cardPulseScale.snapTo(if (isOn) 0.968f else 1.018f)
            cardPulseScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Continuous VIP living animations for stage halo, status indicator, and button sheen
    val infiniteTransition = rememberInfiniteTransition(label = "VipHomeMotion")
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
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )
    val statusDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusDotPulse"
    )
    val powerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PowerPulseScale"
    )
    val buttonSheenSlide by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ButtonSheenSlide"
    )
    val eqWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqWavePhase"
    )

    val heroElevation by animateDpAsState(
        targetValue = if (isOn) 18.dp else 10.dp,
        animationSpec = tween(durationMillis = 480),
        label = "HeroElevation"
    )
    val heroShadowColor by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.26f) else AegisPurple.copy(alpha = 0.16f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroShadow"
    )
    val heroBorderEnd by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.58f) else AegisLavenderBorder.copy(alpha = 0.62f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroBorderEnd"
    )
    val buttonGradStart by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan else AegisPurple,
        animationSpec = tween(durationMillis = 420),
        label = "BtnGradStart"
    )
    val buttonGradEnd by animateColorAsState(
        targetValue = if (isOn) AegisElectricBlue else AegisPrimaryCyan,
        animationSpec = tween(durationMillis = 420),
        label = "BtnGradEnd"
    )
    val powerIconRotation by animateFloatAsState(
        targetValue = if (isOn) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PowerIconRotation"
    )

    val updateInfo by UpdateChecker.updateInfo.collectAsState()
    val apkDownloadState by UpdateChecker.apkDownloadState.collectAsState()
    val homeBannerHidden by UpdateChecker.homeBannerHidden.collectAsState()

    val showUpdateBanner = updateInfo?.isUpdateAvailable == true &&
        !homeBannerHidden &&
        apkDownloadState !is ApkDownloadState.Downloading &&
        apkDownloadState !is ApkDownloadState.Installing &&
        apkDownloadState !is ApkDownloadState.Installed &&
        (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.needsInstallPermission != true

    val hasAnyExtraCard = showUpdateBanner || (isWithinScheduledWindow && !isOn) || !telemetry.isModelLoaded

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
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                verticalArrangement = if (hasAnyExtraCard) {
                    Arrangement.spacedBy(14.dp, Alignment.Top)
                } else {
                    Arrangement.Center
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 0. Update Banner
                item {
                    AnimatedVisibility(
                        visible = showUpdateBanner,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val info = updateInfo
                        if (info != null) {
                            val currentLabel = "v${UpdateChecker.cleanVersionTag(BuildConfig.VERSION_NAME)}"
                            val latestLabel = "v${UpdateChecker.cleanVersionTag(info.latestVersion)}"
                            val isReadyPreloaded = (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.preloadedReady == true
                            val isConflict = apkDownloadState is ApkDownloadState.SignatureConflict
                            CyberCard(
                                borderColor = if (isReadyPreloaded) AegisLavenderBorder else AegisGoldBorder,
                                tone = if (isReadyPreloaded) CyberCardTone.LAVENDER else CyberCardTone.GOLD,
                                modifier = Modifier
                                    .widthIn(max = 580.dp)
                                    .testTag("home_update_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isReadyPreloaded) AegisPurple else AegisChampagneGold),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Update available",
                                                tint = AegisOnPrimaryWhite,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isReadyPreloaded) {
                                                        "Update Ready: $latestLabel"
                                                    } else {
                                                        "Update Available: $latestLabel"
                                                    },
                                                    color = AegisTextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = ClassicalDisplayFont
                                                )
                                                if (isReadyPreloaded) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(AegisPurple.copy(alpha = 0.16f))
                                                            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "READY 0s",
                                                            color = AegisPurple,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = ClassicalBodyFont
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = when (val st = apkDownloadState) {
                                                    is ApkDownloadState.Preloading -> {
                                                        val pct = st.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                                                        "Preloading silently (100 KB/s) • $pct"
                                                    }
                                                    is ApkDownloadState.PausedLowBattery -> {
                                                        val saved = UpdateChecker.formatBytesMb(st.bytesDownloaded)
                                                        "Preload paused (Battery <= 20%) • $saved"
                                                    }
                                                    ApkDownloadState.Verifying -> "Verifying SHA-256 & APK integrity..."
                                                    is ApkDownloadState.ReadyToInstall -> when {
                                                        st.preloadedReady -> "Verified Ready • Instant 1-Tap Install"
                                                        else -> "App $currentLabel → $latestLabel"
                                                    }
                                                    is ApkDownloadState.SignatureConflict -> st.reason
                                                    is ApkDownloadState.Failed -> st.message
                                                    else -> "App $currentLabel → Latest $latestLabel"
                                                },
                                                color = if (isReadyPreloaded) AegisPurple else AegisChampagneGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = ClassicalBodyFont
                                            )
                                        }
                                    }

                                    when (val st = apkDownloadState) {
                                        is ApkDownloadState.Preloading -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.hideHomeUpdateBanner()
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AegisPrimaryCyan,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("home_finish_preload_button")
                                            ) {
                                                Text("FINISH NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        ApkDownloadState.Verifying -> {}
                                        is ApkDownloadState.ReadyToInstall -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.hideHomeUpdateBanner()
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AegisPurple,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("home_install_ready_button")
                                            ) {
                                                Text(
                                                    text = if (st.preloadedReady) "INSTALL (0s)" else "INSTALL",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.hideHomeUpdateBanner()
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isConflict) AegisChampagneGold else AegisPrimaryCyan,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("home_download_update_button")
                                            ) {
                                                Text(
                                                    text = if (isConflict) "UPGRADE" else "UPDATE",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                if (apkDownloadState is ApkDownloadState.Preloading || apkDownloadState is ApkDownloadState.Verifying) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val frac = (apkDownloadState as? ApkDownloadState.Preloading)?.progress
                                    if (frac != null && apkDownloadState !is ApkDownloadState.Verifying) {
                                        LinearProgressIndicator(
                                            progress = { frac },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AegisChampagneGold,
                                            trackColor = AegisChampagneGold.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AegisChampagneGold,
                                            trackColor = AegisChampagneGold.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Pure VIP Shield-Only Sanctuary Hero Widget
                item {
                    val heroShape = RoundedCornerShape(34.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 540.dp)
                            .graphicsLayer {
                                scaleX = cardPulseScale.value
                                scaleY = cardPulseScale.value
                            }
                            .shadow(
                                elevation = heroElevation,
                                shape = heroShape,
                                ambientColor = heroShadowColor,
                                spotColor = heroShadowColor
                            )
                            .testTag("vip_quantum_shield_card"),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.96f), heroBorderEnd)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFCFEFF),
                                            Color(0xFFF1F6FF),
                                            Color(0xFFF4EFFE)
                                        )
                                    )
                                )
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Status & Sensitivity Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isOn) AegisPrimaryCyan.copy(alpha = 0.11f)
                                            else AegisPurple.copy(alpha = 0.09f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isOn) AegisPrimaryCyan.copy(alpha = 0.30f)
                                            else AegisLavenderBorder.copy(alpha = 0.58f),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .graphicsLayer {
                                                scaleX = statusDotPulse
                                                scaleY = statusDotPulse
                                            }
                                            .clip(CircleShape)
                                            .background(if (isOn) AegisPrimaryCyan else AegisPurple.copy(alpha = 0.80f))
                                    )
                                    Text(
                                        text = if (isOn) "ACTIVE" else "STANDBY",
                                        color = if (isOn) AegisPrimaryCyan else AegisPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }

                                Text(
                                    text = sensitivity.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = ClassicalBodyFont
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pure VIP Orbital Shield Stage with continuous fluid ripples & sweep halo
                            Box(
                                modifier = Modifier
                                    .size(216.dp)
                                    .clip(CircleShape)
                                    .clickable { requestShieldToggle(!isOn) }
                                    .testTag("shield_graphic_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(216.dp)) {
                                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                                    val maxR = this.size.minDimension * 0.48f

                                    // Expanding soothing radar ripples
                                    listOf(sonarWave1 to AegisElectricBlue, sonarWave2 to AegisPurple).forEach { (wave, waveColor) ->
                                        val r = maxR * (0.54f + 0.44f * wave)
                                        val alpha = (1f - wave) * (if (isOn) 0.48f else 0.24f)
                                        drawCircle(
                                            color = waveColor.copy(alpha = alpha),
                                            radius = r,
                                            center = c,
                                            style = Stroke(width = 2.6.dp.toPx() * (1f - wave * 0.45f))
                                        )
                                    }

                                    // Subtle dotted celestial track + smooth sweep glow ring
                                    val trackR = maxR * 0.90f
                                    drawCircle(
                                        color = (if (isOn) AegisPrimaryCyan else AegisLavenderBorder).copy(alpha = 0.28f),
                                        radius = trackR,
                                        center = c,
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                                        )
                                    )

                                    val arcR = maxR * 0.85f
                                    val arcTopLeft = Offset(c.x - arcR, c.y - arcR)
                                    val arcSize = Size(arcR * 2f, arcR * 2f)
                                    rotate(degrees = ringRotation, pivot = c) {
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    (if (isOn) AegisElectricBlue else AegisPurple).copy(alpha = 0.45f),
                                                    (if (isOn) AegisPrimaryCyan else AegisLavenderLight).copy(alpha = 0.85f),
                                                    Color.Transparent
                                                ),
                                                center = c
                                            ),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    // Floating orbital light sparks
                                    for (i in 0..2) {
                                        val angleRad = ((ringRotation + i * 120f) * Math.PI / 180.0)
                                        val px = c.x + (trackR * cos(angleRad)).toFloat()
                                        val py = c.y + (trackR * sin(angleRad)).toFloat()
                                        drawCircle(
                                            color = if (isOn) AegisElectricBlue.copy(alpha = 0.85f) else AegisPurple.copy(alpha = 0.65f),
                                            radius = 3.dp.toPx(),
                                            center = Offset(px, py)
                                        )
                                    }
                                }

                                AegisShieldGraphic(
                                    isProtected = isOn,
                                    isThreatActive = false,
                                    size = 160.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            AnimatedContent(
                                targetState = isOn,
                                transitionSpec = {
                                    (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.94f))
                                        .togetherWith(fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f))
                                },
                                label = "VipQuantumHeadline"
                            ) { armed ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (armed) "Shield Active" else "Shield Off",
                                        color = AegisTextPrimary,
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (armed) "On-device visual guard running" else "Ready to protect screen",
                                        color = if (armed) AegisPrimaryCyan else AegisTextSecondary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = ClassicalBodyFont,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // ─── PURE ANIMATED VIP CAPSULE ACTION BUTTON (No Switch UI) ───
                            val capsuleShape = RoundedCornerShape(50)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .graphicsLayer {
                                        scaleX = buttonBounceScale.value
                                        scaleY = buttonBounceScale.value
                                    }
                                    .shadow(
                                        elevation = if (isOn) 14.dp else 9.dp,
                                        shape = capsuleShape,
                                        ambientColor = buttonGradStart.copy(alpha = 0.36f),
                                        spotColor = buttonGradEnd.copy(alpha = 0.36f)
                                    )
                                    .clip(capsuleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(buttonGradStart, buttonGradEnd)
                                        )
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.80f),
                                                Color.White.copy(alpha = 0.25f)
                                            )
                                        ),
                                        shape = capsuleShape
                                    )
                                    .clickable { requestShieldToggle(!isOn) }
                                    .testTag("master_protection_switch"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Smooth glass shimmer wave gliding across the animated VIP button
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sheenCenterX = size.width * buttonSheenSlide
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.24f),
                                                Color.Transparent
                                            ),
                                            start = Offset(sheenCenterX - size.width * 0.22f, 0f),
                                            end = Offset(sheenCenterX + size.width * 0.22f, size.height)
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Glowing orb power emblem
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer {
                                                scaleX = powerPulseScale
                                                scaleY = powerPulseScale
                                            }
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = if (isOn) "Pause screen shield" else "Activate screen shield",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier
                                                .size(21.dp)
                                                .graphicsLayer {
                                                    rotationZ = powerIconRotation
                                                }
                                        )
                                    }

                                    AnimatedContent(
                                        targetState = isOn,
                                        transitionSpec = {
                                            (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.94f))
                                                .togetherWith(fadeOut(tween(160)))
                                        },
                                        label = "PureVipButtonText"
                                    ) { armed ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (armed) "SHIELD ACTIVE" else "PROTECT SCREEN",
                                                color = AegisOnPrimaryWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.0.sp,
                                                fontFamily = ClassicalDisplayFont
                                            )
                                            Text(
                                                text = if (armed) "Tap to pause guard" else "Tap to start protection",
                                                color = AegisOnPrimaryWhite.copy(alpha = 0.86f),
                                                fontSize = 10.5.sp,
                                                fontFamily = ClassicalBodyFont
                                            )
                                        }
                                    }

                                    // Animated live pulse wave bars (no switch look!)
                                    Row(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .width(26.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val h1 = if (isOn) 10.dp + (10.dp * eqWavePhase) else 8.dp + (4.dp * eqWavePhase)
                                        val h2 = if (isOn) 20.dp - (9.dp * eqWavePhase) else 13.dp - (4.dp * eqWavePhase)
                                        val h3 = if (isOn) 12.dp + (8.dp * eqWavePhase) else 9.dp + (3.dp * eqWavePhase)
                                        listOf(h1, h2, h3).forEach { barH ->
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(barH)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.White.copy(alpha = if (isOn) 0.95f else 0.78f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Scheduled Guard Window Reminder
                item {
                    AnimatedVisibility(
                        visible = isWithinScheduledWindow && !isOn,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CyberCard(
                            borderColor = AegisLavenderBorder,
                            tone = CyberCardTone.LAVENDER,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .testTag("scheduled_guard_banner")
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

                // 3. First-Time Model Setup Card
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
                            },
                            modifier = Modifier.widthIn(max = 560.dp)
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
