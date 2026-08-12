package com.example.creative.crop

import kotlin.math.max

/**
 * Calculates a normalized crop rectangle for a requested aspect ratio.
 * The returned rectangle stays centered and always fits inside the source.
 */
object CropAspectRatioCalculator {

    fun centeredCrop(
        sourceAspectRatio: Float,
        target: CropAspectRatio
    ): CropRect {
        require(sourceAspectRatio > 0f) { "sourceAspectRatio must be positive" }

        val targetAspect = target.value ?: return CropRect(0f, 0f, 1f, 1f)

        return if (sourceAspectRatio > targetAspect) {
            val width = targetAspect / sourceAspectRatio
            CropRect(
                left = (1f - width) / 2f,
                top = 0f,
                width = width,
                height = 1f
            )
        } else {
            val height = sourceAspectRatio / targetAspect
            CropRect(
                left = 0f,
                top = (1f - height) / 2f,
                width = 1f,
                height = height
            )
        }
    }

    fun constrain(rect: CropRect): CropRect {
        val width = rect.width.coerceIn(0.01f, 1f)
        val height = rect.height.coerceIn(0.01f, 1f)
        val left = rect.left.coerceIn(0f, max(0f, 1f - width))
        val top = rect.top.coerceIn(0f, max(0f, 1f - height))
        return CropRect(left, top, width, height)
    }
}

data class CropRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}
