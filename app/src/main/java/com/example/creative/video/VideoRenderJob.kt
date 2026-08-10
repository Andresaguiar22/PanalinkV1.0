package com.example.creative.video

import java.io.File

sealed class VideoRenderState {
    object Idle : VideoRenderState()
    data class Processing(val progress: Float) : VideoRenderState()
    data class Success(val exportedFile: File, val durationMs: Long) : VideoRenderState()
    data class Error(val message: String, val cause: Throwable? = null) : VideoRenderState()
}

data class VideoRenderJob(
    val id: String,
    val composition: VideoComposition,
    val qualityProfile: VideoQualityProfile = VideoQualityProfile.REEL_PRO,
    val state: VideoRenderState = VideoRenderState.Idle
)
