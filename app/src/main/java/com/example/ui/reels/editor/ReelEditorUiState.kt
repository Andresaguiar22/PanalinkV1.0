package com.example.ui.reels.editor

import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTrackType

/** UI-facing state. Rendering/exporting is intentionally kept out of this model. */
data class ReelEditorUiState(
    val project: ReelProject = ReelProject(id = "draft_${System.currentTimeMillis()}"),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isPublishing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ReelEditorEvent {
    data class MediaSelected(
        val uri: String,
        val mimeType: String? = null,
        val durationMs: Long = 0L
    ) : ReelEditorEvent

    data class Seek(val timeMs: Long) : ReelEditorEvent
    data class ZoomChanged(val zoom: Float) : ReelEditorEvent
    data class SelectLayer(val layerId: String?) : ReelEditorEvent
    data class AddTrack(val type: ReelTrackType, val name: String) : ReelEditorEvent
    data class RemoveTrack(val trackId: String) : ReelEditorEvent
    data class AddLayer(val trackId: String, val layer: ReelLayer) : ReelEditorEvent
    data class UpdateLayer(val trackId: String, val layer: ReelLayer) : ReelEditorEvent
    data class RemoveLayer(val trackId: String, val layerId: String) : ReelEditorEvent
    data object ClearError : ReelEditorEvent
}
