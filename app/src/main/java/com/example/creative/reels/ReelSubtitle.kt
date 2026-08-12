package com.example.creative.reels

/** Timed subtitle cue; can later be populated by an on-device speech recognizer. */
data class ReelSubtitle(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val style: ReelSubtitleStyle = ReelSubtitleStyle.CLASSIC
) {
    init {
        require(startMs >= 0L)
        require(endMs >= startMs)
        require(text.isNotBlank())
    }
}

enum class ReelSubtitleStyle { CLASSIC, BOLD, OUTLINE, MINIMAL }
