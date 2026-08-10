package com.example.creative.ai.layout

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage

data class LayoutSuggestion(
    val title: String,
    val description: String,
    val applyAction: (PostPage) -> PostPage
)

/**
 * P6.6.4 - Smart Layout Engine
 * Analyzes layer placement, text boundaries and photo framing, suggesting auto-centering and rule of thirds.
 */
object SmartLayoutEngine {

    fun analyzePage(page: PostPage): List<LayoutSuggestion> {
        val suggestions = mutableListOf<LayoutSuggestion>()

        val textLayers = page.layers.filterIsInstance<CreativeLayer.Text>()
        val mediaLayers = page.layers.filter { it is CreativeLayer.Image || it is CreativeLayer.Video }

        // Check text off-center or overlapping edges
        textLayers.forEach { text ->
            if (text.yFraction < 0.15f || text.yFraction > 0.85f) {
                suggestions.add(
                    LayoutSuggestion(
                        title = "Optimizar posición de texto",
                        description = "Ajustar texto a zona segura con margen adecuado."
                    ) { p ->
                        val updated = p.layers.map { l ->
                            if (l.id == text.id) {
                                (l as CreativeLayer.Text).copy(yFraction = 0.25f, xFraction = 0.5f)
                            } else l
                        }
                        p.copy(layers = updated)
                    }
                )
            }
        }

        // Rule of thirds for stickers / overlays
        val stickers = page.layers.filterIsInstance<CreativeLayer.Sticker>()
        if (stickers.isNotEmpty() && mediaLayers.isNotEmpty()) {
            suggestions.add(
                LayoutSuggestion(
                    title = "Aplicar Regla de Tercios",
                    description = "Posicionar stickers en las intersecciones estéticas."
                ) { p ->
                    val updated = p.layers.mapIndexed { idx, l ->
                        if (l is CreativeLayer.Sticker) {
                            l.copy(xFraction = 0.75f, yFraction = 0.25f + (idx * 0.15f))
                        } else l
                    }
                    p.copy(layers = updated)
                }
            )
        }

        // Auto-center single media
        if (mediaLayers.size == 1) {
            val media = mediaLayers.first()
            if (media.scale != 1.0f || media.rotation != 0f) {
                suggestions.add(
                    LayoutSuggestion(
                        title = "Reencuadre Pro",
                        description = "Restablecer alineación central de la imagen/video principal."
                    ) { p ->
                        val updated = p.layers.map { l ->
                            when (l) {
                                is CreativeLayer.Image -> l.copy(xFraction = 0.5f, yFraction = 0.5f, scale = 1.0f, rotation = 0f)
                                is CreativeLayer.Video -> l.copy(xFraction = 0.5f, yFraction = 0.5f, scale = 1.0f, rotation = 0f)
                                else -> l
                            }
                        }
                        p.copy(layers = updated)
                    }
                )
            }
        }

        return suggestions
    }
}
