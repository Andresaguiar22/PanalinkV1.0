package com.example.ui.story

object StoryStudioPublisher {
    const val MAX_DURATION_MS = 120_000L
    const val MAX_SLIDES = 10

    fun validate(draft: StoryStudioDraft): Result<Unit> {
        if (draft.slides.isEmpty()) return Result.failure(IllegalArgumentException("La historia está vacía"))
        if (draft.slides.size > MAX_SLIDES) return Result.failure(IllegalArgumentException("Máximo 10 elementos"))
        val duration = draft.durationMs()
        if (duration <= 0L) return Result.failure(IllegalArgumentException("La duración debe ser mayor que cero"))
        if (duration > MAX_DURATION_MS) return Result.failure(IllegalArgumentException("La historia no puede superar 2 minutos"))
        return Result.success(Unit)
    }
}
