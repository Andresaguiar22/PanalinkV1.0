package com.example.creative.crop

/**
 * Aspect-ratio presets supported by the creative crop UI.
 *
 * Ratios are expressed as width / height and are intentionally independent
 * from any specific screen so Reels and other creative surfaces can reuse them.
 */
enum class CropAspectRatio(
    val label: String,
    val width: Float?,
    val height: Float?
) {
    FREE("Libre", null, null),
    PORTRAIT_9_16("9:16", 9f, 16f),
    SQUARE_1_1("1:1", 1f, 1f),
    PORTRAIT_4_5("4:5", 4f, 5f);

    val value: Float?
        get() = if (width != null && height != null) width / height else null
}
