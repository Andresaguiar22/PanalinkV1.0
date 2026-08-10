package com.example.creative.ai.assistant

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import org.junit.Assert.*
import org.junit.Test

class ViralScoreAnalyzerTest {

    @Test
    fun testViralScoreIncreasesWithHookAndCarousel() {
        val page1 = PostPage(
            id = "p1",
            layers = listOf(
                CreativeLayer.Text(id = "txt1", text = "HOOK PRINCIPAL", colorHex = "#FFFFFF")
            )
        )
        val page2 = PostPage(id = "p2", layers = emptyList())

        val richProject = PostStudioProject(
            id = "proj_rich",
            title = "Carrusel Viral",
            caption = "Un texto de prueba lo suficientemente largo para fomentar interacción.",
            hashtags = listOf("Viral", "PanaLink", "Creative"),
            pages = listOf(page1, page2)
        )

        val emptyProject = PostStudioProject(
            id = "proj_empty",
            title = "Borrador Simple",
            pages = listOf(PostPage(id = "p_alone", layers = emptyList()))
        )

        val richResult = ViralScoreAnalyzer.calculateViralScore(richProject)
        val emptyResult = ViralScoreAnalyzer.calculateViralScore(emptyProject)

        assertTrue("El proyecto optimizado debe tener un score mayor que el proyecto vacío", richResult.totalScore > emptyResult.totalScore)
        assertEquals(90, richResult.hookScore)
        assertTrue("Debe incluir recomendaciones explicativas", richResult.recommendations.isNotEmpty())
    }
}
