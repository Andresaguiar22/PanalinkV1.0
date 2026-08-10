package com.example.creative.post

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class PostCanvasInteractionTest {

    @Test
    fun testAddTextLayerToCurrentPage() {
        val viewModel = PostStudioViewModel()
        viewModel.addTextLayer("Canva Pro Style", colorHex = "#38BDF8")

        val curPage = viewModel.pageManager.getCurrentPage()
        val textLayer = curPage.layers.firstOrNull { it is CreativeLayer.Text } as? CreativeLayer.Text

        assertNotNull(textLayer)
        assertEquals("Canva Pro Style", textLayer?.text)
        assertEquals("#38BDF8", textLayer?.colorHex)
    }

    @Test
    fun testAddStickerLayer() {
        val viewModel = PostStudioViewModel()
        viewModel.addStickerLayer("🔥")

        val curPage = viewModel.pageManager.getCurrentPage()
        val stickerLayer = curPage.layers.firstOrNull { it is CreativeLayer.Sticker } as? CreativeLayer.Sticker

        assertNotNull(stickerLayer)
        assertEquals("🔥", stickerLayer?.stickerUrlOrPath)
    }

    @Test
    fun testApplyFilterToCurrentPage() {
        val page = PostPage(
            layers = listOf(CreativeLayer.Image(id = "img_1", imageUriOrPath = "/test.jpg"))
        )
        val viewModel = PostStudioViewModel()
        viewModel.addPage(page)

        viewModel.applyFilterToCurrentPage("Cyberpunk")

        val updatedPage = viewModel.pageManager.getCurrentPage()
        val imgLayer = updatedPage.layers.firstOrNull { it is CreativeLayer.Image } as? CreativeLayer.Image

        assertEquals("Cyberpunk", imgLayer?.filterName)
    }
}
