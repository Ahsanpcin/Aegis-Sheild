package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SheildlenzColorScheme = lightColorScheme(
    primary = AegisPrimaryCyan,
    onPrimary = AegisOnPrimaryWhite,
    primaryContainer = AegisSecondaryBackground,
    onPrimaryContainer = AegisPrimaryCyan,
    secondary = AegisElectricBlue,
    onSecondary = AegisOnPrimaryWhite,
    secondaryContainer = AegisSurfaceElevated,
    onSecondaryContainer = AegisTextPrimary,
    tertiary = AegisChampagneGold,
    onTertiary = AegisOnPrimaryWhite,
    tertiaryContainer = AegisChampagneSoft,
    onTertiaryContainer = AegisChampagneGold,
    background = AegisVoidBackground,
    onBackground = AegisTextPrimary,
    surface = AegisCardBackground,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisSecondaryBackground,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisCardBorder,
    error = AegisDangerRed,
    onError = AegisOnPrimaryWhite
)

private val SheildlenzShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AegisShieldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SheildlenzColorScheme,
        typography = Typography,
        shapes = SheildlenzShapes,
        content = content
    )
}
