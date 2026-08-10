package com.example.creative

import com.example.PanaApplication
import com.example.creative.crop.CropState
import com.example.creative.timeline.TimelineClip
import com.example.creative.timeline.TimelineController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = PanaApplication::class)
class CanvasEditorEngineTest {

    @Test
    fun testCropStateDefaults() {
        val cropState = CropState()
        assertEquals(0f, cropState.rotationDegrees, 0.01f)
        assertEquals(0f, cropState.cropRectFraction.left, 0.01f)
        assertEquals(1f, cropState.cropRectFraction.right, 0.01f)
        assertFalse(cropState.isCircular)
    }

    @Test
    fun testTimelineControllerCalculations() {
        val controller = TimelineController()
        val clip1 = TimelineClip(id = "c1", mediaUriOrPath = "/v1.mp4", durationMs = 5000L, speed = 1.0f)
        val clip2 = TimelineClip(id = "c2", mediaUriOrPath = "/v2.mp4", durationMs = 10000L, speed = 2.0f)

        controller.setInitialClip(clip1)
        controller.addClip(clip2)

        assertEquals(2, controller.clips.size)
        // Clip 1: 5000ms / 1.0 = 5000ms
        // Clip 2: 10000ms / 2.0 = 5000ms
        // Total: 10000ms
        assertEquals(10000L, controller.getTotalDurationMs())
    }
}
