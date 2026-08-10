package com.example.creative.post

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creative.core.CreativeHistoryManager
import com.example.creative.core.CreativeLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PostStudioState())
    val uiState: StateFlow<PostStudioState> = _uiState.asStateFlow()

    val pageManager = PostPageManager()
    private val historyManager = CreativeHistoryManager()

    init {
        historyManager.pushState(_uiState.value.project.toCreativeProject())
    }

    private var repository: PostStudioRepository? = null
    private var autoSaveJob: Job? = null

    fun initRepository(context: Context) {
        if (repository == null) {
            repository = PostStudioRepository(context.applicationContext)
        }
    }

    fun initFromUris(context: Context, uris: List<Uri>, initialCaption: String = "") {
        initRepository(context)
        viewModelScope.launch {
            val pages = if (uris.isNotEmpty()) {
                PostMediaImporter.importMultipleMedia(context, uris)
            } else {
                listOf(PostPage())
            }

            val project = PostStudioProject(
                caption = initialCaption,
                pages = pages
            )

            pageManager.reorderPages(pages)
            historyManager.pushState(project.toCreativeProject())

            _uiState.value = _uiState.value.copy(
                project = project,
                selectedPageIndex = 0,
                selectedLayerId = null
            )

            scheduleAutoSave()
        }
    }

    fun loadDraft(context: Context, draftId: String) {
        initRepository(context)
        viewModelScope.launch {
            val draft = repository?.loadDraft(draftId)
            if (draft != null) {
                pageManager.reorderPages(draft.pages)
                historyManager.pushState(draft.toCreativeProject())

                _uiState.value = _uiState.value.copy(
                    project = draft,
                    selectedPageIndex = 0,
                    selectedLayerId = null
                )
            }
        }
    }

    fun selectPage(index: Int) {
        pageManager.selectPage(index)
        val currentPage = pageManager.getCurrentPage()
        _uiState.value = _uiState.value.copy(
            selectedPageIndex = pageManager.selectedPageIndex.value,
            selectedLayerId = currentPage.getMainMediaLayer()?.id
        )
    }

    fun addPage(page: PostPage = PostPage()) {
        val newIdx = pageManager.addPage(page)
        syncPagesAndPushHistory()
        selectPage(newIdx)
    }

    fun removePage(pageId: String) {
        pageManager.removePage(pageId)
        syncPagesAndPushHistory()
        selectPage(pageManager.selectedPageIndex.value)
    }

    fun duplicatePage(pageId: String) {
        pageManager.duplicatePage(pageId)
        syncPagesAndPushHistory()
        selectPage(pageManager.selectedPageIndex.value)
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        pageManager.movePage(fromIndex, toIndex)
        syncPagesAndPushHistory()
        selectPage(toIndex)
    }

    fun addTextLayer(text: String, colorHex: String = "#FFFFFF", fontFamily: String = "SansSerif") {
        if (text.isBlank()) return
        val textLayer = CreativeLayer.Text(
            id = java.util.UUID.randomUUID().toString(),
            text = text,
            colorHex = colorHex,
            fontFamily = fontFamily,
            xFraction = 0.5f,
            yFraction = 0.5f,
            scale = 1.0f
        )
        pageManager.addLayerToCurrentPage(textLayer)
        syncPagesAndPushHistory()
        _uiState.value = _uiState.value.copy(selectedLayerId = textLayer.id)
    }

    fun addStickerLayer(stickerUrlOrPath: String) {
        val stickerLayer = CreativeLayer.Sticker(
            id = java.util.UUID.randomUUID().toString(),
            stickerUrlOrPath = stickerUrlOrPath,
            xFraction = 0.5f,
            yFraction = 0.5f,
            scale = 1.0f
        )
        pageManager.addLayerToCurrentPage(stickerLayer)
        syncPagesAndPushHistory()
        _uiState.value = _uiState.value.copy(selectedLayerId = stickerLayer.id)
    }

    fun addDrawingLayer(points: List<Pair<Float, Float>>, colorHex: String = "#FF0000", strokeWidthDp: Float = 4f) {
        val drawingLayer = CreativeLayer.Drawing(
            id = java.util.UUID.randomUUID().toString(),
            points = points,
            strokeColorHex = colorHex,
            strokeWidthDp = strokeWidthDp
        )
        pageManager.addLayerToCurrentPage(drawingLayer)
        syncPagesAndPushHistory()
    }

    fun updateLayer(layer: CreativeLayer) {
        pageManager.updateLayerInCurrentPage(layer)
        syncPagesAndPushHistory()
    }

    fun removeLayer(layerId: String) {
        pageManager.removeLayerFromCurrentPage(layerId)
        syncPagesAndPushHistory()
        _uiState.value = _uiState.value.copy(selectedLayerId = null)
    }

    fun selectLayer(layerId: String?) {
        _uiState.value = _uiState.value.copy(selectedLayerId = layerId)
    }

    fun setAspectRatio(aspectRatio: String) {
        val curPage = pageManager.getCurrentPage()
        val updatedPage = curPage.copy(aspectRatio = aspectRatio)
        val currentPages = pageManager.pages.value.toMutableList()
        val idx = currentPages.indexOfFirst { it.id == curPage.id }
        if (idx >= 0) {
            currentPages[idx] = updatedPage
            pageManager.reorderPages(currentPages)
            syncPagesAndPushHistory()
        }
    }

    fun applyFilterToCurrentPage(filterName: String) {
        val curPage = pageManager.getCurrentPage()
        val updatedLayers = curPage.layers.map { layer ->
            when (layer) {
                is CreativeLayer.Image -> layer.copy(filterName = filterName)
                is CreativeLayer.Video -> layer.copy(filterName = filterName)
                else -> layer
            }
        }
        val updatedPage = curPage.copy(layers = updatedLayers)
        val currentPages = pageManager.pages.value.toMutableList()
        val idx = currentPages.indexOfFirst { it.id == curPage.id }
        if (idx >= 0) {
            currentPages[idx] = updatedPage
            pageManager.reorderPages(currentPages)
            syncPagesAndPushHistory()
        }
    }

    fun setCaption(caption: String) {
        val updatedProj = _uiState.value.project.copy(caption = caption, updatedAtMs = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(project = updatedProj)
        scheduleAutoSave()
    }

    fun setHashtags(hashtags: List<String>) {
        val updatedProj = _uiState.value.project.copy(hashtags = hashtags, updatedAtMs = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(project = updatedProj)
        scheduleAutoSave()
    }

    fun canUndo(): Boolean = historyManager.canUndo()
    fun canRedo(): Boolean = historyManager.canRedo()

    fun undo() {
        val restored = historyManager.undo()
        if (restored != null) {
            // Restore layers into current page
            val curPage = pageManager.getCurrentPage()
            val updatedPage = curPage.copy(layers = restored.layers)
            val currentPages = pageManager.pages.value.toMutableList()
            val idx = currentPages.indexOfFirst { it.id == curPage.id }
            if (idx >= 0) {
                currentPages[idx] = updatedPage
                pageManager.reorderPages(currentPages)
                val updatedProj = _uiState.value.project.copy(pages = currentPages)
                _uiState.value = _uiState.value.copy(project = updatedProj)
            }
        }
    }

    fun redo() {
        val restored = historyManager.redo()
        if (restored != null) {
            val curPage = pageManager.getCurrentPage()
            val updatedPage = curPage.copy(layers = restored.layers)
            val currentPages = pageManager.pages.value.toMutableList()
            val idx = currentPages.indexOfFirst { it.id == curPage.id }
            if (idx >= 0) {
                currentPages[idx] = updatedPage
                pageManager.reorderPages(currentPages)
                val updatedProj = _uiState.value.project.copy(pages = currentPages)
                _uiState.value = _uiState.value.copy(project = updatedProj)
            }
        }
    }

    private fun syncPagesAndPushHistory() {
        val pages = pageManager.pages.value
        val updatedProj = _uiState.value.project.copy(
            pages = pages,
            updatedAtMs = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(project = updatedProj)
        historyManager.pushState(updatedProj.toCreativeProject())
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000)
            repository?.saveDraft(_uiState.value.project)
        }
    }

    fun exportAndPublish(
        context: Context,
        privacy: String = "public",
        onSuccess: (pendingPostId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true, isExporting = true, errorMessage = null)
            try {
                val pendingPostId = PostExportCoordinator.exportAndQueuePost(
                    context = context,
                    project = _uiState.value.project,
                    privacy = privacy
                )
                repository?.deleteDraft(_uiState.value.project.id)
                _uiState.value = _uiState.value.copy(isPublishing = false, isExporting = false)
                onSuccess(pendingPostId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPublishing = false,
                    isExporting = false,
                    errorMessage = e.message ?: "Error al exportar publicación"
                )
                onError(e.message ?: "Error al exportar publicación")
            }
        }
    }
}
