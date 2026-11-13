package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisCyanGlow
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisOpticalTeal
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisRedGlow
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import kotlinx.coroutines.launch

@Composable
fun AegisShieldGraphic(
    isProtected: Boolean,
    isThreatActive: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SheildlenzCrestMotion")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val apertureAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ApertureRotation"
    )

    val activationProgress by animateFloatAsState(
        targetValue = if (isProtected || isThreatActive) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "ActivationProgress"
    )

    val crestScale = remember { Animatable(1f) }
    val waveProgress = remember { Animatable(0f) }
    var lastProtectedState by remember { mutableStateOf<Boolean?>(null) }
    var waveDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(isProtected) {
        val prev = lastProtectedState
        lastProtectedState = isProtected
        if (prev != null && prev != isProtected) {
            waveDirection = if (isProtected) 1 else -1
            launch {
                crestScale.snapTo(if (isProtected) 0.88f else 1.06f)
                crestScale.animateTo(
                    targetValue = if (isProtected) 1.08f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                crestScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                waveProgress.snapTo(0f)
                waveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
                )
                waveDirection = 0
            }
        }
    }

    val primaryColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisPrimaryCyan
            else -> AegisTextMuted
        },
        animationSpec = tween(480),
        label = "ShieldPrimaryColor"
    )

    val rimSecondaryColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed.copy(alpha = 0.72f)
            isProtected -> AegisElectricBlue
            else -> AegisTextMuted.copy(alpha = 0.65f)
        },
        animationSpec = tween(480),
        label = "ShieldRimSecondaryColor"
    )

    val lensAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisOpticalTeal
            else -> AegisTextMuted.copy(alpha = 0.6f)
        },
        animationSpec = tween(480),
        label = "LensAccentColor"
    )

    val coreDotColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisSuccessGreen
            else -> AegisTextMuted
        },
        animationSpec = tween(480),
        label = "CoreDotColor"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisRedGlow
            isProtected -> AegisCyanGlow
            else -> Color.Transparent
        },
        animationSpec = tween(480),
        label = "ShieldGlowColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = crestScale.value
                scaleY = crestScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val center = Offset(width * 0.5f, height * 0.49f)

            // Smooth radial sanctuary halo on ON / Threat transitions
            if (activationProgress > 0.01f && glowColor.alpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowColor.alpha * 1.4f * pulseAlpha * activationProgress),
                            AegisChampagneGold.copy(alpha = 0.07f * pulseAlpha * activationProgress),
                            Color.Transparent
                        ),
                        center = center,
                        radius = width * (0.42f + 0.14f * activationProgress)
                    ),
                    radius = width * (0.42f + 0.14f * activationProgress),
                    center = center
                )
            }

            // ON / OFF transition shockwave or power-down contraction ring
            if (waveDirection != 0 && waveProgress.value in 0.01f..0.99f) {
                val p = waveProgress.value
                val waveRadius = if (waveDirection > 0) {
                    width * (0.20f + 0.36f * p)
                } else {
                    width * (0.50f - 0.30f * p)
                }
                val waveAlpha = (1f - p) * 0.65f
                val waveColor = if (waveDirection > 0) AegisSuccessGreen else AegisTextMuted
                drawCircle(
                    color = waveColor.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = (3.dp.toPx() * (1f - p * 0.5f)))
                )
            }

            // Soft concentric orbital hairline ring
            drawCircle(
                color = (if (isThreatActive) AegisDangerRed else AegisChampagneGold)
                    .copy(alpha = 0.12f + (0.16f * pulseAlpha * activationProgress)),
                radius = width * (0.45f + 0.03f * activationProgress),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Heraldic Sheildlenz Crest Geometry
            val shieldPath = Path().apply {
                moveTo(width * 0.5f, height * 0.08f)
                cubicTo(
                    width * 0.72f, height * 0.08f,
                    width * 0.87f, height * 0.15f,
                    width * 0.89f, height * 0.30f
                )
                cubicTo(
                    width * 0.89f, height * 0.62f,
                    width * 0.68f, height * 0.82f,
                    width * 0.5f, height * 0.94f
                )
                cubicTo(
                    width * 0.32f, height * 0.82f,
                    width * 0.11f, height * 0.62f,
                    width * 0.11f, height * 0.30f
                )
                cubicTo(
                    width * 0.13f, height * 0.15f,
                    width * 0.28f, height * 0.08f,
                    width * 0.5f, height * 0.08f
                )
                close()
            }

            // Luminous Pearl-Porcelain Shield Interior
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        AegisSecondaryBackground
                    )
                )
            )

            // Inner Champagne-Gold Bevel Crest
            val innerPath = Path().apply {
                moveTo(width * 0.5f, height * 0.14f)
                cubicTo(
                    width * 0.69f, height * 0.14f,
                    width * 0.81f, height * 0.20f,
                    width * 0.83f, height * 0.32f
                )
                cubicTo(
                    width * 0.83f, height * 0.59f,
                    width * 0.65f, height * 0.77f,
                    width * 0.5f, height * 0.88f
                )
                cubicTo(
                    width * 0.35f, height * 0.77f,
                    width * 0.17f, height * 0.59f,
                    width * 0.17f, height * 0.32f
                )
                cubicTo(
                    width * 0.19f, height * 0.20f,
                    width * 0.31f, height * 0.14f,
                    width * 0.5f, height * 0.14f
                )
                close()
            }

            drawPath(
                path = innerPath,
                color = if (isThreatActive) AegisDangerRed.copy(alpha = 0.50f)
                else AegisChampagneGold.copy(alpha = 0.35f + 0.40f * activationProgress),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Outer Bold Heraldic Rim
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        rimSecondaryColor
                    )
                ),
                style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Center Optical Lens Housing (Sheildlenz signature motif)
            val lensCenter = Offset(width * 0.5f, height * 0.47f)
            val outerLensRadius = width * 0.21f
            val innerLensRadius = width * (0.115f + 0.015f * activationProgress)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isThreatActive) Color(0xFF4A0A14) else Color(0xFF0E234E),
                        if (isThreatActive) AegisDangerRed.copy(alpha = 0.8f) else primaryColor
                    ),
                    center = lensCenter,
                    radius = outerLensRadius
                ),
                radius = outerLensRadius,
                center = lensCenter
            )

            // Rotating segmented optical iris arcs expanding & spinning when ON
            val arcTopLeft = Offset(lensCenter.x - outerLensRadius * 0.78f, lensCenter.y - outerLensRadius * 0.78f)
            val arcSize = Size(outerLensRadius * 1.56f, outerLensRadius * 1.56f)
            rotate(degrees = apertureAngle * activationProgress, pivot = lensCenter) {
                for (i in 0 until 4) {
                    drawArc(
                        color = lensAccent.copy(alpha = 0.45f + 0.42f * activationProgress),
                        startAngle = i * 90f + 10f,
                        sweepAngle = 32f + 26f * activationProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Inner glowing cyan/sapphire glass core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        lensAccent.copy(alpha = 0.85f + 0.10f * activationProgress),
                        primaryColor
                    ),
                    center = Offset(lensCenter.x - innerLensRadius * 0.28f, lensCenter.y - innerLensRadius * 0.28f),
                    radius = innerLensRadius * 1.3f
                ),
                radius = innerLensRadius,
                center = lensCenter
            )

            // Specular crystal highlight & status pupil
            drawCircle(
                color = coreDotColor,
                radius = innerLensRadius * (0.34f + 0.06f * activationProgress),
                center = lensCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = innerLensRadius * 0.22f,
                center = Offset(lensCenter.x - innerLensRadius * 0.35f, lensCenter.y - innerLensRadius * 0.35f)
            )
        }
    }
}
