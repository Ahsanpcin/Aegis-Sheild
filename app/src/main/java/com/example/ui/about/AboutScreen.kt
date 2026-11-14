package com.example.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.core.update.ApkDownloadState
import com.example.core.update.UpdateChecker
import com.example.data.preferences.AegisPreferences
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.settings.AdvancedPrecisionLabSection
import com.example.ui.settings.AppVersionSecretCard
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

private data class FaqEntry(
    val question: String,
    val answer: String
)

private val SheildlenzFaqs = listOf(
    FaqEntry(
        question = "Does Sheildlenz ever record or save my screen?",
        answer = "Never. Each frame is scored in volatile RAM (~20ms) and discarded instantly—zero screenshots or videos are ever stored on disk."
    ),
    FaqEntry(
        question = "Is anything sent to cloud servers or tracked?",
        answer = "No. Inference runs 100% offline using your local .tflite file with zero analytics, ad SDKs, or telemetry. Internet only fetches app/model downloads you trigger."
    ),
    FaqEntry(
        question = "Why does it ask for Accessibility & Overlay?",
        answer = "Overlay draws the protective shield; Accessibility checks only the active app package name so Excluded Apps stay untouched. Typed passwords, chats, and text are never read."
    ),
    FaqEntry(
        question = "What data is stored in Progress history?",
        answer = "Only lightweight text logs (app name, time, category score) stay inside private on-device SQLite storage—no images—and can be cleared with one tap."
    ),
    FaqEntry(
        question = "How do Excluded Apps protect private apps?",
        answer = "Excluded package names stay strictly on your phone. Whenever you open an excluded app, screen capture and AI scanning pause immediately."
    ),
    FaqEntry(
        question = "Why can't Protection be stopped without my PIN?",
        answer = "Anti-Tamper and Master Code prevent impulsive bypasses. After Stage 1 lockdown ends, entering your PIN or choosing Double-Time Wait keeps Shield armed."
    ),
    FaqEntry(
        question = "Will continuous scanning drain my battery?",
        answer = "Compact 256px tensors, static frame skipping, Adaptive Eco backoff, and automatic screen-off sleep keep CPU and battery usage minimal."
    ),
    FaqEntry(
        question = "How do I change the model or exact timers?",
        answer = "Tap CUSTOM under Edits to load any 2, 3, or 5-class .tflite file, or tap the Version card above 7 times to unlock Precision Lab."
    )
)

