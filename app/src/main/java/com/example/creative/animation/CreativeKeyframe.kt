package com.example.creative.animation

/**
 * P6.5B - Creative Engine Keyframe & Independent Property Tracks
 * Provides multi-property keyframing, property tracks and interpolation curves (Bézier, Spring, Easing).
 */

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE,
    SPRING,
    BEZIER
}

data class BezierCurve(
    val p1x: Float = 0.25f,
    val p1y: Float = 0.1f,
    val p2x: Float = 0.25f,
    val p2y: Float = 1.0f
)

data class CreativeKeyframe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: Float,
    val easing: EasingType = EasingType.LINEAR,
    val bezier: BezierCurve? = null
)

/**
 * Independent Keyframe Track per specific property (e.g. X, Y, Scale, Rotation, Opacity, Glow, Blur, Volume, Speed)
 */
data class PropertyTrack(
    val propertyName: String,
    val keyframes: List<CreativeKeyframe> = emptyList()
) {
    fun addOrUpdateKeyframe(keyframe: CreativeKeyframe): PropertyTrack {
        val updated = keyframes.filterNot { it.timeMs == keyframe.timeMs } + keyframe
        return copy(keyframes = updated.sortedBy { it.timeMs })
    }

    fun removeKeyframe(timeMs: Long): PropertyTrack {
        return copy(keyframes = keyframes.filterNot { it.timeMs == timeMs })
    }

    fun getKeyframeAt(timeMs: Long): CreativeKeyframe? {
        return keyframes.firstOrNull { it.timeMs == timeMs }
    }
}

/**
 * Master Animation Track containing all property tracks for a single layer or clip.
 */
data class AnimationTrack(
    val propertyTracks: Map<String, PropertyTrack> = emptyMap()
) {
    fun getPropertyTrack(propertyName: String): PropertyTrack {
        return propertyTracks[propertyName] ?: PropertyTrack(propertyName)
    }

    fun setPropertyTrack(track: PropertyTrack): AnimationTrack {
        val updated = propertyTracks.toMutableMap()
        updated[track.propertyName] = track
        return copy(propertyTracks = updated)
    }

    fun removePropertyTrack(propertyName: String): AnimationTrack {
        val updated = propertyTracks.toMutableMap()
        updated.remove(propertyName)
        return copy(propertyTracks = updated)
    }
}

object InterpolationEngine {

    fun interpolate(track: PropertyTrack, timeMs: Long, defaultValue: Float): Float {
        val keyframes = track.keyframes
        if (keyframes.isEmpty()) return defaultValue
        if (timeMs <= keyframes.first().timeMs) return keyframes.first().value
        if (timeMs >= keyframes.last().timeMs) return keyframes.last().value

        var k1 = keyframes.first()
        var k2 = keyframes.last()

        for (i in 0 until keyframes.size - 1) {
            if (timeMs >= keyframes[i].timeMs && timeMs <= keyframes[i + 1].timeMs) {
                k1 = keyframes[i]
                k2 = keyframes[i + 1]
                break
            }
        }

        val range = (k2.timeMs - k1.timeMs).toFloat()
        if (range <= 0f) return k1.value

        val progress = (timeMs - k1.timeMs).toFloat() / range
        val easedProgress = applyEasing(progress, k1.easing, k1.bezier)

        return k1.value + (k2.value - k1.value) * easedProgress
    }

    private fun applyEasing(t: Float, easing: EasingType, bezier: BezierCurve?): Float {
        return when (easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> t * (2f - t)
            EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
            EasingType.BOUNCE -> {
                if (t < 0.5f) {
                    8f * t * t * t * t
                } else {
                    1f - Math.pow(-2.0 * t + 2.0, 4.0).toFloat() / 2f
                }
            }
            EasingType.SPRING -> {
                val factor = 0.4f
                Math.pow(2.0, -10.0 * t).toFloat() * Math.sin((t - factor / 4f) * (2f * Math.PI) / factor).toFloat() + 1f
            }
            EasingType.BEZIER -> {
                val b = bezier ?: BezierCurve()
                val u = 1f - t
                3f * u * u * t * b.p1y + 3f * u * t * t * b.p2y + t * t * t
            }
        }
    }
}
