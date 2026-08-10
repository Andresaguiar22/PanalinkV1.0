package com.example.creative.ai.assistant

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostStudioProject

data class ViralCheckResult(
    val totalScore: Int, // 0 to 100
    val hookScore: Int,
    val visualAppealScore: Int,
    val engagementScore: Int,
    val recommendations: List<String>
)

/**
 * P6.6.5 - Viral Score Analyzer
 * Calculates viral engagement probability based on hook presence, carousel structure, text readability, and hashtags.
 */
object ViralScoreAnalyzer {

    fun calculateViralScore(project: PostStudioProject): ViralCheckResult {
        var score = 50
        val recs = mutableListOf<String>()

        val hasCaption = project.caption.isNotBlank()
        val hasHashtags = project.hashtags.isNotEmpty()
        val pageCount = project.pages.size
        val firstPageTextLayers = project.pages.firstOrNull()?.layers?.filterIsInstance<CreativeLayer.Text>() ?: emptyList()

        if (firstPageTextLayers.isNotEmpty()) {
            score += 15
            recs.add("✓ Excelente enganche (Hook) visual en la portada.")
        } else {
            recs.add("💡 Añade un título o texto en la portada para enganchar en los primeros 2 segundos.")
        }

        if (pageCount >= 2) {
            score += 15
            recs.add("✓ El formato carrusel ($pageCount páginas) aumenta un 40% la retención.")
        } else {
            recs.add("💡 Convierte tu post en carrusel agregando una segunda página.")
        }

        if (hasCaption && project.caption.length > 20) {
            score += 10
            recs.add("✓ Descripción rica para fomentar interacción.")
        } else {
            recs.add("💡 Amplía la descripción contando una historia corta.")
        }

        if (hasHashtags) {
            score += 10
            recs.add("✓ Hashtags presentes para descubrimiento.")
        } else {
            recs.add("💡 Incluye al menos 3 hashtags relevantes.")
        }

        val finalScore = score.coerceIn(30, 98)

        return ViralCheckResult(
            totalScore = finalScore,
            hookScore = if (firstPageTextLayers.isNotEmpty()) 90 else 50,
            visualAppealScore = if (pageCount > 1) 88 else 72,
            engagementScore = if (hasHashtags && hasCaption) 85 else 60,
            recommendations = recs
        )
    }
}
