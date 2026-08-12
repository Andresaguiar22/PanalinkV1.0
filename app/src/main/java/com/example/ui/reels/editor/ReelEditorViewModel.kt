package com.example.ui.reels.editor

import androidx.lifecycle.ViewModel
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelLayerContent
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTimelineEditOperations
import com.example.ui.reels.editor.model.ReelTimelineOperations
import com.example.ui.reels.editor.model.ReelTrack
import com.example.ui.reels.editor.model.ReelTrackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** State and editing commands for Reel Studio. Media rendering/export remains outside the ViewModel. */
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
            is ReelEditorEvent.RemoveTrack -> removeTrack(event.trackId)
            is ReelEditorEvent.AddLayer -> _uiState.update { state -> state.copy(project = state.project.copy(timeline = ReelTimelineOperations.addLayer(state.project.timeline, event.trackId, event.layer))) }
            is ReelEditorEvent.UpdateLayer -> _uiState.update { state -> state.copy(project = state.project.copy(timeline = ReelTimelineOperations.updateLayer(state.project.timeline, event.trackId, event.layer))) }
            is ReelEditorEvent.RemoveLayer -> removeLayer(event.trackId, event.layerId)
            is ReelEditorEvent.SplitLayer -> applyEdit { ReelTimelineEditOperations.splitLayer(it, event.trackId, event.layerId, event.atMs) }
            is ReelEditorEvent.TrimLayer -> applyEdit { ReelTimelineEditOperations.trimLayer(it, event.trackId, event.layerId, event.startMs, event.endMs) }
            is ReelEditorEvent.ReorderLayer -> applyEdit { ReelTimelineEditOperations.reorderLayer(it, event.trackId, event.layerId, event.targetIndex) }
            ReelEditorEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /** Appends media to the end of the video track instead of replacing existing clips. */
    private fun selectMedia(uri: String, mimeType: String?, durationMs: Long) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        _uiState.update { state ->
            val existingTrack = state.project.timeline.tracks.firstOrNull { it.type == ReelTrackType.VIDEO }
            val videoTrack = existingTrack ?: ReelTrack("video", ReelTrackType.VIDEO, "Video", zIndex = 0)
            val startMs = videoTrack.layers.maxOfOrNull { it.endTimeMs } ?: 0L
            val layer = ReelLayer(
                id = "media_${System.nanoTime()}", type = ReelTrackType.VIDEO,
                startTimeMs = startMs, endTimeMs = startMs + safeDuration,
                content = ReelLayerContent.Media(uri, mimeType)
            )
            var timeline = if (existingTrack == null) ReelTimelineOperations.addTrack(state.project.timeline, videoTrack) else state.project.timeline
            timeline = ReelTimelineOperations.addLayer(timeline, videoTrack.id, layer)
            timeline = timeline.withDuration(timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L)
            state.copy(project = state.project.copy(durationMs = timeline.durationMs, timeline = timeline, selectedLayerId = layer.id))
        }
    }

    private fun addTrack(type: ReelTrackType, name: String) {
        _uiState.update { state ->
            val track = ReelTrack("${type.name.lowercase()}_${System.nanoTime()}", type, name, zIndex = state.project.timeline.tracks.size)
            state.copy(project = state.project.copy(timeline = ReelTimelineOperations.addTrack(state.project.timeline, track)))
        }
    }

    private fun removeTrack(trackId: String) {
        _uiState.update { state ->
            val timeline = ReelTimelineOperations.removeTrack(state.project.timeline, trackId)
            state.copy(project = state.project.copy(durationMs = timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L, timeline = timeline.withDuration(timeline.durationMs)))
        }
    }

    private fun removeLayer(trackId: String, layerId: String) {
        _uiState.update { state ->
            val timeline = ReelTimelineOperations.removeLayer(state.project.timeline, trackId, layerId)
            val duration = timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L
            state.copy(project = state.project.copy(durationMs = duration, timeline = timeline.withDuration(duration), selectedLayerId = state.project.selectedLayerId.takeUnless { it == layerId }))
        }
    }

    private fun applyEdit(transform: (com.example.ui.reels.editor.model.ReelTimeline) -> com.example.ui.reels.editor.model.ReelTimeline) {
        _uiState.update { state ->
            val timeline = transform(state.project.timeline)
            val duration = timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L
            state.copy(project = state.project.copy(durationMs = duration, timeline = timeline.withDuration(duration)))
        }
    }
}
