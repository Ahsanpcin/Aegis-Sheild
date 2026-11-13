package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AegisColorScheme = lightColorScheme(
    primary = AegisPrimaryCyan,
    onPrimary = AegisOnPrimaryWhite,
    primaryContainer = AegisSecondaryBackground,
    onPrimaryContainer = AegisPrimaryCyan,
    secondary = AegisElectricBlue,
    onSecondary = AegisOnPrimaryWhite,
    secondaryContainer = AegisSurfaceElevated,
    onSecondaryContainer = AegisTextPrimary,
    tertiary = AegisPurple,
    onTertiary = AegisOnPrimaryWhite,
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

@Composable
fun AegisShieldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AegisColorScheme,
        typography = Typography,
        content = content
    )
}