@Composable
fun AboutScreen(
    preferences: AegisPreferences
) {
    var expandedFaqIndex by remember { mutableIntStateOf(0) }

    SheildlenzBackdrop(modifier = Modifier.testTag("about_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(title = "About")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Version Card (with 7-Tap Precision Lab unlock)
                item {
                    LuxurySectionHeader(title = "Version")
                    Spacer(modifier = Modifier.height(6.dp))
                    AppVersionSecretCard(preferences = preferences)
                }

                // Precision Lab expands directly below when 7-tap unlocked
                item {
                    AdvancedPrecisionLabSection(preferences = preferences)
                }

                // 2. Compact App Update Checker Widget
                item {
                    LuxurySectionHeader(title = "Updates", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    AppUpdateCheckerCard()
                }

                // 3. Compact Developer Widget
                item {
                    LuxurySectionHeader(title = "Developer", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = AegisGoldBorder,
                        tone = CyberCardTone.HERO
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(AegisChampagneGold, AegisPurple)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AegisOnPrimaryWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AhsanTCr",
                                    color = AegisTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Text(
                                    text = "Creator • 100% Offline Privacy Guard",
                                    color = AegisChampagneGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AhsanTcrBrandingPill()
                        }
                    }
                }

                // 4. Privacy & Logic FAQs
                item {
                    LuxurySectionHeader(title = "Privacy & Logic Q/A", accentColor = AegisPurple)
                }

                itemsIndexed(SheildlenzFaqs) { index, faq ->
                    val isExpanded = expandedFaqIndex == index
                    val faqTone = when {
                        index % 3 == 0 -> CyberCardTone.LAVENDER
                        index % 3 == 1 -> CyberCardTone.SKY
                        else -> CyberCardTone.GOLD
                    }
                    val faqAccent = when (faqTone) {
                        CyberCardTone.LAVENDER -> AegisPurple
                        CyberCardTone.GOLD -> AegisChampagneGold
                        else -> AegisPrimaryCyan
                    }
                    CyberCard(
                        borderColor = when (faqTone) {
                            CyberCardTone.LAVENDER -> AegisLavenderBorder
                            CyberCardTone.GOLD -> AegisGoldBorder
                            else -> AegisCardBorder
                        },
                        tone = faqTone,
                        modifier = Modifier.testTag("about_faq_item_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedFaqIndex = if (isExpanded) -1 else index
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(faqAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                            contentDescription = null,
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = faq.question,
                                        color = AegisTextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        lineHeight = 16.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse answer" else "Expand answer",
                                    tint = faqAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(
                                        color = faqAccent.copy(alpha = 0.28f),
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                    Text(
                                        text = faq.answer,
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateCheckerCard() {
    val context = LocalContext.current
    val isChecking by UpdateChecker.isChecking.collectAsState()
    val updateInfo by UpdateChecker.updateInfo.collectAsState()
    val showUpToDateDialog by UpdateChecker.showUpToDateDialog.collectAsState()
    val checkError by UpdateChecker.checkError.collectAsState()
    val apkDownloadState by UpdateChecker.apkDownloadState.collectAsState()

    val currentLabel = "v${UpdateChecker.cleanVersionTag(BuildConfig.VERSION_NAME)}"
    val latestLabel = updateInfo?.let { info ->
        "v${UpdateChecker.cleanVersionTag(info.latestVersion)}"
    } ?: "Checking..."
    val hasUpdate = updateInfo?.isUpdateAvailable == true
    val isReadyPreloaded = (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.preloadedReady == true
    val isBusyDownloading = apkDownloadState is ApkDownloadState.Downloading ||
        apkDownloadState is ApkDownloadState.Preloading ||
        apkDownloadState is ApkDownloadState.Verifying ||
        apkDownloadState is ApkDownloadState.Installing

    CyberCard(
        borderColor = when {
            isReadyPreloaded -> AegisLavenderBorder
            hasUpdate -> AegisGoldBorder
            else -> AegisLavenderBorder
        },
        tone = when {
            isReadyPreloaded -> CyberCardTone.LAVENDER
            hasUpdate -> CyberCardTone.GOLD
            else -> CyberCardTone.SKY
        },
        modifier = Modifier.testTag("app_update_checker_card")
    ) {
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
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (hasUpdate) listOf(AegisChampagneGold, AegisPurple) else listOf(AegisPrimaryCyan, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasUpdate) Icons.Default.Download else Icons.Default.Update,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (hasUpdate) "Update Ready ($latestLabel)" else "App Updates",
                            color = AegisTextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        if (isReadyPreloaded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AegisPurple.copy(alpha = 0.16f))
                                    .border(1.dp, AegisLavenderBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "0s",
                                    color = AegisPurple,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }
                    }
                    Text(
                        text = "$currentLabel → $latestLabel",
                        color = if (hasUpdate) AegisChampagneGold else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            if (hasUpdate && !isBusyDownloading) {
                IconButton(
                    onClick = { UpdateChecker.startCheckForUpdates(context, showUpToDatePopup = true) },
                    enabled = !isChecking
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-check for updates",
                        tint = AegisPurple
                    )
                }
            }
        }

        // Inline Preload / Download / Verification status
        when (val state = apkDownloadState) {
            is ApkDownloadState.Preloading -> {
                Spacer(modifier = Modifier.height(8.dp))
                val pct = state.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                Text(
                    text = "Preloading (100 KB/s) • $pct",
                    color = AegisChampagneGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress },
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
            is ApkDownloadState.PausedLowBattery -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paused (<= 20% batt) • ${UpdateChecker.formatBytesMb(state.bytesDownloaded)} saved",
                    color = AegisChampagneGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            is ApkDownloadState.Downloading -> {
                Spacer(modifier = Modifier.height(8.dp))
                val pct = state.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                Text(
                    text = "Downloading • $pct",
                    color = AegisPrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f)
                    )
                }
            }
            ApkDownloadState.Verifying -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verifying SHA-256...",
                    color = AegisPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AegisPurple,
                    trackColor = AegisPurple.copy(alpha = 0.2f)
                )
            }
            ApkDownloadState.Installing -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Installing update...",
                    color = AegisPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AegisPurple,
                    trackColor = AegisPurple.copy(alpha = 0.2f)
                )
            }
            is ApkDownloadState.ReadyToInstall -> {
                if (state.needsInstallPermission) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allow 'Install unknown apps' to auto-install.",
                        color = AegisChampagneGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            is ApkDownloadState.SignatureConflict -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.reason,
                    color = AegisChampagneGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            ApkDownloadState.Installed -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Up to date!",
                    color = AegisPrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
            is ApkDownloadState.Failed -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.message,
                    color = AegisWarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            ApkDownloadState.Idle -> {}
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            apkDownloadState is ApkDownloadState.Downloading -> {
                OutlinedButton(
                    onClick = { UpdateChecker.cancelApkDownload(context) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_update_download_button")
                ) {
                    Text("CANCEL DOWNLOAD", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Preloading -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("boost_preload_turbo_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FINISH NOW", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Verifying -> {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = AegisPurple.copy(alpha = 0.65f),
                        disabledContentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VERIFYING...", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Installing -> {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = AegisPurple.copy(alpha = 0.65f),
                        disabledContentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("INSTALLING...", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.needsInstallPermission == true -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_install_permission_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ALLOW & INSTALL", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.preloadedReady == true -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("install_ready_update_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("INSTALL NOW (0s)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            apkDownloadState is ApkDownloadState.SignatureConflict -> {
                Button(
                    onClick = { UpdateChecker.triggerCleanUninstallForUpgrade(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clean_upgrade_uninstall_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CLEAN UPGRADE", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            hasUpdate -> {
                val info = updateInfo!!
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_install_update_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val sizeHint = if (info.apkSizeBytes > 0L) " (${UpdateChecker.formatBytesMb(info.apkSizeBytes)})" else ""
                    Text("UPDATE TO $latestLabel$sizeHint", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
            else -> {
                Button(
                    onClick = { UpdateChecker.startCheckForUpdates(context, showUpToDatePopup = true) },
                    enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button")
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CHECKING...", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CHECK UPDATES", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }
        }

        if (checkError != null) {
            Text(
                text = checkError!!,
                color = AegisWarningAmber,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { UpdateChecker.dismissUpToDateDialog() },
            containerColor = AegisLavenderSoft,
            title = { Text("App is Up to Date", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "App: $currentLabel • Latest: $latestLabel\nYou are on the newest version.",
                    fontFamily = ClassicalBodyFont
                )
            },
            confirmButton = {
                Button(onClick = { UpdateChecker.dismissUpToDateDialog() }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
