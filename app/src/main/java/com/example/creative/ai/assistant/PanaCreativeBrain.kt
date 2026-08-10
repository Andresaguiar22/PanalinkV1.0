package com.example.creative.ai.assistant

import com.example.creative.ai.color.ColorHarmonyEngine
import com.example.creative.ai.color.ColorHarmonyResult
import com.example.creative.ai.layout.LayoutSuggestion
import com.example.creative.ai.layout.SmartLayoutEngine
import com.example.creative.ai.quality.PostQualityAnalyzer
import com.example.creative.ai.quality.PostQualityReport
import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import com.example.creative.templates.AnimationPreset
import com.example.creative.templates.PostTemplate
import com.example.creative.templates.PostTemplateEngine

data class CreativeSuggestionResult(
    val recommendedTemplate: PostTemplate,
    val suggestedCaptions: List<GeneratedCaption>,
    val colorHarmony: ColorHarmonyResult,
    val layoutSuggestions: List<LayoutSuggestion>,
    val qualityReport: PostQualityReport,
    val viralReport: ViralCheckResult,
    val suggestedAnimation: AnimationPreset
)

/**
 * P6.6.5 - PanaCreative AI Brain
 * Master Orchestrator coordinating Content Understanding, Color Harmony, Layout, Captions, Quality, and Viral Score.
 */
object PanaCreativeBrain {

    fun generateCreativeBrainSuggestions(
        project: PostStudioProject,
        currentPage: PostPage
    ): CreativeSuggestionResult {
        val mainLayer = currentPage.getMainMediaLayer()
        val mediaPath = when (mainLayer) {
            is CreativeLayer.Image -> mainLayer.imageUriOrPath
            is CreativeLayer.Video -> mainLayer.videoUriOrPath
            else -> ""
        }

        // 1. Analyze media content
        val contentAnalysis = ContentUnderstandingEngine.analyzeMediaAndCaption(mediaPath, project.caption)

        // 2. Select best template for category
        val categoryTemplates = PostTemplateEngine.getTemplatesByCategory(contentAnalysis.detectedCategory)
        val recommendedTemplate = categoryTemplates.firstOrNull() ?: PostTemplateEngine.availableTemplates.first()

        // 3. Color Harmony
        val colorHarmony = ColorHarmonyEngine.analyzeImagePalette(mediaPath)

        // 4. Layout Suggestions
        val layoutSuggestions = SmartLayoutEngine.analyzePage(currentPage)

        // 5. Quality Report
        val qualityReport = PostQualityAnalyzer.analyzeProject(project)

        // 6. Viral Score
        val viralReport = ViralScoreAnalyzer.calculateViralScore(project)

        // 7. Generated Captions
        val captions = SmartCaptionGenerator.generateCaptions(project.caption, contentAnalysis.detectedCategory.displayName)

        return CreativeSuggestionResult(
            recommendedTemplate = recommendedTemplate,
            suggestedCaptions = captions,
            colorHarmony = colorHarmony,
            layoutSuggestions = layoutSuggestions,
            qualityReport = qualityReport,
            viralReport = viralReport,
            suggestedAnimation = recommendedTemplate.style.animation
        )
    }
}
