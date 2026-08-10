package com.example.creative.ai

import android.graphics.PointF
import com.example.PanaApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = PanaApplication::class)
class FaceTrackerTest {

    @Test
    fun testLayerAttachmentCalculation() {
        val frame = TrackedFaceFrame(
            frameIndex = 1L,
            timeUs = 1000L,
            landmarks = mapOf(
                FaceFeatureLandmark.HEAD_CENTER to PointF(500f, 500f)
            )
        )

        val attachment = LayerFaceAttachment(
            layerId = "layer_hat",
            targetLandmark = FaceFeatureLandmark.HEAD_CENTER,
            offsetY = -100f
        )

        val transform = FaceTracker.calculateLayerTransform(attachment, frame, 1080f, 1920f)
        assertNotNull(transform)
        assertEquals(500f, transform?.x ?: 0f, 0.1f)
        assertEquals(400f, transform?.y ?: 0f, 0.1f)
    }
}

