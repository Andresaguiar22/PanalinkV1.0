package com.example.creative.reels

/** Central immutable state for the independent Reel editor modules. */
data class ReelEditorComposition(
    val timeline: ReelTimelineState = ReelTimelineState(),
    val audio: ReelAudioTrack? = null,
    val activeTool: ReelEditorTool = ReelEditorTool.SELECT,
    val isPreviewing: Boolean = false
) {
    fun setAudio(track: ReelAudioTrack?): ReelEditorComposition = copy(audio = track)
    fun setTool(tool: ReelEditorTool): ReelEditorComposition = copy(activeTool = tool)
    fun setPreviewing(value: Boolean): ReelEditorComposition = copy(isPreviewing = value)
}

enum class ReelEditorTool {
    SELECT, CROP, FILTER, ADJUST, TEXT, STICKER, AUDIO, SPEED
}
