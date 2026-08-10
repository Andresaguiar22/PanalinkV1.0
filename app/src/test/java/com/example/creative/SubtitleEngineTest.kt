package com.example.creative.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SubtitleEngineTest {

    @Test
    fun testTranscribeAudioTrack() = runBlocking {
        var progressValue = 0f
        val captions = SubtitleEngine.transcribeAudioTrack("/sample.mp4") { progress ->
            progressValue = progress
        }

        assertEquals(1.0f, progressValue, 0.01f)
        assertTrue(captions.isNotEmpty())
        assertEquals("¡Bienvenidos a PanaLink Creative Studio!", captions.first().text)
    }
}
