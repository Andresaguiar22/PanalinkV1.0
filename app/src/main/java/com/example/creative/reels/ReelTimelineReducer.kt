package com.example.creative.reels

/** Pure reducer for timeline actions; keeps ReelEditorScreen free of timeline mutation logic. */
sealed interface ReelTimelineAction {
    data class AddClip(val clip: ReelMediaClip) : ReelTimelineAction
    data class RemoveClip(val id: String) : ReelTimelineAction
    data class SelectClip(val id: String?) : ReelTimelineAction
    data class MoveClip(val id: String, val targetIndex: Int) : ReelTimelineAction
    data class UpdateClip(val clip: ReelMediaClip) : ReelTimelineAction
    data class SetBackgroundAudio(val uri: String?) : ReelTimelineAction
    data class SetBackgroundAudioVolume(val volume: Float) : ReelTimelineAction
}

object ReelTimelineReducer {
    fun reduce(state: ReelTimelineState, action: ReelTimelineAction): ReelTimelineState = when (action) {
        is ReelTimelineAction.AddClip -> state.addClip(action.clip)
        is ReelTimelineAction.RemoveClip -> state.removeClip(action.id)
        is ReelTimelineAction.SelectClip -> state.selectClip(action.id)
        is ReelTimelineAction.MoveClip -> state.moveClip(action.id, action.targetIndex)
        is ReelTimelineAction.UpdateClip -> state.updateClip(action.clip)
        is ReelTimelineAction.SetBackgroundAudio -> state.setBackgroundAudio(action.uri)
        is ReelTimelineAction.SetBackgroundAudioVolume -> state.setBackgroundAudioVolume(action.volume)
    }
}
