package com.example.creative

import com.example.creative.core.CreativeHistoryManager
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class CreativeProjectTest {

    @Test
    fun testCreativeProjectCreation() {
        val project = CreativeProject(
            id = "proj_123",
            sourceMedia = "/path/to/video.mp4",
            type = CreativeType.REEL
        )

        assertEquals("proj_123", project.id)
        assertEquals("/path/to/video.mp4", project.sourceMedia)
        assertEquals(CreativeType.REEL, project.type)
        assertTrue(project.layers.isEmpty())
    }

    @Test
    fun testCreativeLayersAndHistory() {
        val historyManager = CreativeHistoryManager()
        var project = CreativeProject(id = "proj_1", sourceMedia = "media.jpg")
        historyManager.pushState(project)

        val textLayer = CreativeLayer.Text(id = "layer_1", text = "Hola PanaLink")
        project = project.copy(layers = listOf(textLayer))
        historyManager.pushState(project)

        assertEquals(1, historyManager.getCurrent()?.layers?.size)

        val undone = historyManager.undo()
        assertNotNull(undone)
        assertEquals(0, undone?.layers?.size)

        val redone = historyManager.redo()
        assertNotNull(redone)
        assertEquals(1, redone?.layers?.size)
    }
}
