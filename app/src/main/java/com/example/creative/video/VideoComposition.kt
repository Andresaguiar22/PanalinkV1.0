package com.example.creative.video

import com.example.creative.core.CreativeLayer
import com.example.creative.timeline.TimelineClip

data class VideoComposition(
    val clips: List<TimelineClip> = emptyList(),
    val textLayers: List<CreativeLayer.Text> = emptyList(),
    val stickerLayers: List<CreativeLayer.Sticker> = emptyList(),
    val drawingLayers: List<CreativeLayer.Drawing> = emptyList(),
    val filterName: String = "none",
    val filterIntensity: Float = 1.0f,
    val backgroundAudioPath: String? = null,
    val backgroundAudioVolume: Float = 1.0f,
    val originalVideoVolume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920
)
