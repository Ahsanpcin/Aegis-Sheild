package com.example.core.models

enum class SensitivityLevel(val displayName: String, val threshold: Float, val detail: String) {
    STRICT("Strict (Max Security)", 0.60f, "Blocks even moderately suggestive and sensitive elements"),
    BALANCED("Balanced (Standard)", 0.80f, "Optimal balance between protection and false positives"),
    PERMISSIVE("Permissive (Explicit Only)", 0.92f, "Triggers solely on undeniable explicit adult content")
}
