package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Sheildlenz — Strict 3-Color Luminous Light Palette
// 1. Pearl Cloud White     (#FFFFFF / #F7FAFF / #EEF4FF)
// 2. Cerulean Sky Blue     (#2B6CE6 / #5A93FA / #1E4494 blue ink)
// 3. Luminous Champagne Gold (#D99B26 / #FFF8E7)
// Strictly light & airy — zero green, zero red, zero black, zero dark colors.
// ============================================================================

// Color 1: Pearl Cloud White (Airy Canvas & Elevated Cards)
val AegisVoidBackground = Color(0xFFF7FAFF)       // Airy Pearl-Sky Canvas
val AegisSecondaryBackground = Color(0xFFEEF4FF)  // Soft Cloud Sky Tint
val AegisCardBackground = Color(0xFFFFFFFF)       // Pure Pearl Card Surface
val AegisCardBorder = Color(0xFFD6E4FA)           // Delicate Sky Hairline Border
val AegisSurfaceElevated = Color(0xFFF3F8FF)      // Luminous Top-lit Mist
val AegisOnPrimaryWhite = Color(0xFFFFFFFF)       // Crisp Pearl White

// Color 2: Cerulean Sky Blue (Primary Controls, Active Shield, Healthy Status & Blue Ink)
val AegisPrimaryCyan = Color(0xFF2B6CE6)          // Vibrant Cerulean Sky Blue
val AegisElectricBlue = Color(0xFF5A93FA)         // Luminous Sky Azure
val AegisPurple = Color(0xFF5A93FA)               // Unified Sky Azure
val AegisOpticalTeal = Color(0xFF5A93FA)          // Unified Sky Azure (zero green)
val AegisSuccessGreen = Color(0xFF2B6CE6)         // Unified Cerulean Sky Blue (zero green)
val AegisMintSoft = Color(0xFFEEF4FF)             // Unified Soft Cloud Sky (zero green)

// Readable Blue Ink (strictly within the Cerulean Sky Blue family — zero black)
val AegisTextPrimary = Color(0xFF1E4494)          // Crisp Royal Blue Ink
val AegisTextSecondary = Color(0xFF5272B3)        // Soft Cobalt Sky Ink
val AegisTextMuted = Color(0xFF728FC7)            // Airy Sky Mist Ink

// Color 3: Luminous Champagne Gold (Secondary Accent, Warnings & Alerts — zero dark brown/red)
val AegisChampagneGold = Color(0xFFD99B26)        // Luminous Champagne Gold
val AegisChampagneSoft = Color(0xFFFFF8E7)        // Soft Sunlit Ivory Gold
val AegisWarningAmber = Color(0xFFD99B26)         // Unified Luminous Champagne Gold
val AegisDangerRed = Color(0xFFD99B26)            // Unified Luminous Champagne Gold (zero red/brown)

// Soft Luminous Halos (strictly Sky Blue & Champagne Gold)
val AegisCyanGlow = Color(0x282B6CE6)             // Soft Sky Blue Aura
val AegisGoldGlow = Color(0x26D99B26)             // Soft Champagne Gold Aura
val AegisRedGlow = Color(0x28D99B26)              // Soft Champagne Gold Aura
val AegisGreenGlow = Color(0x285A93FA)            // Soft Sky Azure Aura
