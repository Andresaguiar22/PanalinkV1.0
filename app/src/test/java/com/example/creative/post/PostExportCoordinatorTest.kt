package com.example.creative.post

import com.example.creative.core.CreativeLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PostExportCoordinatorTest {

    @Test
    fun testPostStudioProjectStructure() {
        val page1 = PostPage(
            pageIndex = 0,
            layers = listOf(
                CreativeLayer.Image(id = "img1", imageUriOrPath = "/path/file1.jpg")
            )
        )

        val page2 = PostPage(
            pageIndex = 1,
            layers = listOf(
                CreativeLayer.Video(id = "vid1", videoUriOrPath = "/path/file2.mp4")
            )
        )

        val project = PostStudioProject(
            id = "proj_test_123",
            caption = "Test Post Export",
            hashtags = listOf("panalink", "creative"),
            pages = listOf(page1, page2)
        )

        assertEquals("proj_test_123", project.id)
        assertEquals(2, project.pages.size)
        assertNotNull(project.pages[0].getMainMediaLayer())
        assertNotNull(project.pages[1].getMainMediaLayer())
    }
}
