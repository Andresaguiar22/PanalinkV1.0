package com.example.ui.reels.editor.model

/** Logical lanes in the Reel timeline. New editor features should map to a track instead of UI state. */
enum class ReelTrackType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    STICKER,
    EFFECT,
    SUBTITLE
}

data class ReelTrack(
    val id: String,
    val type: ReelTrackType,
    val name: String,
    val zIndex: Int = 0,
    val muted: Boolean = false,
    val locked: Boolean = false,
    val layers: List<ReelLayer> = emptyList()
)
