package com.example.media.playlist.cover

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject

data class CoverSuggestion(
    val id: String,
    val name: String,
    val description: String,
    val layers: List<CreativeLayer>,
    val colorHex: String,
    val tag: String
)

/**
 * P6.7.4 - Playlist Cover Creative Brain
 * Offline-first design intelligence for professional covers.
 */
object PlaylistCoverBrain {

    val availableTemplates = listOf(
        CoverSuggestion(
            id = "cinematic",
            name = "Cinemática",
            description = "Filtro dramático y tipografía serif elegante.",
            tag = "Premium",
            colorHex = "#111827",
            layers = listOf(
                CreativeLayer.Filter(id = "f1", filterName = "cinematic", intensity = 0.8f)
            )
        ),
        CoverSuggestion(
            id = "minimal",
            name = "Minimalista",
            description = "Limpio, con mucho espacio negativo.",
            tag = "Clean",
            colorHex = "#F3F4F6",
            layers = listOf()
        ),
        CoverSuggestion(
            id = "neon",
            name = "Neon Night",
            description = "Colores vibrantes y efectos de brillo.",
            tag = "Electronic",
            colorHex = "#9333EA",
            layers = listOf(
                CreativeLayer.Filter(id = "f1", filterName = "neon", intensity = 1.0f)
            )
        ),
        CoverSuggestion(
            id = "retro",
            name = "Retro Vinyl",
            description = "Estilo clásico de disco de vinilo.",
            tag = "Vintage",
            colorHex = "#D97706",
            layers = listOf()
        )
    )

    fun getSuggestionsForPlaylist(name: String): List<CoverSuggestion> {
        // Logic to select templates based on name keywords
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("relax") || nameLower.contains("chill") -> availableTemplates.filter { it.id == "minimal" || it.id == "cinematic" }
            nameLower.contains("party") || nameLower.contains("dance") -> availableTemplates.filter { it.id == "neon" }
            nameLower.contains("old") || nameLower.contains("classic") -> availableTemplates.filter { it.id == "retro" }
            else -> availableTemplates
        }
    }
}
