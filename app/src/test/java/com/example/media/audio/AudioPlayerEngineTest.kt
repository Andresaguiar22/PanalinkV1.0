package com.example.media.audio

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioPlayerEngineTest {

    private lateinit var playerEngine: AudioPlayerEngine

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        playerEngine = AudioPlayerEngine(context)
    }

    @After
    fun tearDown() {
        playerEngine.release()
    }

    @Test
    fun testQueueAndNavigation() = runBlocking {
        val track1 = AudioTrackEntity(id = "1", userId = "u1", title = "Pana 1", filePath = "/tmp/1.mp3")
        val track2 = AudioTrackEntity(id = "2", userId = "u1", title = "Pana 2", filePath = "/tmp/2.mp3")

        playerEngine.setQueueAndPlay(listOf(track1, track2), 0)

        var state = playerEngine.state.value
        assertEquals(2, state.queue.size)
        assertEquals("1", state.currentTrack?.id)
        assertTrue(state.isPlaying)

        // Next track
        playerEngine.nextTrack()
        state = playerEngine.state.value
        assertEquals("2", state.currentTrack?.id)

        // Toggle repeat & shuffle
        playerEngine.toggleRepeat()
        assertEquals(RepeatMode.ALL, playerEngine.state.value.repeatMode)

        playerEngine.toggleShuffle()
        assertTrue(playerEngine.state.value.isShuffle)

        // Pause
        playerEngine.pause()
        assertFalse(playerEngine.state.value.isPlaying)
    }
}
