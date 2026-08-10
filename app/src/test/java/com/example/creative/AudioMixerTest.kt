package com.example.creative.video

import org.junit.Assert.*
import org.junit.Test

class AudioMixerTest {

    @Test
    fun testAudioTrackConfig() {
        val config = AudioTrackConfig(
            audioPath = "/music/song.mp3",
            volume = 0.7f,
            startOffsetMs = 2000L,
            durationMs = 10000L,
            fadeInMs = 1000L,
            fadeOutMs = 1000L
        )

        assertEquals("/music/song.mp3", config.audioPath)
        assertEquals(0.7f, config.volume, 0.01f)
        assertEquals(2000L, config.startOffsetMs)
        assertEquals(10000L, config.durationMs)
    }

    @Test
    fun testSpeedControllerCalculation() {
        val durationAt1x = 10000L
        val durationAt2x = VideoSpeedController.calculateDurationWithSpeed(durationAt1x, 2.0f)
        val durationAtHalfX = VideoSpeedController.calculateDurationWithSpeed(durationAt1x, 0.5f)

        assertEquals(5000L, durationAt2x)
        assertEquals(20000L, durationAtHalfX)
    }
}
