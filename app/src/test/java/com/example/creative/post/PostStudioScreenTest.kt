package com.example.creative.post

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class PostStudioScreenTest {

    @Test
    fun testPostStudioViewModelInitialization() {
        val viewModel = PostStudioViewModel()

        assertNotNull(viewModel.uiState.value)
        assertEquals(0, viewModel.uiState.value.selectedPageIndex)
        assertFalse(viewModel.uiState.value.isPublishing)
        assertTrue(viewModel.pageManager.pages.value.isNotEmpty())
    }

    @Test
    fun testAddPageAndNavigation() {
        val viewModel = PostStudioViewModel()
        val initialPageCount = viewModel.pageManager.pages.value.size

        viewModel.addPage(PostPage(id = "page_2"))

        assertEquals(initialPageCount + 1, viewModel.pageManager.pages.value.size)
        assertEquals(1, viewModel.uiState.value.selectedPageIndex)
    }

    @Test
    fun testUndoRedoFlow() {
        val viewModel = PostStudioViewModel()

        assertFalse(viewModel.canUndo())

        viewModel.addTextLayer("Hello Post Studio")
        assertTrue(viewModel.canUndo())

        viewModel.undo()
        assertFalse(viewModel.canUndo())
        assertTrue(viewModel.canRedo())

        viewModel.redo()
        assertTrue(viewModel.canUndo())
    }
}
