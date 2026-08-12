package com.example.creative.reels

/** Immutable timeline state for a multi-media Reel editor. */
data class ReelTimelineState(
    val clips: List<ReelMediaClip> = emptyList(),
    val selectedClipId: String? = null,
    val backgroundAudioUri: String? = null,
    val backgroundAudioVolume: Float = 1f,
    val musicTrimStartMs: Long = 0L
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }

    fun addClip(clip: ReelMediaClip): ReelTimelineState =
        copy(
            clips = clips + clip,
            selectedClipId = clip.id
        )

    fun removeClip(id: String): ReelTimelineState =
        copy(
            clips = clips.filterNot { it.id == id },
            selectedClipId = selectedClipId.takeUnless { it == id }
        )

    fun moveClip(id: String, targetIndex: Int): ReelTimelineState {
        val index = clips.indexOfFirst { it.id == id }
        if (index < 0) return this
        val mutable = clips.toMutableList()
        val clip = mutable.removeAt(index)
        mutable.add(targetIndex.coerceIn(0, mutable.size), clip)
        return copy(clips = mutable)
    }

    fun updateClip(updated: ReelMediaClip): ReelTimelineState =
        copy(clips = clips.map { if (it.id == updated.id) updated else it })

    fun selectClip(id: String?): ReelTimelineState =
        copy(selectedClipId = id?.takeIf { candidate -> clips.any { it.id == candidate } })

    fun setBackgroundAudio(uri: String?): ReelTimelineState =
        copy(backgroundAudioUri = uri)

    fun setBackgroundAudioVolume(volume: Float): ReelTimelineState =
        copy(backgroundAudioVolume = volume.coerceIn(0f, 1f))
}
