package com.example.creative.ai.animation

import com.example.creative.animation.AnimationTrack
import com.example.creative.animation.CreativeKeyframe
import com.example.creative.animation.PropertyTrack
import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage

enum class PostAnimationType {
    FADE_IN,
    SOFT_ZOOM,
    PARALLAX,
    KEN_BURNS
}

/**
 * P6.6.4 - Smart Animation Engine
 * Creates automatic keyframes and animation tracks (Ken Burns, Fade In, Soft Zoom, Parallax) for post layers.
 */
object SmartAnimationEngine {

    fun generateAnimationTrack(
        layerId: String,
        type: PostAnimationType,
        durationMs: Long = 1000L
    ): AnimationTrack {
        val opacityTrack = when (type) {
            PostAnimationType.FADE_IN -> PropertyTrack(
                propertyName = "opacity",
                keyframes = listOf(
                    CreativeKeyframe(id = "kf1_$layerId", timeMs = 0L, value = 0f),
                    CreativeKeyframe(id = "kf2_$layerId", timeMs = durationMs, value = 1f)
                )
            )
            else -> PropertyTrack("opacity")
        }

        val scaleTrack = when (type) {
            PostAnimationType.SOFT_ZOOM -> PropertyTrack(
                propertyName = "scale",
                keyframes = listOf(
                    CreativeKeyframe(id = "kf1_$layerId", timeMs = 0L, value = 0.8f),
                    CreativeKeyframe(id = "kf2_$layerId", timeMs = durationMs, value = 1.05f)
                )
            )
            PostAnimationType.KEN_BURNS -> PropertyTrack(
                propertyName = "scale",
                keyframes = listOf(
                    CreativeKeyframe(id = "kf1_$layerId", timeMs = 0L, value = 1.0f),
                    CreativeKeyframe(id = "kf2_$layerId", timeMs = durationMs / 2, value = 1.15f),
                    CreativeKeyframe(id = "kf3_$layerId", timeMs = durationMs, value = 1.05f)
                )
            )
            else -> PropertyTrack("scale")
        }

        return AnimationTrack(
            propertyTracks = mapOf(
                "opacity" to opacityTrack,
                "scale" to scaleTrack
            )
        )
    }

    fun applyPresetToPage(page: PostPage, animationType: PostAnimationType): PostPage {
        val updatedLayers = page.layers.map { layer ->
            when (layer) {
                is CreativeLayer.Image -> layer.copy(scale = if (animationType == PostAnimationType.SOFT_ZOOM) 1.05f else layer.scale)
                is CreativeLayer.Text -> layer.copy(scale = if (animationType == PostAnimationType.SOFT_ZOOM) 1.1f else layer.scale)
                else -> layer
            }
        }
        return page.copy(layers = updatedLayers)
    }
}

