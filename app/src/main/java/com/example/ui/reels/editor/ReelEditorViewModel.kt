package com.example.ui.reels.editor

import androidx.lifecycle.ViewModel
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTimelineOperations
import com.example.ui.reels.editor.model.ReelTrack
import com.example.ui.reels.editor.model.ReelTrackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** State and pure editing commands for Reel Studio. No Compose or media rendering belongs here. */
class ReelEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReelEditorUiState())
    val uiState: StateFlow<ReelEditorUiState> = _uiState.asStateFlow()

    fun onEvent(event: ReelEditorEvent) {
        when (event) {
            is ReelEditorEvent.MediaSelected -> selectMedia(event.uri, event.mimeType, event.durationMs)
            is ReelEditorEvent.Seek -> _uiState.update { it.copy(project = it.project.copy(timeline = it.project.timeline.seekTo(event.timeMs))) }
            is ReelEditorEvent.ZoomChanged -> _uiState.update { it.copy(project = it.project.copy(timeline = it.project.timeline.withZoom(event.zoom))) }
            is ReelEditorEvent.SelectLayer -> _uiState.update { it.copy(project = it.project.copy(selectedLayerId = event.layerId)) }
            is ReelEditorEvent.AddTrack -> addTrack(event.type, event.name)
            is ReelEditorEvent.RemoveTrack -> _uiState.update { it.copy(project = it.project.copy(timeline = ReelTimelineOperations.removeTrack(it.project.timeline, event.trackId))) }
            is ReelEditorEvent.AddLayer -> _uiState.update { state -> state.copy(project = state.project.copy(timeline = ReelTimelineOperations.addLayer(state.project.timeline, event.trackId, event.layer))) }
            is ReelEditorEvent.UpdateLayer -> _uiState.update { state -> state.copy(project = state.project.copy(timeline = ReelTimelineOperations.updateLayer(state.project.timeline, event.trackId, event.layer))) }
            is ReelEditorEvent.RemoveLayer -> _uiState.update { state -> state.copy(project = state.project.copy(timeline = ReelTimelineOperations.removeLayer(state.project.timeline, event.trackId, event.layerId))) }
            ReelEditorEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun selectMedia(uri: String, mimeType: String?, durationMs: Long) {
        _uiState.update { state ->
            val videoTrack = state.project.timeline.tracks.firstOrNull { it.type == ReelTrackType.VIDEO }
                ?: ReelTrack("video", ReelTrackType.VIDEO, "Video", zIndex = 0)
            val layer = ReelLayer(
                id = "media_${System.currentTimeMillis()}",
                type = ReelTrackType.VIDEO,
                startTimeMs = 0L,
                endTimeMs = durationMs.coerceAtLeast(0L),
                content = com.example.ui.reels.editor.model.ReelLayerContent.Media(uri, mimeType)
            )
            var timeline = if (state.project.timeline.tracks.none { it.id == videoTrack.id }) {
                ReelTimelineOperations.addTrack(state.project.timeline, videoTrack)
            } else state.project.timeline
            timeline = ReelTimelineOperations.addLayer(timeline, videoTrack.id, layer).withDuration(durationMs)
            state.copy(project = state.project.copy(durationMs = durationMs.coerceAtLeast(0L), timeline = timeline))
        }
    }

    private fun addTrack(type: ReelTrackType, name: String) {
        _uiState.update { state ->
            val id = "${type.name.lowercase()}_${System.currentTimeMillis()}"
            val track = ReelTrack(id, type, name, zIndex = state.project.timeline.tracks.size)
            state.copy(project = state.project.copy(timeline = ReelTimelineOperations.addTrack(state.project.timeline, track)))
        }
    }
}
