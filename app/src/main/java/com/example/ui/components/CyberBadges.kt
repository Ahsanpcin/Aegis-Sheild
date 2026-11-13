package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = AegisSuccessGreen,
    inactiveColor: Color = AegisTextMuted
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

    val color by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 360),
        label = "StatusPillColor"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "StatusDotScale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.2.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                }
                .clip(CircleShape)
                .background(color.copy(alpha = if (isActive) 0.30f * dotAlpha else 0.18f)),
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
        AnimatedContent(
            targetState = label.uppercase(),
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { if (isActive) it / 2 else -it / 2 })
                    .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { if (isActive) -it / 2 else it / 2 })
            },
            label = "StatusPillText"
        ) { targetText ->
            Text(
                text = targetText,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@Composable
fun ThreatCategoryBadge(
    category: ThreatCategory,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (category) {
        ThreatCategory.EXPLICIT_ADULT -> Pair(AegisChampagneGold, "EXPLICIT ADULT")
        ThreatCategory.SUGGESTIVE_CONTENT -> Pair(com.example.ui.theme.AegisPurple, "SUGGESTIVE")
        ThreatCategory.SENSITIVE_GRAPHIC -> Pair(AegisPrimaryCyan, "GRAPHIC / VIOLENT")
        ThreatCategory.BENIGN_SAFE -> Pair(AegisElectricBlue, "SAFE")
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
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun SeverityTag(
    severity: ThreatSeverity,
    modifier: Modifier = Modifier
) {
    val color = when (severity) {
        ThreatSeverity.CRITICAL -> AegisChampagneGold
        ThreatSeverity.HIGH -> AegisWarningAmber
        ThreatSeverity.MEDIUM -> com.example.ui.theme.AegisPurple
        ThreatSeverity.LOW -> AegisPrimaryCyan
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
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
