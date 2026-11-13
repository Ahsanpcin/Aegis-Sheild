package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AegisCardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 3.dp,
            shape = shape,
            ambientColor = AegisPrimaryCyan.copy(alpha = 0.08f),
            spotColor = AegisPrimaryCyan.copy(alpha = 0.12f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(AegisCardBackground, AegisSurfaceElevated)
            )
        )
        .border(BorderStroke(1.dp, borderColor), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(18.dp)

    Column(modifier = cardModifier, content = content)
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
    CyberCard(modifier = modifier, borderColor = accentColor.copy(alpha = 0.25f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = AegisTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.10f)),
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
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (caption != null) {
            Text(
                text = caption,
                color = AegisTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
