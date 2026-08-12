package com.example.creative.reels

data class ReelTransitionSpec(
    val type: ReelTransition,
    val durationMs: Long = when (type) {
        ReelTransition.CUT -> 0L
        ReelTransition.FADE -> 300L
        ReelTransition.SLIDE -> 300L
    }
) {
    init { require(durationMs >= 0L) { "Transition duration cannot be negative" } }
}
