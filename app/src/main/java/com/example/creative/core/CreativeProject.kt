package com.example.creative.core

enum class CreativeType {
    STORY,
    REEL,
    POST,
    CHAT_EDIT,
    PROFILE,
    STICKER,
    PLAYLIST_COVER
}

sealed class CreativeLayer {
    abstract val id: String
    abstract val xFraction: Float
    abstract val yFraction: Float
    abstract val scale: Float
    abstract val rotation: Float
    abstract val isVisible: Boolean
    abstract val isLocked: Boolean
    abstract val groupId: String?
    abstract val startOffsetMs: Long
    abstract val durationMs: Long
    abstract val opacity: Float
    abstract val animationType: String
    abstract val isFlipped: Boolean

    data class Text(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val text: String,
        val colorHex: String = "#FFFFFF",
        val fontSizeSp: Float = 24f,
        val fontFamily: String = "SansSerif",
        val hasShadow: Boolean = true,
        val backgroundColorHex: String? = null
    ) : CreativeLayer()

    data class Sticker(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val stickerUrlOrPath: String,
        val isAnimated: Boolean = false
    ) : CreativeLayer()

    data class Drawing(
        override val id: String,
        override val xFraction: Float = 0f,
        override val yFraction: Float = 0f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val strokeColorHex: String = "#FF0000",
        val strokeWidthDp: Float = 4f,
        val points: List<Pair<Float, Float>> = emptyList()
    ) : CreativeLayer()

    data class Filter(
        override val id: String,
        override val xFraction: Float = 0f,
        override val yFraction: Float = 0f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val filterName: String = "cinematic",
        val intensity: Float = 1.0f
    ) : CreativeLayer()

    data class Audio(
        override val id: String,
        override val xFraction: Float = 0f,
        override val yFraction: Float = 0f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val audioUrlOrPath: String,
        val volume: Float = 1.0f
    ) : CreativeLayer()

    data class Interactive(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val interactiveType: String, // POLL, QUESTION, COUNTDOWN, LOCATION, MENTION, HASHTAG, LINK, TIME, WEATHER
        val title: String = "",
        val optionA: String = "",
        val optionB: String = "",
        val extraData: String = ""
    ) : CreativeLayer()

    data class Image(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val imageUriOrPath: String,
        val cropXRatio: Float = 0f,
        val cropYRatio: Float = 0f,
        val cropWidthRatio: Float = 1f,
        val cropHeightRatio: Float = 1f,
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val filterName: String = "Normal"
    ) : CreativeLayer()

    data class Video(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val videoUriOrPath: String,
        val volume: Float = 1.0f,
        val speed: Float = 1.0f,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = 15000L,
        val filterName: String = "Normal"
    ) : CreativeLayer()

    data class Group(
        override val id: String,
        override val xFraction: Float = 0.5f,
        override val yFraction: Float = 0.5f,
        override val scale: Float = 1.0f,
        override val rotation: Float = 0f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val groupId: String? = null,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        override val opacity: Float = 1.0f,
        override val animationType: String = "none",
        override val isFlipped: Boolean = false,
        val memberLayerIds: List<String> = emptyList(),
        val groupName: String = "Grupo"
    ) : CreativeLayer()
}

data class CreativeProject(
    val id: String,
    val sourceMedia: String,
    val layers: List<CreativeLayer> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val type: CreativeType = CreativeType.STORY
)
