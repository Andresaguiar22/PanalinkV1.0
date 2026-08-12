package com.example.creative.reels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Small state holder that can be consumed by ReelEditorScreen without moving
 * media playback, publishing, or Android lifecycle concerns into the reducer.
 */
class ReelEditorController(
    initialState: ReelEditorComposition = ReelEditorComposition()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ReelEditorComposition> = _state.asStateFlow()

    fun dispatch(action: ReelEditorAction) {
        _state.update { current ->
            ReelEditorReducer.reduce(current, action)
        }
    }

    fun selectClip(id: String?) {
        dispatch(ReelEditorAction.Timeline(ReelTimelineAction.SelectClip(id)))
    }

    fun addClip(clip: ReelMediaClip) {
        dispatch(ReelEditorAction.Timeline(ReelTimelineAction.AddClip(clip)))
    }

    fun removeClip(id: String) {
        dispatch(ReelEditorAction.Timeline(ReelTimelineAction.RemoveClip(id)))
    }

    fun moveClip(id: String, targetIndex: Int) {
        dispatch(ReelEditorAction.Timeline(ReelTimelineAction.MoveClip(id, targetIndex)))
    }

    fun updateClip(clip: ReelMediaClip) {
        dispatch(ReelEditorAction.Timeline(ReelTimelineAction.UpdateClip(clip)))
    }

    fun setAudio(track: ReelAudioTrack?) {
        dispatch(ReelEditorAction.SetAudio(track))
    }

    fun setTool(tool: ReelEditorTool) {
        dispatch(ReelEditorAction.SetTool(tool))
    }

    fun setPreviewing(previewing: Boolean) {
        dispatch(ReelEditorAction.SetPreviewing(previewing))
    }
}
