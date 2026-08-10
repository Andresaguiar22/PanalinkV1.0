package com.example.creative.video

enum class TransitionType(val displayName: String) {
    NONE("None"),
    FADE("Fade"),
    ZOOM("Zoom"),
    SLIDE("Slide"),
    BLUR("Blur")
}

data class VideoTransition(
    val type: TransitionType,
    val durationMs: Long = 500L
)

object VideoTransitionEngine {
    fun getSupportedTransitions(): List<TransitionType> {
        return TransitionType.values().toList()
    }
}
