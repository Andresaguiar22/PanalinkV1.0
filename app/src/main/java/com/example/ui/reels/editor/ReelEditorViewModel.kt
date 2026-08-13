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
            is ReelEditorEvent.AddTextLayer -> addTextLayer(event.text)
            is ReelEditorEvent.UpdateTextStyle -> updateTextStyle(event)
            is ReelEditorEvent.TransformLayer -> transformLayer(event)
            is ReelEditorEvent.AddImageLayer -> addImageLayer(event.imageUri)
            is ReelEditorEvent.AddStickerLayer -> addStickerLayer(event.stickerUri)
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

    private fun selectMedia(uri: String, mimeType: String?, durationMs: Long) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        _uiState.update { state ->
            val existingTrack = state.project.timeline.tracks.firstOrNull { it.type == ReelTrackType.VIDEO }
            val videoTrack = existingTrack ?: ReelTrack("video", ReelTrackType.VIDEO, "Video", zIndex = 0)
            val startMs = videoTrack.layers.maxOfOrNull { it.endTimeMs } ?: 0L
            val layer = ReelLayer("media_${System.nanoTime()}", ReelTrackType.VIDEO, startMs, startMs + safeDuration, content = ReelLayerContent.Media(uri, mimeType))
            var timeline = if (existingTrack == null) ReelTimelineOperations.addTrack(state.project.timeline, videoTrack) else state.project.timeline
            timeline = ReelTimelineOperations.addLayer(timeline, videoTrack.id, layer)
            timeline = timeline.withDuration(timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L)
            state.copy(project = state.project.copy(durationMs = timeline.durationMs, timeline = timeline, selectedLayerId = layer.id))
        }
    }

    private fun addTextLayer(text: String) = addOverlayTrack(ReelTrackType.TEXT, "Texto") { start, end ->
        ReelLayer("text_${System.nanoTime()}", ReelTrackType.TEXT, start, end, zIndex = 100, x = 0.5f, y = 0.5f, content = ReelLayerContent.Text(value = text.ifBlank { "Escribe aquí" }))
    }

    private fun addImageLayer(imageUri: String) = addOverlayTrack(ReelTrackType.IMAGE, "Fotos") { start, end ->
        ReelMediaLayerFactory.image(imageUri, start, end)
    }

    private fun addStickerLayer(stickerUri: String) = addOverlayTrack(ReelTrackType.STICKER, "Stickers") { start, end ->
        ReelLayer("sticker_${System.nanoTime()}", ReelTrackType.STICKER, start, end, zIndex = 110, x = 0.5f, y = 0.5f, content = ReelLayerContent.Sticker(stickerUri))
    }

    private fun updateTextStyle(event: ReelEditorEvent.UpdateTextStyle) {
        updateSelectedLayer(event.layerId) { layer ->
            val content = layer.content as? ReelLayerContent.Text ?: return@updateSelectedLayer layer
            layer.copy(content = content.copy(
                fontSizeSp = (event.fontSizeSp ?: content.fontSizeSp).coerceIn(8f, 160f),
                colorArgb = event.colorArgb ?: content.colorArgb,
                backgroundColorArgb = event.backgroundColorArgb ?: content.backgroundColorArgb
            ))
        }
    }

    private fun transformLayer(event: ReelEditorEvent.TransformLayer) {
        updateSelectedLayer(event.layerId) { layer ->
            layer.copy(
                scale = (event.scale ?: layer.scale).coerceIn(0.1f, 8f),
                rotationDegrees = event.rotationDegrees ?: layer.rotationDegrees
            )
        }
    }

    private fun updateSelectedLayer(layerId: String, transform: (ReelLayer) -> ReelLayer) {
        _uiState.update { state ->
            val track = state.project.timeline.tracks.firstOrNull { it.layers.any { layer -> layer.id == layerId } } ?: return@update state
            val layer = track.layers.first { it.id == layerId }
            val updated = transform(layer)
            state.copy(project = state.project.copy(timeline = ReelTimelineOperations.updateLayer(state.project.timeline, track.id, updated), selectedLayerId = layerId))
        }
    }

    private fun addOverlayTrack(type: ReelTrackType, name: String, createLayer: (Long, Long) -> ReelLayer) {
        _uiState.update { state ->
            val timeline = state.project.timeline
            val track = timeline.tracks.firstOrNull { it.type == type } ?: ReelTrack("${type.name.lowercase()}_${System.nanoTime()}", type, name, zIndex = timeline.tracks.size)
            val baseEnd = timeline.durationMs.coerceAtLeast(1000L)
            val start = timeline.currentTimeMs.coerceIn(0L, (baseEnd - 500L).coerceAtLeast(0L))
            val end = (start + 5000L).coerceAtMost(baseEnd).coerceAtLeast(start + 1L)
            var updated = if (timeline.tracks.none { it.id == track.id }) ReelTimelineOperations.addTrack(timeline, track) else timeline
            val layer = createLayer(start, end)
            updated = ReelTimelineOperations.addLayer(updated, track.id, layer)
            state.copy(project = state.project.copy(timeline = updated, selectedLayerId = layer.id))
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
            val duration = timeline.tracks.flatMap { it.layers }.maxOfOrNull { it.endTimeMs } ?: 0L
            state.copy(project = state.project.copy(durationMs = duration, timeline = timeline.withDuration(duration)))
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