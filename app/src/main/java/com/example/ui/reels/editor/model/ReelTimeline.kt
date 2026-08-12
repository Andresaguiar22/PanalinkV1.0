package com.example.ui.reels.editor.model

/** Timeline state kept independent from Compose. */
data class ReelTimeline(
    val tracks: List<ReelTrack> = emptyList(),
    val currentTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val zoom: Float = 1f
) {
    fun seekTo(timeMs: Long): ReelTimeline = copy(
        currentTimeMs = timeMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    )

    fun withDuration(durationMs: Long): ReelTimeline {
        val safeDuration = durationMs.coerceAtLeast(0L)
        return copy(
            durationMs = safeDuration,
            currentTimeMs = currentTimeMs.coerceIn(0L, safeDuration)
        )
    }

    fun withZoom(zoom: Float): ReelTimeline = copy(
        zoom = zoom.coerceIn(0.5f, 8f)
    )
}
