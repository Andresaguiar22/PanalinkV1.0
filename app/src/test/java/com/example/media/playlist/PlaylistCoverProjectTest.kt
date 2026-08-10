package com.example.media.playlist

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class PlaylistCoverProjectTest {

    @Test
    fun testPlaylistCoverProjectCreation() {
        val playlistName = "Mi Playlist Top"
        val project = CreativeProject(
            id = UUID.randomUUID().toString(),
            sourceMedia = "",
            type = CreativeType.PLAYLIST_COVER,
            layers = listOf(
                CreativeLayer.Text(
                    id = UUID.randomUUID().toString(),
                    text = playlistName
                )
            )
        )

        assertEquals(CreativeType.PLAYLIST_COVER, project.type)
        val textLayer = project.layers.first() as CreativeLayer.Text
        assertEquals(playlistName, textLayer.text)
    }
}
