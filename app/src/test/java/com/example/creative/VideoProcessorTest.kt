package com.example.creative.video

import com.example.creative.timeline.TimelineClip
import org.junit.Assert.*
import org.junit.Test

class VideoProcessorTest {

    @Test
    fun testVideoCompositionBuilder() {
        val clip = TimelineClip(id = "clip1", mediaUriOrPath = "/storage/test.mp4", durationMs = 15000L)
        val composition = VideoComposition(
            clips = listOf(clip),
            filterName = "cinematic",
            playbackSpeed = 1.5f,
            originalVideoVolume = 0.8f
        )

        assertEquals(1, composition.clips.size)
        assertEquals("cinematic", composition.filterName)
        assertEquals(1.5f, composition.playbackSpeed, 0.01f)
        assertEquals(0.8f, composition.originalVideoVolume, 0.01f)
    }

    @Test
    fun testQualityProfileValues() {
        val reelProfile = VideoQualityProfile.REEL_PRO
        assertEquals(1080, reelProfile.width)
        assertEquals(1920, reelProfile.height)
        assertEquals(12_000_000, reelProfile.bitrate)
    }
}
