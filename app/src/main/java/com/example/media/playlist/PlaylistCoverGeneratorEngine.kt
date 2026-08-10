package com.example.media.playlist

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import java.util.UUID

/**
 * P6.7 - Playlist Cover Generator Engine
 * Reuses Creative Engine (CreativeProject & CreativeLayer) to auto-generate beautiful playlist, album, and artist cover art.
 */
object PlaylistCoverGeneratorEngine {

    fun generatePlaylistCoverProject(
        title: String,
        description: String? = null,
        themeColorHex: String = "#FF007A"
    ): CreativeProject {
        val titleTextLayer = CreativeLayer.Text(
            id = "txt_title_${UUID.randomUUID()}",
            text = title.uppercase(),
            colorHex = "#FFFFFF",
            fontSizeSp = 32f,
            fontFamily = "SansSerif",
            xFraction = 0.5f,
            yFraction = 0.5f,
            hasShadow = true
        )

        val descTextLayer = CreativeLayer.Text(
            id = "txt_desc_${UUID.randomUUID()}",
            text = description ?: "PLAYLIST OFICIAL PANALINK",
            colorHex = "#38BDF8",
            fontSizeSp = 14f,
            fontFamily = "SansSerif",
            xFraction = 0.5f,
            yFraction = 0.65f,
            hasShadow = false
        )

        return CreativeProject(
            id = "cover_proj_${UUID.randomUUID()}",
            sourceMedia = "cover_gradient_$themeColorHex",
            type = CreativeType.POST,
            layers = listOf(titleTextLayer, descTextLayer)
        )
    }
}
