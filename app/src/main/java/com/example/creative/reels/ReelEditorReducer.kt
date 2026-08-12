package com.example.creative.reels

sealed interface ReelEditorAction {
    data class Timeline(val action: ReelTimelineAction) : ReelEditorAction
    data class SetAudio(val track: ReelAudioTrack?) : ReelEditorAction
    data class SetTool(val tool: ReelEditorTool) : ReelEditorAction
    data class SetPreviewing(val value: Boolean) : ReelEditorAction
}

object ReelEditorReducer {
    fun reduce(state: ReelEditorComposition, action: ReelEditorAction): ReelEditorComposition = when (action) {
        is ReelEditorAction.Timeline -> state.copy(
            timeline = ReelTimelineReducer.reduce(state.timeline, action.action)
        )
        is ReelEditorAction.SetAudio -> state.setAudio(action.track)
        is ReelEditorAction.SetTool -> state.setTool(action.tool)
        is ReelEditorAction.SetPreviewing -> state.setPreviewing(action.value)
    }
}
