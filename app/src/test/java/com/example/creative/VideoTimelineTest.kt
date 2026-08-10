package com.example.creative.video

import com.example.creative.timeline.TimelineClip
import com.example.creative.timeline.TimelineController
import org.junit.Assert.*
import org.junit.Test

class VideoTimelineTest {

    @Test
    fun testMultiClipTotalDuration() {
        val controller = TimelineController()
        val clip1 = TimelineClip(id = "c1", mediaUriOrPath = "/v1.mp4", durationMs = 6000L, speed = 1.0f)
        val clip2 = TimelineClip(id = "c2", mediaUriOrPath = "/v2.mp4", durationMs = 8000L, speed = 2.0f)

        controller.setInitialClip(clip1)
        controller.addClip(clip2)

        // clip1: 6000ms, clip2: 8000 / 2 = 4000ms -> Total = 10000ms
        assertEquals(10000L, controller.getTotalDurationMs())
    }
}
