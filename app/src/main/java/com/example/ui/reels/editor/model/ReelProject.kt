package com.example.ui.reels.editor.model

/** Root document for a Reel edit session. Keeps the project non-destructive. */
data class ReelProject(
    val id: String,
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val durationMs: Long = 0L,
    val backgroundColorArgb: Int = 0xFF000000.toInt(),
    val timeline: ReelTimeline = ReelTimeline(),
    val selectedLayerId: String? = null,
    val version: Int = 1
)
