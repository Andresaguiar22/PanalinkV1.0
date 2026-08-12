package com.example.creative.reels

/** A single ordered media item in a Reel timeline. */
data class ReelMediaClip(
    val id: String,
    val localUri: String,
    val mimeType: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val transition: ReelTransition = ReelTransition.CUT
) {
    val effectiveDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

enum class ReelTransition {
    CUT,
    FADE,
    SLIDE
}
