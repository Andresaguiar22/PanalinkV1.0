package com.example.creative.ai

import android.graphics.PointF

enum class FaceFeatureLandmark {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE_TIP,
    MOUTH_CENTER,
    FOREHEAD,
    HEAD_CENTER
}

data class TrackedFaceFrame(
    val frameIndex: Long,
    val timeUs: Long,
    val landmarks: Map<FaceFeatureLandmark, PointF>,
    val headYaw: Float = 0f,
    val headPitch: Float = 0f,
    val headRoll: Float = 0f,
    val boundingBoxWidth: Float = 0f,
    val boundingBoxHeight: Float = 0f
)

data class LayerFaceAttachment(
    val layerId: String,
    val targetLandmark: FaceFeatureLandmark,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scaleWithFace: Boolean = true,
    val rotateWithHead: Boolean = true
)

object FaceTracker {

    fun calculateLayerTransform(
        attachment: LayerFaceAttachment,
        trackedFrame: TrackedFaceFrame,
        canvasWidth: Float,
        canvasHeight: Float
    ): PointF? {
        val landmarkPos = trackedFrame.landmarks[attachment.targetLandmark] ?: return null
        val posX = landmarkPos.x + attachment.offsetX
        val posY = landmarkPos.y + attachment.offsetY
        return PointF(posX, posY)
    }

    fun generateInterpolatedTrackSequence(
        startFrame: TrackedFaceFrame,
        endFrame: TrackedFaceFrame,
        targetTimeUs: Long
    ): TrackedFaceFrame {
        val duration = (endFrame.timeUs - startFrame.timeUs).coerceAtLeast(1L)
        val fraction = ((targetTimeUs - startFrame.timeUs).toFloat() / duration).coerceIn(0f, 1f)

        val interpolatedLandmarks = mutableMapOf<FaceFeatureLandmark, PointF>()
        FaceFeatureLandmark.values().forEach { landmark ->
            val p1 = startFrame.landmarks[landmark]
            val p2 = endFrame.landmarks[landmark]
            if (p1 != null && p2 != null) {
                val ix = p1.x + (p2.x - p1.x) * fraction
                val iy = p1.y + (p2.y - p1.y) * fraction
                interpolatedLandmarks[landmark] = PointF(ix, iy)
            }
        }

        return TrackedFaceFrame(
            frameIndex = startFrame.frameIndex,
            timeUs = targetTimeUs,
            landmarks = interpolatedLandmarks,
            headYaw = startFrame.headYaw + (endFrame.headYaw - startFrame.headYaw) * fraction,
            headRoll = startFrame.headRoll + (endFrame.headRoll - startFrame.headRoll) * fraction
        )
    }
}
