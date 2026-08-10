package com.example.media.playlist.cover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.creative.canvas.CanvasEditorEngine
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * P6.7.4 - Playlist Cover View Model
 * Orchestrates the cover design process using the Creative Engine.
 */
class PlaylistCoverViewModel(
    application: Application,
    private val repository: PlaylistCoverRepository,
    private val exporter: PlaylistCoverExporter
) : AndroidViewModel(application) {

    private val _project = MutableStateFlow<CreativeProject?>(null)
    val project: StateFlow<CreativeProject?> = _project.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun createNewProject(playlistName: String) {
        val newProject = CreativeProject(
            id = UUID.randomUUID().toString(),
            sourceMedia = "", // Background color or image
            type = CreativeType.PLAYLIST_COVER,
            layers = listOf(
                CreativeLayer.Text(
                    id = UUID.randomUUID().toString(),
                    text = playlistName,
                    fontSizeSp = 32f,
                    colorHex = "#FFFFFF"
                )
            )
        )
        _project.value = newProject
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _project.value = repository.getProject(projectId)
        }
    }

    fun addLayer(layer: CreativeLayer) {
        _project.value?.let { current ->
            _project.value = current.copy(layers = current.layers + layer)
        }
    }

    fun removeLayer(layerId: String) {
        _project.value?.let { current ->
            _project.value = current.copy(layers = current.layers.filter { it.id != layerId })
        }
    }

    fun updateLayer(updatedLayer: CreativeLayer) {
        _project.value?.let { current ->
            _project.value = current.copy(
                layers = current.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
            )
        }
    }

    fun exportAndApply(onExported: (String) -> Unit) {
        val currentProject = _project.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            val path = exporter.exportCover(currentProject)
            _isExporting.value = false
            path?.let { onExported(it) }
        }
    }
}
