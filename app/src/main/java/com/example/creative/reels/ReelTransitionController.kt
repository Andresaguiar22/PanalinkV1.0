package com.example.creative.reels

/** Keeps transition editing isolated from the timeline UI. */
object ReelTransitionController {
    fun setTransition(
        state: ReelTimelineState,
        clipId: String,
        transition: ReelTransition,
        durationMs: Long = 300L
    ): ReelTimelineState {
        val specDuration = durationMs.coerceIn(0L, 2000L)
        return state.updateClip(
            state.clips.firstOrNull { it.id == clipId }?.let { clip ->
                clip.copy(transition = transition)
            } ?: return state
        )
    }
}
