package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisCyanGlow
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisRedGlow
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted

@Composable
fun AegisShieldGraphic(
    isProtected: Boolean,
    isThreatActive: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val primaryColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisPrimaryCyan
            else -> AegisTextMuted
        },
        animationSpec = tween(500),
        label = "ShieldPrimaryColor"
    )

    val secondaryAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisSuccessGreen
            else -> AegisTextMuted
        },
        animationSpec = tween(500),
        label = "ShieldSecondaryColor"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisRedGlow
            isProtected -> AegisCyanGlow
            else -> Color.Transparent
        },
        animationSpec = tween(500),
        label = "ShieldGlowColor"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height

            if (isProtected || isThreatActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = glowColor.alpha * pulseAlpha), Color.Transparent),
                        center = Offset(width / 2f, height / 2f),
                        radius = width * 0.54f
                    ),
                    radius = width * 0.54f
                )
            }

            // Classical Heraldic Crest Geometry
            val path = Path().apply {
                moveTo(width * 0.5f, height * 0.08f)
                cubicTo(
                    width * 0.72f, height * 0.08f,
                    width * 0.88f, height * 0.16f,
                    width * 0.90f, height * 0.30f
                )
                cubicTo(
                    width * 0.90f, height * 0.62f,
                    width * 0.68f, height * 0.82f,
                    width * 0.5f, height * 0.94f
                )
                cubicTo(
                    width * 0.32f, height * 0.82f,
                    width * 0.10f, height * 0.62f,
                    width * 0.10f, height * 0.30f
                )
                cubicTo(
                    width * 0.12f, height * 0.16f,
                    width * 0.28f, height * 0.08f,
                    width * 0.5f, height * 0.08f
                )
                close()
            }

            // Luminous Porcelain Interior Gradient
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        AegisSecondaryBackground
                    )
                )
            )

            // Inner Classical Crest Filigree
            val innerPath = Path().apply {
                moveTo(width * 0.5f, height * 0.20f)
                lineTo(width * 0.78f, height * 0.36f)
                lineTo(width * 0.74f, height * 0.58f)
                lineTo(width * 0.5f, height * 0.82f)
                lineTo(width * 0.26f, height * 0.58f)
                lineTo(width * 0.22f, height * 0.36f)
                close()
            }

            drawPath(
                path = innerPath,
                color = primaryColor.copy(alpha = if (isProtected) 0.07f else 0.03f)
            )

            drawPath(
                path = innerPath,
                color = if (isProtected && !isThreatActive) AegisChampagneGold.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.45f),
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Classical Core Emblem
            drawCircle(
                color = secondaryAccent,
                radius = 7.dp.toPx(),
                center = Offset(width * 0.5f, height * 0.46f)
            )

            drawLine(
                color = primaryColor.copy(alpha = 0.85f),
                start = Offset(width * 0.5f, height * 0.46f),
                end = Offset(width * 0.5f, height * 0.72f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Outer Heraldic Border
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        if (isProtected) AegisElectricBlue else primaryColor.copy(alpha = 0.55f)
                    )
                ),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
