package com.example.creative.ai.assistant

import com.example.creative.templates.TemplateCategory

data class MediaContentAnalysis(
    val detectedCategory: TemplateCategory,
    val isVideo: Boolean,
    val hasFaceOrSubject: Boolean,
    val dominantTone: String, // e.g. "Warm", "Cool", "Vivid", "Dark"
    val safeZonePassed: Boolean
)

/**
 * P6.6.5 - Content Understanding Engine
 * Analyzes media URI/path and text content to determine category, dominant tone, and layout recommendations.
 */
object ContentUnderstandingEngine {

    fun analyzeMediaAndCaption(mediaPathOrUri: String, currentCaption: String): MediaContentAnalysis {
        val lowerPath = mediaPathOrUri.lowercase()
        val lowerCaption = currentCaption.lowercase()

        val isVideo = lowerPath.endsWith(".mp4") || lowerPath.endsWith(".mov") || lowerPath.contains("video")

        val category = when {
            lowerPath.contains("beach") || lowerPath.contains("travel") || lowerCaption.contains("viaje") || lowerCaption.contains("playa") -> TemplateCategory.TRAVEL
            lowerPath.contains("gym") || lowerCaption.contains("fit") || lowerCaption.contains("deporte") -> TemplateCategory.SPORTS
            lowerPath.contains("code") || lowerCaption.contains("negocio") || lowerCaption.contains("business") -> TemplateCategory.BUSINESS
            lowerPath.contains("game") || lowerCaption.contains("stream") -> TemplateCategory.GAMING
            else -> TemplateCategory.INFLUENCER
        }

        val dominantTone = when {
            lowerPath.contains("sunset") || lowerPath.contains("warm") -> "Warm"
            lowerPath.contains("ocean") || lowerPath.contains("cool") -> "Cool"
            lowerPath.contains("night") || lowerPath.contains("neon") -> "Dark"
            else -> "Vivid"
        }

        return MediaContentAnalysis(
            detectedCategory = category,
            isVideo = isVideo,
            hasFaceOrSubject = mediaPathOrUri.isNotEmpty(),
            dominantTone = dominantTone,
            safeZonePassed = true
        )
    }
}
