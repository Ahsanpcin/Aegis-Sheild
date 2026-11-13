package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = AegisSuccessGreen,
    inactiveColor: Color = Color(0xFF64748B)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PillPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotPulse"
    )

    val color = if (isActive) activeColor else inactiveColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isActive) 0.22f * dotAlpha else 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) color.copy(alpha = dotAlpha) else color)
            )
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            fontFamily = ClassicalBodyFont
        )
    }
}

@Composable
fun ThreatCategoryBadge(
    category: ThreatCategory,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (category) {
        ThreatCategory.EXPLICIT_ADULT -> Pair(AegisDangerRed, "EXPLICIT ADULT")
        ThreatCategory.SUGGESTIVE_CONTENT -> Pair(AegisWarningAmber, "SUGGESTIVE")
        ThreatCategory.SENSITIVE_GRAPHIC -> Pair(AegisPurple, "GRAPHIC / VIOLENT")
        ThreatCategory.BENIGN_SAFE -> Pair(AegisSuccessGreen, "SAFE")
    }

    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = ClassicalBodyFont,
        letterSpacing = 0.7.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun SeverityTag(
    severity: ThreatSeverity,
    modifier: Modifier = Modifier
) {
    val color = when (severity) {
        ThreatSeverity.CRITICAL -> AegisDangerRed
        ThreatSeverity.HIGH -> Color(0xFFEA580C)
        ThreatSeverity.MEDIUM -> AegisWarningAmber
        ThreatSeverity.LOW -> AegisElectricBlue
    }

    Text(
        text = severity.name,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = ClassicalBodyFont,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.26f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
