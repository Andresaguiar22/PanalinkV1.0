package com.example.ui.story

/**
 * Small, explicit model used by the new Story Studio.
 * A story is one publication containing one or more slides and an optional soundtrack.
 */
data class StoryStudioSlide(
    val id: String,
    val uri: String? = null,
    val isVideo: Boolean = false,
    val text: String = "",
    val backgroundHex: String = "#111318",
    val durationMs: Long = if (isVideo) 0L else 5000L
)

data class StoryStudioDraft(
    val id: String,
    val slides: List<StoryStudioSlide> = emptyList(),
    val audioUri: String? = null,
    val audioStartMs: Long = 0L,
    val audioDurationMs: Long = 0L
) {
    companion object {
        const val MAX_DURATION_MS = 120_000L
        const val MAX_SLIDES = 10
    }

    fun durationMs(): Long = slides.sumOf { it.durationMs.coerceAtLeast(0L) }

    fun isValid(): Boolean = slides.isNotEmpty() && durationMs() in 1..MAX_DURATION_MS
}
