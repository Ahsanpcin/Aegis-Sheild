package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisLavenderSurface
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

enum class CyberCardTone {
    SKY,
    LAVENDER,
    GOLD,
    HERO
}

/**
 * Luminous 3-color gradient atmosphere blending Sky Azure, Orchid Lavender, and Sunlit Gold.
 */
@Composable
fun SheildlenzBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AegisVoidBackground)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD2E5FF), // Sky Azure Mist
                            Color(0xFFE1E8FF), // Periwinkle Blend
                            Color(0xFFECE2FF)  // Soft Orchid Lavender
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisElectricBlue.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.88f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisPurple.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.10f, size.height * 0.42f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.10f, size.height * 0.42f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisGoldBright.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.88f),
                        radius = size.width * 0.58f
                    ),
                    radius = size.width * 0.58f,
                    center = Offset(size.width * 0.78f, size.height * 0.88f)
                )
            },
        content = content
    )
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AegisCardBorder,
    tone: CyberCardTone? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedTone = tone ?: when {
        borderColor == AegisChampagneGold || borderColor.red > 0.75f && borderColor.green > 0.45f && borderColor.blue < 0.35f -> CyberCardTone.GOLD
        borderColor == AegisPurple || borderColor == AegisLavenderBorder || (borderColor.blue > 0.80f && borderColor.red > 0.42f && borderColor.green < 0.48f) -> CyberCardTone.LAVENDER
        else -> CyberCardTone.SKY
    }

    val (gradientColors, resolvedBorder, accentStripeColors, shadowTint) = when (resolvedTone) {
        CyberCardTone.SKY -> Quad(
            listOf(AegisCardBackground, AegisSkySoft),
            if (borderColor == AegisCardBorder) AegisCardBorder else borderColor,
            listOf(AegisPrimaryCyan, AegisElectricBlue),
            AegisPrimaryCyan.copy(alpha = 0.16f)
        )
        CyberCardTone.LAVENDER -> Quad(
            listOf(AegisLavenderSoft, AegisLavenderSurface),
            AegisLavenderBorder,
            listOf(AegisPurple, AegisLavenderLight),
            AegisPurple.copy(alpha = 0.16f)
        )
        CyberCardTone.GOLD -> Quad(
            listOf(AegisChampagneSoft, AegisGoldDeepTint),
            AegisGoldBorder,
            listOf(AegisChampagneGold, AegisGoldBright),
            AegisChampagneGold.copy(alpha = 0.18f)
        )
        CyberCardTone.HERO -> Quad(
            listOf(AegisCardBackground, AegisLavenderSoft, AegisChampagneSoft.copy(alpha = 0.85f)),
            AegisCardBorder,
            listOf(AegisPrimaryCyan, AegisPurple, AegisChampagneGold),
            AegisPurple.copy(alpha = 0.18f)
        )
    }

    val shape = RoundedCornerShape(22.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            resolvedBorder
        )
    )
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = shadowTint,
            spotColor = shadowTint
        )
        .clip(shape)
        .background(brush = Brush.verticalGradient(colors = gradientColors))
        .border(BorderStroke(1.2.dp, borderBrush), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = cardModifier) {
        // Vibrant 3-color style top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .background(brush = Brush.horizontalGradient(colors = accentStripeColors))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun LuxurySectionHeader(
    title: String,
    accentColor: Color = AegisChampagneGold,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, AegisPrimaryCyan)
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = AegisPrimaryCyan,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    val tileTone = when (accentColor) {
        AegisPurple, AegisLavenderLight -> CyberCardTone.LAVENDER
        AegisChampagneGold, AegisGoldBright -> CyberCardTone.GOLD
        else -> CyberCardTone.SKY
    }
    CyberCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.45f),
        tone = tileTone
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = AegisTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                letterSpacing = 0.9.sp
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.72f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AegisOnPrimaryWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (caption != null) {
            Text(
                text = caption,
                color = AegisTextMuted,
                fontSize = 12.sp,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, AegisChampagneGold)
                    )
                )
        )
    }
}
