package com.example.creative.reels

data class ReelAudioTrack(
    val uri: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val volume: Float = 1f,
    val muted: Boolean = false
) {
    val effectiveDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
    val effectiveVolume: Float get() = if (muted) 0f else volume.coerceIn(0f, 1f)

    fun withTrim(startMs: Long, endMs: Long): ReelAudioTrack = copy(
        trimStartMs = startMs.coerceIn(0L, durationMs),
        trimEndMs = endMs.coerceIn(startMs.coerceIn(0L, durationMs), durationMs)
    )
}
