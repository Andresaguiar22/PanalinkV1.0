package com.example.creative.templates

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostTemplateEngineTest {

    private lateinit var sampleProject: PostStudioProject

    @Before
    fun setUp() {
        val initialPage = PostPage(
            id = "page_1",
            aspectRatio = "4:5",
            layers = listOf(
                CreativeLayer.Image(
                    id = "bg_img_1",
                    imageUriOrPath = "content://media/sample_travel.jpg",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    scale = 1.0f
                )
            )
        )
        sampleProject = PostStudioProject(
            id = "project_test_101",
            title = "Proyecto de Prueba",
            pages = listOf(initialPage)
        )
    }

    @Test
    fun testApplyTravelTemplateWithVariables() {
        val travelTemplate = PostTemplateEngine.findTemplateById("template_travel_cinematic")
        assertNotNull("La plantilla Travel Cinematic debe existir", travelTemplate)

        val variables = mapOf(
            TemplateVariable.TITLE to "MI VIAJE A CANCÚN",
            TemplateVariable.LOCATION to "Cancún, México",
            TemplateVariable.DATE to "Agosto 2026"
        )

        val updatedProject = travelTemplate!!.applyTemplate(sampleProject, variables)

        // Validate project state
        assertNotEquals(sampleProject.title, updatedProject.title)
        assertEquals(1, updatedProject.pages.size)

        val firstPage = updatedProject.pages[0]
        assertEquals("4:5", firstPage.aspectRatio)

        // Validate layers preservation & filter application
        val imageLayer = firstPage.layers.filterIsInstance<CreativeLayer.Image>().firstOrNull()
        assertNotNull("La capa de imagen original debe conservarse", imageLayer)
        assertEquals(FilterPreset.OCEAN_CINEMATIC.filterName, imageLayer!!.filterName)

        // Validate text layers created with variable values
        val textLayers = firstPage.layers.filterIsInstance<CreativeLayer.Text>()
        assertTrue("Deben generarse capas de texto para el título y subtítulo", textLayers.size >= 2)

        val titleTextLayer = textLayers.firstOrNull { it.id.contains("title") }
        assertNotNull(titleTextLayer)
        assertEquals("MI VIAJE A CANCÚN", titleTextLayer!!.text)

        val subtitleTextLayer = textLayers.firstOrNull { it.id.contains("sub") }
        assertNotNull(subtitleTextLayer)
        assertTrue(subtitleTextLayer!!.text.contains("Cancún, México"))
        assertTrue(subtitleTextLayer.text.contains("Agosto 2026"))
    }

    @Test
    fun testTemplateCategoryFiltering() {
        val businessTemplates = PostTemplateEngine.getTemplatesByCategory(TemplateCategory.BUSINESS)
        assertTrue("Debe existir al menos una plantilla de categoría BUSINESS", businessTemplates.isNotEmpty())
        assertEquals(TemplateCategory.BUSINESS, businessTemplates.first().category)

        val influencerTemplates = PostTemplateEngine.getTemplatesByCategory(TemplateCategory.INFLUENCER)
        assertTrue("Debe existir al menos una plantilla de categoría INFLUENCER", influencerTemplates.isNotEmpty())
    }

    @Test
    fun testTemplateVariableFallbackPlaceholders() {
        val travelTemplate = PostTemplateEngine.findTemplateById("template_travel_cinematic")!!
        // Pass empty map -> defaults should be filled
        val updatedProject = travelTemplate.applyTemplate(sampleProject, emptyMap())
        val textLayers = updatedProject.pages[0].layers.filterIsInstance<CreativeLayer.Text>()

        val titleText = textLayers.first { it.id.contains("title") }.text
        assertEquals(TemplateVariable.TITLE.defaultLabel, titleText)
    }

    @Test
    fun testUndoRedoCompatibilityOnProject() {
        val template = PostTemplateEngine.availableTemplates.first()
        val modifiedProject = template.applyTemplate(sampleProject, emptyMap())

        // Ensure original sampleProject is unmodified (immutable data structure)
        assertEquals("Proyecto de Prueba", sampleProject.title)
        assertNotEquals(sampleProject.title, modifiedProject.title)
        assertNotEquals(sampleProject.pages[0].layers.size, modifiedProject.pages[0].layers.size)
    }
}
