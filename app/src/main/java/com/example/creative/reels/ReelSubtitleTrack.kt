package com.example.creative.reels

data class ReelSubtitleTrack(
    val cues: List<ReelSubtitle> = emptyList(),
    val enabled: Boolean = true
) {
    fun add(cue: ReelSubtitle): ReelSubtitleTrack = copy(cues = (cues + cue).sortedBy { it.startMs })
    fun remove(id: String): ReelSubtitleTrack = copy(cues = cues.filterNot { it.id == id })
    fun setEnabled(value: Boolean): ReelSubtitleTrack = copy(enabled = value)
}
