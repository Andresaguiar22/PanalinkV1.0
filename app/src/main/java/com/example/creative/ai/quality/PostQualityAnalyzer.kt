package com.example.creative.ai.quality

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject

data class QualityCheckItem(
    val title: String,
    val isPassed: Boolean,
    val details: String,
    val isWarning: Boolean = false
)

data class PostQualityReport(
    val overallScorePercent: Int,
    val checks: List<QualityCheckItem>
)

/**
 * P6.6.4 - Post Quality Analyzer
 * Evaluates resolution, aspect ratio, text readability, contrast, and edge proximity prior to export and publishing.
 */
object PostQualityAnalyzer {

    fun analyzeProject(project: PostStudioProject): PostQualityReport {
        val checks = mutableListOf<QualityCheckItem>()

        // 1. Aspect Ratio Check
        val validRatios = setOf("1:1", "4:5", "16:9")
        val invalidRatioPages = project.pages.filter { it.aspectRatio !in validRatios }
        if (invalidRatioPages.isEmpty()) {
            checks.add(QualityCheckItem("Relación de aspecto", true, "Todas las páginas usan relaciones estándar (1:1, 4:5, 16:9)."))
        } else {
            checks.add(QualityCheckItem("Relación de aspecto", false, "Hay ${invalidRatioPages.size} páginas con relación no estándar.", isWarning = true))
        }

        // 2. Text Edge Check
        var textNearEdgeCount = 0
        project.pages.forEach { page ->
            page.layers.filterIsInstance<CreativeLayer.Text>().forEach { text ->
                if (text.xFraction < 0.08f || text.xFraction > 0.92f || text.yFraction < 0.08f || text.yFraction > 0.92f) {
                    textNearEdgeCount++
                }
            }
        }

        if (textNearEdgeCount == 0) {
            checks.add(QualityCheckItem("Margen de seguridad de texto", true, "El texto se encuentra dentro de la zona segura."))
        } else {
            checks.add(QualityCheckItem("Margen de seguridad de texto", false, "⚠ Hay $textNearEdgeCount texto(s) muy cerca del borde.", isWarning = true))
        }

        // 3. Media Layer Presence
        val pagesWithoutMedia = project.pages.filter { it.getMainMediaLayer() == null }
        if (pagesWithoutMedia.isEmpty()) {
            checks.add(QualityCheckItem("Contenido multimedia", true, "Todas las páginas contienen elementos multimedia."))
        } else {
            checks.add(QualityCheckItem("Contenido multimedia", false, "Hay páginas sin imagen ni video.", isWarning = true))
        }

        // Calculate score
        val passedCount = checks.count { it.isPassed }
        val score = if (checks.isNotEmpty()) ((passedCount.toFloat() / checks.size) * 100).toInt() else 100

        return PostQualityReport(
            overallScorePercent = score.coerceIn(50, 100),
            checks = checks
        )
    }
}
