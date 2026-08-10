package com.example.creative.templates

import com.example.creative.core.CreativeLayer

data class CaptionStylePreset(
    val id: String,
    val name: String,
    val fontFamily: String,
    val fontSizeSp: Float,
    val colorHex: String,
    val isUppercase: Boolean = false,
    val lineSpacingMultiplier: Float = 1.0f
)

/**
 * P6.6.4 - Auto Caption Designer Engine
 * Generates styled visual text typography presets (e.g. Cinematic, Premium, Neon).
 */
object CaptionDesignerEngine {

    val captionPresets = listOf(
        CaptionStylePreset(
            id = "cinematic_bold",
            name = "🌅 Cinemático",
            fontFamily = "Serif",
            fontSizeSp = 28f,
            colorHex = "#FFFFFF",
            isUppercase = true
        ),
        CaptionStylePreset(
            id = "premium_gold",
            name = "✨ Premium Gold",
            fontFamily = "SansSerif",
            fontSizeSp = 26f,
            colorHex = "#F59E0B",
            isUppercase = true
        ),
        CaptionStylePreset(
            id = "neon_cyber",
            name = "⚡ Neon Cyber",
            fontFamily = "Monospace",
            fontSizeSp = 30f,
            colorHex = "#00E5FF",
            isUppercase = false
        ),
        CaptionStylePreset(
            id = "minimal_clean",
            name = "🍃 Minimal Clean",
            fontFamily = "SansSerif",
            fontSizeSp = 22f,
            colorHex = "#E2E8F0",
            isUppercase = false
        )
    )

    fun applyPresetToTextLayer(
        textLayer: CreativeLayer.Text,
        preset: CaptionStylePreset
    ): CreativeLayer.Text {
        val formattedText = if (preset.isUppercase) textLayer.text.uppercase() else textLayer.text
        return textLayer.copy(
            text = formattedText,
            fontFamily = preset.fontFamily,
            fontSizeSp = preset.fontSizeSp,
            colorHex = preset.colorHex
        )
    }
}
