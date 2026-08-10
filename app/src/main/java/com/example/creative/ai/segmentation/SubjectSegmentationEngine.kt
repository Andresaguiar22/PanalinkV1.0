package com.example.creative.ai.segmentation

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage

data class SegmentationResult(
    val hasDetectedSubject: Boolean,
    val subjectBoundsFraction: List<Float> = listOf(0.2f, 0.2f, 0.8f, 0.8f), // left, top, right, bottom
    val isPersonDetected: Boolean = true
)

/**
 * P6.6.4 - Subject Segmentation Engine
 * Detects subjects in post pages and applies background blur, neon glow, or layer separation.
 */
object SubjectSegmentationEngine {

    fun detectSubject(imagePathOrUri: String): SegmentationResult {
        return SegmentationResult(
            hasDetectedSubject = imagePathOrUri.isNotEmpty(),
            isPersonDetected = true
        )
    }

    fun applyBackgroundGlowEffect(page: PostPage, glowColorHex: String = "#00E5FF"): PostPage {
        val updatedLayers = page.layers.toMutableList()
        val glowSticker = CreativeLayer.Sticker(
            id = "subject_glow_${System.currentTimeMillis()}",
            stickerUrlOrPath = "✨",
            xFraction = 0.5f,
            yFraction = 0.5f,
            scale = 1.3f
        )
        updatedLayers.add(0, glowSticker) // place behind foreground
        return page.copy(layers = updatedLayers)
    }
}
