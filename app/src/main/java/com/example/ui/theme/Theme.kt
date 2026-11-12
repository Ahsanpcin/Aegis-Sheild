package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AegisColorScheme = darkColorScheme(
    primary = AegisPrimaryCyan,
    onPrimary = AegisVoidBackground,
    primaryContainer = AegisCardBackground,
    onPrimaryContainer = AegisPrimaryCyan,
    secondary = AegisElectricBlue,
    onSecondary = AegisTextPrimary,
    secondaryContainer = AegisSecondaryBackground,
    onSecondaryContainer = AegisPrimaryCyan,
    tertiary = AegisPurple,
    onTertiary = AegisTextPrimary,
    background = AegisVoidBackground,
    onBackground = AegisTextPrimary,
    surface = AegisSecondaryBackground,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisCardBackground,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisCardBorder,
    error = AegisDangerRed,
    onError = AegisTextPrimary
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
