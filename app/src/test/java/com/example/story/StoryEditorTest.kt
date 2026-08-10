package com.example.story

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import com.example.creative.template.CreativeTemplateManager
import org.junit.Assert.*
import org.junit.Test

class StoryEditorTest {

    @Test
    fun testStoryTemplateApplication() {
        val baseProject = CreativeProject(
            id = "proj_base",
            sourceMedia = "image.jpg",
            type = CreativeType.STORY
        )

        val templates = CreativeTemplateManager.getTemplates()
        assertTrue(templates.isNotEmpty())

        val template = templates.first()
        val updatedProject = CreativeTemplateManager.applyTemplateToProject(template, baseProject)

        assertNotNull(updatedProject)
        assertEquals(template.layers.size, updatedProject.layers.size)
    }

    @Test
    fun testDrawingLayerIntegration() {
        val drawingLayer = CreativeLayer.Drawing(
            id = "draw_1",
            strokeColorHex = "#00E5FF",
            strokeWidthDp = 8f,
            points = listOf(Pair(0.1f, 0.2f), Pair(0.5f, 0.8f))
        )

        val project = CreativeProject(
            id = "proj_draw",
            sourceMedia = "",
            layers = listOf(drawingLayer)
        )

        assertEquals(1, project.layers.size)
        val layer = project.layers.first() as CreativeLayer.Drawing
        assertEquals("#00E5FF", layer.strokeColorHex)
        assertEquals(2, layer.points.size)
    }
}
