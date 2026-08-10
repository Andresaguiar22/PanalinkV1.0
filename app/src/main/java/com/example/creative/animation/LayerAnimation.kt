package com.example.creative.animation

enum class AnimationType {
    NONE,
    FADE_IN,
    FADE_OUT,
    BOUNCE_IN,
    SLIDE_IN_LEFT,
    SLIDE_IN_RIGHT,
    ZOOM_IN,
    TYPEWRITER,
    PULSE_LOOP,
    ROTATE_LOOP
}

data class LayerAnimationConfig(
    val entranceAnimation: AnimationType = AnimationType.FADE_IN,
    val entranceDurationMs: Long = 400L,
    val exitAnimation: AnimationType = AnimationType.FADE_OUT,
    val exitDurationMs: Long = 400L,
    val loopAnimation: AnimationType = AnimationType.NONE,
    val loopDurationMs: Long = 1000L
)

object LayerAnimationEngine {

    fun calculateAlpha(
        config: LayerAnimationConfig,
        layerAgeMs: Long,
        layerTotalDurationMs: Long
    ): Float {
        if (config.entranceAnimation == AnimationType.FADE_IN && layerAgeMs < config.entranceDurationMs) {
            return (layerAgeMs.toFloat() / config.entranceDurationMs).coerceIn(0f, 1f)
        }
        val timeLeftMs = layerTotalDurationMs - layerAgeMs
        if (config.exitAnimation == AnimationType.FADE_OUT && timeLeftMs < config.exitDurationMs) {
            return (timeLeftMs.toFloat() / config.exitDurationMs).coerceIn(0f, 1f)
        }
        return 1.0f
    }

    fun calculateScale(
        config: LayerAnimationConfig,
        layerAgeMs: Long,
        layerTotalDurationMs: Long
    ): Float {
        if (config.entranceAnimation == AnimationType.ZOOM_IN && layerAgeMs < config.entranceDurationMs) {
            return (layerAgeMs.toFloat() / config.entranceDurationMs).coerceIn(0.1f, 1.0f)
        }
        if (config.loopAnimation == AnimationType.PULSE_LOOP) {
            val cycle = (layerAgeMs % config.loopDurationMs).toFloat() / config.loopDurationMs
            val sineWave = Math.sin(cycle * Math.PI * 2.0).toFloat()
            return 1.0f + (sineWave * 0.08f)
        }
        return 1.0f
    }
}
