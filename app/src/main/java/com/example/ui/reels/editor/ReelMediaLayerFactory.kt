package com.example.ui.reels.editor

import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelLayerContent
import com.example.ui.reels.editor.model.ReelTrackType

/** Pure factories for non-destructive visual media layers. */
object ReelMediaLayerFactory {
    fun image(uri: String, startTimeMs: Long, endTimeMs: Long, zIndex: Int = 120): ReelLayer =
        ReelLayer(
            id = "image_${System.nanoTime()}",
            type = ReelTrackType.IMAGE,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs.coerceAtLeast(startTimeMs + 1L),
            zIndex = zIndex,
            x = 0.5f,
            y = 0.5f,
            content = ReelLayerContent.Image(uri)
        )

    fun sticker(uri: String, startTimeMs: Long, endTimeMs: Long, zIndex: Int = 130): ReelLayer =
        ReelLayer(
            id = "sticker_${System.nanoTime()}",
            type = ReelTrackType.STICKER,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs.coerceAtLeast(startTimeMs + 1L),
            zIndex = zIndex,
            x = 0.5f,
            y = 0.5f,
            content = ReelLayerContent.Sticker(uri)
        )
}