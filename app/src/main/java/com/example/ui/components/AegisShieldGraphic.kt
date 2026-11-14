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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisCyanGlow
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisRedGlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure VIP layered crystal shield crest with continuous smooth levitation,
 * rotating aurora sweep halo, orbiting constellation nodes, specular glass sheen,
 * and spring-animated shockwave activation.
 */
@Composable
fun AegisShieldGraphic(
    isProtected: Boolean,
    isThreatActive: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VipCrystalShieldMotion")

    // Smooth continuous aurora sweep rotation (360 deg in 5.2s)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraSweepAngle"
    )

    // Counter-rotating inner orbital phase
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitConstellationAngle"
    )

    // Smooth breathing levitation & glow pulse
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathPhase"
    )

    // Specular glass sheen glide across crystal face
    val glassSheen by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlassSheenGlide"
    )

    val activationProgress by animateFloatAsState(
        targetValue = if (isProtected || isThreatActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
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
                crestScale.snapTo(if (isProtected) 0.85f else 1.07f)
                crestScale.animateTo(
                    targetValue = if (isProtected) 1.11f else 0.93f,
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
                    animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing)
                )
                waveDirection = 0
            }
        }
    }

    val shieldPrimary by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisPrimaryCyan
            else -> Color(0xFF6B86C4)
        },
        animationSpec = tween(460),
        label = "ShieldPrimary"
    )

    val shieldAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisChampagneGold
            isProtected -> AegisElectricBlue
            else -> AegisLavenderLight
        },
        animationSpec = tween(460),
        label = "ShieldAccent"
    )

    val haloAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisChampagneGold
            isProtected -> AegisElectricBlue
            else -> AegisPurple
        },
        animationSpec = tween(460),
        label = "HaloAccent"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisRedGlow
            isProtected -> AegisCyanGlow
            else -> Color(0x2E7C5CFA)
        },
        animationSpec = tween(460),
        label = "ShieldGlow"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val floatDp = ((breathPhase - 0.5f) * 6f).dp.toPx()
                translationY = floatDp
                scaleX = crestScale.value
                scaleY = crestScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w * 0.5f, h * 0.50f)

            // 1. Living breathing aura behind shield
            val auraRadius = w * (0.45f + 0.08f * breathPhase + 0.10f * activationProgress)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = (0.30f + 0.42f * activationProgress) * (0.75f + 0.25f * breathPhase)),
                        haloAccent.copy(alpha = (0.14f + 0.14f * activationProgress) * breathPhase),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = center
            )

            // 2. Toggle activation burst ripple
            if (waveDirection != 0 && waveProgress.value in 0.01f..0.99f) {
                val p = waveProgress.value
                val waveRadius = if (waveDirection > 0) w * (0.20f + 0.36f * p) else w * (0.48f - 0.28f * p)
                val waveAlpha = (1f - p) * 0.85f
                val waveColor = if (waveDirection > 0) AegisElectricBlue else AegisPurple
                drawCircle(
                    color = waveColor.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 3.2.dp.toPx() * (1f - p * 0.45f))
                )
            }

            // 3. Smooth luminous aurora sweep-gradient orbital halo
            val ringRadius = w * 0.43f
            val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
            rotate(degrees = sweepAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            haloAccent.copy(alpha = 0.22f + 0.30f * activationProgress),
                            shieldPrimary.copy(alpha = 0.65f + 0.30f * activationProgress),
                            Color.White.copy(alpha = 0.90f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = (2.4f + 0.8f * activationProgress).dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Counter-rotating orbiting pearl nodes on outer ring
            val orbCount = 3
            for (i in 0 until orbCount) {
                val theta = ((orbitAngle + i * (360f / orbCount)) * PI / 180.0)
                val ox = center.x + (ringRadius * cos(theta)).toFloat()
                val oy = center.y + (ringRadius * sin(theta)).toFloat()
                val orbColor = if (i % 2 == 0) shieldPrimary else haloAccent
                drawCircle(
                    color = orbColor.copy(alpha = 0.28f + 0.25f * breathPhase),
                    radius = 5.5.dp.toPx(),
                    center = Offset(ox, oy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.8.dp.toPx(),
                    center = Offset(ox, oy)
                )
            }

            // 5. Outer sculpted crystal shield geometry
            val shieldPath = Path().apply {
                val cx = center.x
                val cy = center.y
                val sw = w * 0.34f
                val sh = h * 0.37f
                moveTo(cx, cy - sh * 1.06f)
                cubicTo(cx + sw * 0.95f, cy - sh * 0.78f, cx + sw * 0.96f, cy - sh * 0.14f, cx + sw * 0.84f, cy + sh * 0.44f)
                cubicTo(cx + sw * 0.64f, cy + sh * 0.88f, cx, cy + sh * 1.08f, cx, cy + sh * 1.08f)
                cubicTo(cx, cy + sh * 1.08f, cx - sw * 0.64f, cy + sh * 0.88f, cx - sw * 0.84f, cy + sh * 0.44f)
                cubicTo(cx - sw * 0.96f, cy - sh * 0.14f, cx - sw * 0.95f, cy - sh * 0.78f, cx, cy - sh * 1.06f)
                close()
            }

            // Layered frosted crystal glass fill + diagonal specular sheen sweep
            clipPath(shieldPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isProtected) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFDCEBFF), Color(0xFFE7D8FF))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFEAF1FF), Color(0xFFF0E8FF))
                        }
                    )
                )

                // Subtle dual-facet depth on right half
                drawRect(
                    color = haloAccent.copy(alpha = 0.06f + 0.06f * activationProgress),
                    topLeft = Offset(center.x, 0f),
                    size = Size(w * 0.5f, h)
                )

                // Moving glass specular gleam
                val sheenX = w * glassSheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        start = Offset(sheenX - w * 0.24f, 0f),
                        end = Offset(sheenX + w * 0.24f, h)
                    )
                )
            }

            // Inner white specular rim
            val innerShieldPath = Path().apply {
                val cx = center.x
                val cy = center.y
                val sw = w * 0.28f
                val sh = h * 0.305f
                moveTo(cx, cy - sh * 1.05f)
                cubicTo(cx + sw * 0.90f, cy - sh * 0.76f, cx + sw * 0.92f, cy - sh * 0.12f, cx + sw * 0.80f, cy + sh * 0.40f)
                cubicTo(cx + sw * 0.60f, cy + sh * 0.84f, cx, cy + sh * 1.05f, cx, cy + sh * 1.05f)
                cubicTo(cx, cy + sh * 1.05f, cx - sw * 0.60f, cy + sh * 0.84f, cx - sw * 0.80f, cy + sh * 0.40f)
                cubicTo(cx - sw * 0.92f, cy - sh * 0.12f, cx - sw * 0.90f, cy - sh * 0.76f, cx, cy - sh * 1.05f)
                close()
            }
            drawPath(
                path = innerShieldPath,
                color = Color.White.copy(alpha = 0.92f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Crisp luminous sculpted outer shield rim
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(shieldPrimary, shieldAccent, haloAccent),
                    start = Offset(center.x - w * 0.3f, center.y - h * 0.4f),
                    end = Offset(center.x + w * 0.3f, center.y + h * 0.4f)
                ),
                style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 6. Center floating medallion orb with breathing halo
            val coreRadius = w * (0.155f + 0.012f * breathPhase)
            drawCircle(
                color = shieldPrimary.copy(alpha = 0.20f + 0.18f * activationProgress),
                radius = coreRadius * 1.24f,
                center = center
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isProtected) {
                        listOf(AegisElectricBlue, AegisPrimaryCyan, AegisPurple)
                    } else {
                        listOf(Color(0xFF93ACE6), Color(0xFF8C77F5))
                    },
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                ),
                radius = coreRadius,
                center = center
            )

            // Crisp inner emblem (Checkmark when active, clean protective shield-check when standby)
            val checkPath = Path().apply {
                moveTo(center.x - coreRadius * 0.38f, center.y + coreRadius * 0.04f)
                lineTo(center.x - coreRadius * 0.08f, center.y + coreRadius * 0.34f)
                lineTo(center.x + coreRadius * 0.42f, center.y - coreRadius * 0.32f)
            }
            drawPath(
                path = checkPath,
                color = Color.White,
                style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
