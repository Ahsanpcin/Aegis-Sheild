package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DailyThreatCount
import com.example.data.repository.ThreatAnalyticsSummary
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun SevenDayThreatChart(
    weeklyTrend: List<DailyThreatCount>,
    modifier: Modifier = Modifier
) {
    val maxCount = (weeklyTrend.maxOfOrNull { it.count } ?: 0).coerceAtLeast(4)

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = modifier.testTag("seven_day_threat_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisPurple, AegisPrimaryCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "7-Day Threat Activity",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "Daily blocked content events",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            val weekTotal = weeklyTrend.sumOf { it.count }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AegisPurple.copy(alpha = 0.12f))
                    .border(1.dp, AegisPurple.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$weekTotal This Week",
                    color = AegisPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Canvas Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                val availableWidth = size.width
                val availableHeight = size.height
                val barCount = weeklyTrend.size
                if (barCount == 0) return@Canvas

                val slotWidth = availableWidth / barCount
                val barWidth = (slotWidth * 0.46f).coerceIn(12f, 26f)

                // Background horizontal guide lines
                val gridLines = 3
                for (g in 0..gridLines) {
                    val y = availableHeight * (g.toFloat() / gridLines)
                    drawLine(
                        color = AegisCardBorder.copy(alpha = 0.25f),
                        start = Offset(0f, y),
                        end = Offset(availableWidth, y),
                        strokeWidth = 1f
                    )
                }

                // Render Bars
                weeklyTrend.forEachIndexed { index, day ->
                    val centerX = (index * slotWidth) + (slotWidth / 2f)
                    val normalizedHeight = (day.count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val barHeight = (normalizedHeight * (availableHeight - 12f)).coerceAtLeast(6f)
                    val topY = availableHeight - barHeight

                    val barBrush = if (day.count > 0) {
                        Brush.verticalGradient(
                            colors = if (day.isToday) {
                                listOf(AegisChampagneGold, AegisPurple)
                            } else {
                                listOf(AegisPrimaryCyan, AegisPurple)
                            }
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                AegisCardBorder.copy(alpha = 0.3f),
                                AegisCardBorder.copy(alpha = 0.15f)
                            )
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(centerX - (barWidth / 2f), topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            // Day Labels Row below Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 104.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeklyTrend.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${day.count}",
                            color = if (day.count > 0) AegisTextPrimary else AegisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (day.count > 0) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = day.dayLabel,
                            color = if (day.isToday) AegisPrimaryCyan else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatCategoryDistributionCard(
    analytics: ThreatAnalyticsSummary,
    modifier: Modifier = Modifier
) {
    val total = analytics.totalDetections
    val explicit = analytics.explicitAdultCount
    val suggestive = analytics.suggestiveCount
    val sensitive = analytics.sensitiveGraphicCount

    val explicitPercent = if (total > 0) (explicit * 100 / total) else 0
    val suggestivePercent = if (total > 0) (suggestive * 100 / total) else 0
    val sensitivePercent = if (total > 0) (sensitive * 100 / total) else 0

    CyberCard(
        borderColor = AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = modifier.testTag("threat_category_distribution_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisPrimaryCyan, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Threat Category Distribution",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "Classification breakdown of blocked material",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Segmented Meter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(AegisSurfaceElevated)
                .border(1.dp, AegisCardBorder.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
        ) {
            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(AegisCardBorder.copy(alpha = 0.25f))
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                    if (explicit > 0) {
                        Box(
                            modifier = Modifier
                                .weight(explicit.toFloat())
                                .height(14.dp)
                                .background(AegisDangerRed)
                        )
                    }
                    if (suggestive > 0) {
                        Box(
                            modifier = Modifier
                                .weight(suggestive.toFloat())
                                .height(14.dp)
                                .background(AegisPurple)
                        )
                    }
                    if (sensitive > 0) {
                        Box(
                            modifier = Modifier
                                .weight(sensitive.toFloat())
                                .height(14.dp)
                                .background(AegisPrimaryCyan)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CategoryLegendItem(
                color = AegisDangerRed,
                label = "Adult / Explicit",
                count = explicit,
                percent = explicitPercent
            )
            CategoryLegendItem(
                color = AegisPurple,
                label = "Suggestive",
                count = suggestive,
                percent = suggestivePercent
            )
            CategoryLegendItem(
                color = AegisPrimaryCyan,
                label = "Sensitive",
                count = sensitive,
                percent = sensitivePercent
            )
        }
    }
}

@Composable
private fun CategoryLegendItem(
    color: Color,
    label: String,
    count: Int,
    percent: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                color = AegisTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
            )
            Text(
                text = "$count ($percent%)",
                color = AegisTextMuted,
                fontSize = 10.sp,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@Composable
fun PeakThreatWindowCard(
    analytics: ThreatAnalyticsSummary,
    modifier: Modifier = Modifier
) {
    CyberCard(
        borderColor = AegisGoldBorder,
        tone = CyberCardTone.GOLD,
        modifier = modifier.testTag("peak_threat_window_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisChampagneGold, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Peak Threat Window",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = analytics.peakHourWindow,
                        color = AegisChampagneGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            if (!analytics.peakAppDisplayName.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AegisSurfaceElevated)
                        .border(1.dp, AegisGoldBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Top: ${analytics.peakAppDisplayName}",
                        color = AegisTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
