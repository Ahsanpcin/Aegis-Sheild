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
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisOpticalTeal
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

/**
 * Atmospheric luxury silk-porcelain backdrop with delicate radial sapphire and gold halos.
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
                            Color(0xFFFFFFFF),
                            AegisVoidBackground,
                            Color(0xFFEEF4FF)
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisElectricBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.08f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.85f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisPrimaryCyan.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.36f),
                        radius = size.width * 0.60f
                    ),
                    radius = size.width * 0.60f,
                    center = Offset(size.width * 0.12f, size.height * 0.36f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AegisChampagneGold.copy(alpha = 0.09f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.88f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
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
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            borderColor
        )
    )
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = AegisElectricBlue.copy(alpha = 0.08f),
            spotColor = AegisPrimaryCyan.copy(alpha = 0.10f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(AegisCardBackground, AegisSurfaceElevated)
            )
        )
        .border(BorderStroke(1.dp, borderBrush), shape)
        .border(BorderStroke(0.5.dp, borderColor.copy(alpha = 0.7f)), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(18.dp)

    Column(modifier = cardModifier, content = content)
}

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
                .width(18.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = AegisTextSecondary,
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
    CyberCard(modifier = modifier, borderColor = accentColor.copy(alpha = 0.24f)) {
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
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.10f))
                    .border(1.dp, accentColor.copy(alpha = 0.20f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
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
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.25f))
                    )
                )
        )
    }
}
