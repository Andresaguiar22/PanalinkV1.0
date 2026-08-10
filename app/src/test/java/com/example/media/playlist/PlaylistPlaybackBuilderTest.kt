package com.example.media.playlist

import androidx.test.core.app.ApplicationProvider
import com.example.media.audio.AudioPlayerEngine
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistPlaybackBuilderTest {

    @Test
    fun testPlayTracksInQueue(): Unit = runBlocking {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        val playerEngine = AudioPlayerEngine(context)
        
        val tracks = listOf(
            AudioTrackEntity(id = "1", userId = "u1", title = "T1", filePath = "/1.mp3"),
            AudioTrackEntity(id = "2", userId = "u1", title = "T2", filePath = "/2.mp3")
        )

        PlaylistPlaybackBuilder.playTracks(tracks, playerEngine, 1)

        val state = playerEngine.state.value
        assertEquals(2, state.queue.size)
        assertEquals("2", state.currentTrack?.id) // Starts at index 1
        
        playerEngine.release()
        Unit
    }
}
