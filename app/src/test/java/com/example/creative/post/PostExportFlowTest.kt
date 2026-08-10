package com.example.creative.post

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class PostExportFlowTest {

    @Test
    fun testPostStudioProjectToCreativeProjectMapping() {
        val page1 = PostPage(
            pageIndex = 0,
            layers = listOf(
                CreativeLayer.Image(id = "img_1", imageUriOrPath = "/storage/p1.jpg"),
                CreativeLayer.Text(id = "txt_1", text = "Caption 1")
            )
        )

        val page2 = PostPage(
            pageIndex = 1,
            layers = listOf(
                CreativeLayer.Image(id = "img_2", imageUriOrPath = "/storage/p2.jpg")
            )
        )

        val project = PostStudioProject(
            id = "proj_export_test",
            caption = "Test export caption #panalink",
            pages = listOf(page1, page2)
        )

        val creativeProject = project.toCreativeProject()

        assertEquals(CreativeType.POST, creativeProject.type)
        assertEquals("/storage/p1.jpg", creativeProject.sourceMedia)
        assertEquals(3, creativeProject.layers.size)
    }
}
