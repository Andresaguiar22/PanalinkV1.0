package com.example.creative.video

import android.content.Context
import com.example.creative.core.CreativeProject
import com.example.creative.timeline.TimelineClip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object VideoExportManager {

    fun exportProject(
        context: Context,
        project: CreativeProject,
        qualityProfile: VideoQualityProfile = VideoQualityProfile.REEL_PRO
    ): Flow<VideoRenderState> = flow {
        val clip = TimelineClip(
            id = "clip_main",
            mediaUriOrPath = project.sourceMedia
        )

        val composition = VideoComposition(
            clips = listOf(clip),
            textLayers = project.layers.filterIsInstance<com.example.creative.core.CreativeLayer.Text>(),
            stickerLayers = project.layers.filterIsInstance<com.example.creative.core.CreativeLayer.Sticker>(),
            drawingLayers = project.layers.filterIsInstance<com.example.creative.core.CreativeLayer.Drawing>(),
            filterName = project.layers.filterIsInstance<com.example.creative.core.CreativeLayer.Filter>().firstOrNull()?.filterName ?: "none"
        )

        VideoProcessor.processVideoComposition(context, composition, qualityProfile).collect { state ->
            emit(state)
        }
    }
}
