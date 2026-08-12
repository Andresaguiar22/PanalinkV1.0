package com.example.ui.reels.editor.model

/** A non-destructive element placed on a Reel timeline. */
data class ReelLayer(
    val id: String,
    val type: ReelTrackType,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val zIndex: Int = 0,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val content: ReelLayerContent
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}

sealed class ReelLayerContent {
    data class Media(val uri: String, val mimeType: String? = null) : ReelLayerContent()
    data class Image(val uri: String) : ReelLayerContent()
    data class Text(
        val value: String,
        val fontFamily: String? = null,
        val fontSizeSp: Float = 32f,
        val colorArgb: Int = 0xFFFFFFFF.toInt(),
        val backgroundColorArgb: Int? = null
    ) : ReelLayerContent()
    data class Sticker(val uri: String) : ReelLayerContent()
    data class Audio(val uri: String, val volume: Float = 1f) : ReelLayerContent()
    data class Effect(val effectId: String) : ReelLayerContent()
    data class Subtitle(val value: String, val styleId: String = "default") : ReelLayerContent()
}