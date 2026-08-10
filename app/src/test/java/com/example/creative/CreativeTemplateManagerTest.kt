package com.example.creative.template

import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class CreativeTemplateManagerTest {

    @Test
    fun testCreateAndApplyTemplate() {
        val project = CreativeProject(
            id = "proj_tmpl_src",
            sourceMedia = "/media/sample.mp4",
            type = CreativeType.REEL,
            layers = emptyList()
        )

        val tmpl = CreativeTemplateManager.createTemplateFromProject(
            project = project,
            templateName = "Plantilla Neón",
            description = "Plantilla con estilo neón"
        )

        assertEquals("Plantilla Neón", tmpl.name)
        assertTrue(CreativeTemplateManager.getTemplates().any { it.id == tmpl.id })
    }
}
