package com.example.media.playlist

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class PlaylistCoverGeneratorTest {

    @Test
    fun testCoverGeneratorCreatesCreativeProjectWithLayers() {
        val title = "Vibra Verano"
        val desc = "Canciones para la playa"
        val project = PlaylistCoverGeneratorEngine.generatePlaylistCoverProject(title, desc)

        assertNotNull(project)
        assertEquals(2, project.layers.size)

        val titleLayer = project.layers.filterIsInstance<CreativeLayer.Text>().firstOrNull { it.text.contains("VIBRA VERANO") }
        assertNotNull("El título de la playlist debe estar formateado como texto en la portada", titleLayer)
    }
}
