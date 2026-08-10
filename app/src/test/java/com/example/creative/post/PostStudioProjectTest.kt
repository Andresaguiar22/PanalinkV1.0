package com.example.creative.post

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class PostStudioProjectTest {

    @Test
    fun testPostStudioProjectToCreativeProject() {
        val page1 = PostPage(
            pageIndex = 0,
            layers = listOf(
                CreativeLayer.Image(
                    id = "img_1",
                    imageUriOrPath = "/storage/test_1.jpg"
                ),
                CreativeLayer.Text(
                    id = "txt_1",
                    text = "Page 1 Title"
                )
            )
        )

        val page2 = PostPage(
            pageIndex = 1,
            layers = listOf(
                CreativeLayer.Image(
                    id = "img_2",
                    imageUriOrPath = "/storage/test_2.jpg"
                )
            )
        )

        val project = PostStudioProject(
            title = "Test Post",
            pages = listOf(page1, page2),
            caption = "Amazing multi-page post #test",
            hashtags = listOf("test", "panalink")
        )

        val creativeProject = project.toCreativeProject()

        assertEquals(CreativeType.POST, creativeProject.type)
        assertEquals("/storage/test_1.jpg", creativeProject.sourceMedia)
        assertEquals(3, creativeProject.layers.size)
    }

    @Test
    fun testPostPageManagerOperations() {
        val manager = PostPageManager(initialPages = listOf(PostPage(id = "p1", pageIndex = 0)))

        assertEquals(1, manager.pages.value.size)
        assertEquals(0, manager.selectedPageIndex.value)

        val newPageIdx = manager.addPage(PostPage(id = "p2"))
        assertEquals(1, newPageIdx)
        assertEquals(2, manager.pages.value.size)

        manager.duplicatePage("p1")
        assertEquals(3, manager.pages.value.size)

        manager.selectPage(0)
        manager.addLayerToCurrentPage(
            CreativeLayer.Text(id = "t1", text = "Added Text Layer")
        )

        val curPage = manager.getCurrentPage()
        assertEquals(1, curPage.layers.size)
        assertTrue(curPage.layers.first() is CreativeLayer.Text)

        manager.removePage("p2")
        assertEquals(2, manager.pages.value.size)
    }
}
