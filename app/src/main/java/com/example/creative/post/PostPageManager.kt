package com.example.creative.post

import com.example.creative.core.CreativeLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P6.6.2 - Post Page Manager
 * Manages carousel pages and layer manipulation per page.
 */
class PostPageManager(
    initialPages: List<PostPage> = listOf(PostPage())
) {
    private val _pages = MutableStateFlow(initialPages)
    val pages: StateFlow<List<PostPage>> = _pages.asStateFlow()

    private val _selectedPageIndex = MutableStateFlow(0)
    val selectedPageIndex: StateFlow<Int> = _selectedPageIndex.asStateFlow()

    fun getCurrentPage(): PostPage {
        val currentList = _pages.value
        val idx = _selectedPageIndex.value.coerceIn(0, (currentList.size - 1).coerceAtLeast(0))
        return currentList.getOrElse(idx) { PostPage() }
    }

    fun selectPage(index: Int) {
        val maxIdx = (_pages.value.size - 1).coerceAtLeast(0)
        _selectedPageIndex.value = index.coerceIn(0, maxIdx)
    }

    fun addPage(page: PostPage = PostPage()): Int {
        val current = _pages.value.toMutableList()
        val newIndex = current.size
        val indexedPage = page.copy(pageIndex = newIndex)
        current.add(indexedPage)
        _pages.value = current
        _selectedPageIndex.value = newIndex
        return newIndex
    }

    fun removePage(pageId: String) {
        val current = _pages.value.toMutableList()
        if (current.size <= 1) return // Keep at least 1 page
        current.removeAll { it.id == pageId }
        val reindexed = current.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        _pages.value = reindexed
        if (_selectedPageIndex.value >= reindexed.size) {
            _selectedPageIndex.value = (reindexed.size - 1).coerceAtLeast(0)
        }
    }

    fun duplicatePage(pageId: String) {
        val current = _pages.value.toMutableList()
        val target = current.firstOrNull { it.id == pageId } ?: return
        val newPage = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            pageIndex = target.pageIndex + 1,
            layers = target.layers.map { layer ->
                when (layer) {
                    is CreativeLayer.Text -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Sticker -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Image -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Video -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Drawing -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Filter -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Audio -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Interactive -> layer.copy(id = java.util.UUID.randomUUID().toString())
                    is CreativeLayer.Group -> layer.copy(id = java.util.UUID.randomUUID().toString())
                }
            }
        )
        current.add(target.pageIndex + 1, newPage)
        val reindexed = current.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        _pages.value = reindexed
        _selectedPageIndex.value = target.pageIndex + 1
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _pages.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val reindexed = current.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        _pages.value = reindexed
        _selectedPageIndex.value = toIndex
    }

    fun reorderPages(newPages: List<PostPage>) {
        val reindexed = newPages.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        _pages.value = reindexed
        if (_selectedPageIndex.value >= reindexed.size) {
            _selectedPageIndex.value = (reindexed.size - 1).coerceAtLeast(0)
        }
    }

    fun addLayerToCurrentPage(layer: CreativeLayer) {
        val currentPage = getCurrentPage()
        val updatedPage = currentPage.copy(layers = currentPage.layers + layer)
        updatePage(updatedPage)
    }

    fun updateLayerInCurrentPage(updatedLayer: CreativeLayer) {
        val currentPage = getCurrentPage()
        val updatedLayers = currentPage.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
        updatePage(currentPage.copy(layers = updatedLayers))
    }

    fun removeLayerFromCurrentPage(layerId: String) {
        val currentPage = getCurrentPage()
        val updatedLayers = currentPage.layers.filterNot { it.id == layerId }
        updatePage(currentPage.copy(layers = updatedLayers))
    }

    private fun updatePage(updatedPage: PostPage) {
        val current = _pages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == updatedPage.id }
        if (idx >= 0) {
            current[idx] = updatedPage
            _pages.value = current
        }
    }
}
