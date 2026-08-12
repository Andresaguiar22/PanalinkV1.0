package com.example.ui.story

/** Shared content types supported by the Story Studio editor. */
data class StoryStudioSlide(
    val id: String,
    val kind: StoryStudioKind = StoryStudioKind.PHOTO,
    val uri: String? = null,
    val isVideo: Boolean = kind == StoryStudioKind.VIDEO,
    val text: String = "",
    val backgroundHex: String = "#111318",
    val durationMs: Long = when (kind) {
        StoryStudioKind.VIDEO -> 0L
        StoryStudioKind.TEXT -> 5000L
        StoryStudioKind.PHOTO -> 5000L
        StoryStudioKind.CAROUSEL -> 5000L
    }
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
