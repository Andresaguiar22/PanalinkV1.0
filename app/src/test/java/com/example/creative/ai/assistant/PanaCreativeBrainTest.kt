package com.example.creative.ai.assistant

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import com.example.creative.templates.TemplateCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PanaCreativeBrainTest {

    private lateinit var testProject: PostStudioProject
    private lateinit var testPage: PostPage

    @Before
    fun setUp() {
        testPage = PostPage(
            id = "page_test_1",
            aspectRatio = "4:5",
            layers = listOf(
                CreativeLayer.Image(
                    id = "img_travel",
                    imageUriOrPath = "content://media/beach_sunset.jpg",
                    xFraction = 0.5f,
                    yFraction = 0.5f
                ),
                CreativeLayer.Text(
                    id = "txt_hook",
                    text = "VACACIONES EN CASCADAS",
                    colorHex = "#FFFFFF",
                    fontFamily = "SansSerif",
                    fontSizeSp = 28f,
                    xFraction = 0.5f,
                    yFraction = 0.2f
                )
            )
        )

        testProject = PostStudioProject(
            id = "project_test_brain",
            title = "Viaje Playa 2026",
            caption = "Un viaje inolvidable a la playa con la mejor vibra de verano.",
            hashtags = listOf("Viajes", "Playa", "PanaLink"),
            pages = listOf(testPage)
        )
    }

    @Test
    fun testOrchestrationGeneratesCompleteSuggestions() {
        val result = PanaCreativeBrain.generateCreativeBrainSuggestions(testProject, testPage)

        assertNotNull("La plantilla recomendada no debe ser nula", result.recommendedTemplate)
        assertEquals(TemplateCategory.TRAVEL, result.recommendedTemplate.category)

        assertTrue("Deben generarse sugerencias de caption", result.suggestedCaptions.isNotEmpty())
        assertNotNull("Debe devolverse análisis de armonía de color", result.colorHarmony)
        assertTrue("El reporte viral debe calcular un score válido", result.viralReport.totalScore in 30..100)
        assertTrue("El reporte de calidad debe tener puntuación", result.qualityReport.overallScorePercent in 50..100)
    }
}
