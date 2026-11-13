package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isOnboardingCompleted: Boolean,
    onNavigateNext: (destination: String) -> Unit
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }
    var initStatus by remember { mutableStateOf("Initializing Sheildlenz Security Core...") }
    var progress by remember { mutableStateOf(0.18f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550)
        )

        delay(350)
        initStatus = "Verifying On-Device AI Pipeline..."
        progress = 0.55f

        delay(350)
        initStatus = "Auditing Screen Protection Policy..."
        progress = 0.88f

        delay(350)
        initStatus = "Sheildlenz Core Armed • Ready"
        progress = 1.0f

        delay(320)
        if (isOnboardingCompleted) {
            onNavigateNext("dashboard")
        } else {
            onNavigateNext("onboarding")
        }
    }

    SheildlenzBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen_root")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value),
                contentAlignment = Alignment.Center
            ) {
                AegisShieldGraphic(
                    isProtected = true,
                    isThreatActive = false,
                    size = 205.dp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SHEILDLENZ",
                color = AegisTextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.5.sp,
                fontFamily = ClassicalDisplayFont
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ornamental Champagne Gold Rule
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, AegisChampagneGold, Color.Transparent)
                        )
                    )
            )

            Text(
                text = "PRIVATE ON-DEVICE VISION GUARDIAN",
                color = AegisPrimaryCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AegisElectricBlue,
                trackColor = AegisPrimaryCyan.copy(alpha = 0.14f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = initStatus,
                color = AegisTextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            AhsanTcrBrandingPill()
        }
    }
}
