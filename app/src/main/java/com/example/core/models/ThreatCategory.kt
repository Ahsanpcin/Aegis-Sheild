package com.example.core.models

enum class ThreatCategory(val displayName: String, val description: String) {
    EXPLICIT_ADULT("Explicit Adult Content", "Sexually explicit and adult visual material"),
    SUGGESTIVE_CONTENT("Suggestive Content", "Provocative or partially dressed imagery"),
    SENSITIVE_GRAPHIC("Sensitive / Graphic", "Violent or disturbing graphic scenes"),
    BENIGN_SAFE("Safe Content", "Clean and appropriate visual content")
}
