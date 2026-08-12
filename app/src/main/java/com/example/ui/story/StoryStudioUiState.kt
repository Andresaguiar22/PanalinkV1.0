package com.example.ui.story

import android.net.Uri
import java.util.UUID

enum class StoryStudioKind { PHOTO, VIDEO, TEXT, CAROUSEL }

data class StoryStudioItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val kind: StoryStudioKind,
    val text: String = "",
    val background: Long = 0xFF111318,
    val durationMs: Long = 5_000L
)

data class StoryStudioUiState(
    val items: List<StoryStudioItem> = emptyList(),
    val audioUri: Uri? = null,
    val audioDurationMs: Long = 0L,
    val selectedIndex: Int = 0,
    val isPublishing: Boolean = false,
    val progress: Int = 0,
    val error: String? = null
) {
    companion object {
        const val MAX_DURATION_MS = 120_000L
        const val MAX_ITEMS = 10
    }

    val totalDurationMs: Long
        get() = items.sumOf { it.durationMs.coerceAtLeast(0L) }

    val canPublish: Boolean
        get() = items.isNotEmpty() && totalDurationMs in 1..MAX_DURATION_MS && !isPublishing

    fun withError(message: String) = copy(error = message)
}
