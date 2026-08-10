package com.example.creative.templates

/**
 * P6.6.4.1 - Template Style & Presets Engine
 * Typed design presets for typography, color palettes, animations, filters, and asset references.
 */

enum class TemplateCategory(val displayName: String) {
    SOCIAL_MEDIA("Redes Sociales"),
    BUSINESS("Negocios y Ventas"),
    GAMING("Gaming & Streaming"),
    SPORTS("Deportes y Fitness"),
    INFLUENCER("Influencer & Lifestyle"),
    EDUCATION("Educación y Tips"),
    PERSONAL_BRAND("Marca Personal"),
    TRAVEL("Viajes y Aventuras"),
    BIRTHDAY("Eventos y Fiestas"),
    NEWS("Noticias y Tendencias")
}

enum class FilterPreset(val filterName: String) {
    NORMAL("Normal"),
    VIVID("Vivid"),
    MONO("Mono"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    CYBERPUNK("Cyberpunk"),
    OCEAN_CINEMATIC("Ocean Cinematic"),
    NEON_GLOW("Neon Glow")
}

enum class CreativeAssetType {
    STICKER,
    OVERLAY,
    BACKGROUND,
    BADGE
}

data class CreativeAsset(
    val id: String,
    val type: CreativeAssetType,
    val assetUriOrPath: String
)

data class TypographyPreset(
    val titleFontFamily: String = "SansSerif",
    val titleFontSizeSp: Float = 28f,
    val subtitleFontFamily: String = "Serif",
    val subtitleFontSizeSp: Float = 16f,
    val isBoldTitle: Boolean = true,
    val isItalicSubtitle: Boolean = false
)

data class ColorPalette(
    val primaryHex: String = "#38BDF8",
    val secondaryHex: String = "#A855F7",
    val backgroundHex: String = "#0F172A",
    val textPrimaryHex: String = "#FFFFFF",
    val textSecondaryHex: String = "#94A3B8",
    val accentHex: String = "#F59E0B"
)

data class AnimationPreset(
    val entryAnimation: String = "FADE_IN",
    val durationMs: Long = 800L,
    val easing: String = "EASE_IN_OUT",
    val parallaxEnabled: Boolean = false
)

data class LayoutPreset(
    val aspectRatio: String = "4:5", // "1:1", "4:5", "16:9"
    val contentPaddingDp: Float = 16f,
    val titleYFraction: Float = 0.2f,
    val subtitleYFraction: Float = 0.8f,
    val alignment: String = "CENTER"
)

data class TemplateStyle(
    val name: String,
    val category: TemplateCategory,
    val typography: TypographyPreset = TypographyPreset(),
    val colors: ColorPalette = ColorPalette(),
    val animation: AnimationPreset = AnimationPreset(),
    val layout: LayoutPreset = LayoutPreset(),
    val filter: FilterPreset = FilterPreset.NORMAL
)
