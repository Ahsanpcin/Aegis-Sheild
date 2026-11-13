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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.settings.AdvancedPrecisionLabSection
import com.example.ui.settings.AppVersionSecretCard
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

private data class FaqEntry(
    val question: String,
    val answer: String
)

private val SheildlenzFaqs = listOf(
    FaqEntry(
        question = "How do I load or replace the AI model?",
        answer = "Load a 2, 3, or 5-class .tflite file on Home (first time) or swap it anytime under Edits."
    ),
    FaqEntry(
        question = "Is screen data uploaded anywhere?",
        answer = "No. Detection runs 100% offline in volatile RAM with zero cloud upload."
    ),
    FaqEntry(
        question = "How do Master Code and Double-Time Wait work?",
        answer = "After Stage 1 lockdown ends, enter your Master Code or choose Double-Time Wait. Shield stays ON after both."
    ),
    FaqEntry(
        question = "Why enable Device Admin & Accessibility?",
        answer = "They prevent unauthorized uninstall, time changes, or disabling protection without your PIN."
    ),
    FaqEntry(
        question = "How do I customize exact timers or thresholds?",
        answer = "Adjust presets in Edits, or tap the Version card 7 times to unlock Precision Lab."
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
                CyberTopBar(
                    title = "About",
                    subtitle = "Version, Developer & FAQs"
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Version Name Widget (with 7-Tap Precision Lab unlock)
                item {
                    LuxurySectionHeader(title = "Application Version")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppVersionSecretCard(preferences = preferences)
                }

                // Precision Lab expands directly below when 7-tap unlocked
                item {
                    AdvancedPrecisionLabSection(preferences = preferences)
                }

                // 2. Developer & Sponsoring Information Widget
                item {
                    LuxurySectionHeader(title = "Developer", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberCard(borderColor = AegisChampagneGold.copy(alpha = 0.45f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AegisPrimaryCyan.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AegisPrimaryCyan,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AhsanTCr",
                                    color = AegisTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Text(
                                    text = "Creator of Sheildlenz • 100% Offline Guard",
                                    color = AegisChampagneGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AhsanTcrBrandingPill()
                        }
                    }
                }

                // 3. FAQs & Q/Answers Widget
                item {
                    LuxurySectionHeader(title = "FAQs & Q/A")
                }

                itemsIndexed(SheildlenzFaqs) { index, faq ->
                    val isExpanded = expandedFaqIndex == index
                    CyberCard(
                        borderColor = if (isExpanded) AegisPrimaryCyan.copy(alpha = 0.45f) else AegisCardBorder,
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
                                    .heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AegisSurfaceElevated)
                                            .border(1.dp, AegisCardBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                            contentDescription = null,
                                            tint = AegisPrimaryCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Q: ${faq.question}",
                                        color = AegisTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        lineHeight = 18.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse answer" else "Expand answer",
                                    tint = AegisPrimaryCyan
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.55f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "A: ${faq.answer}",
                                        color = AegisTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
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
