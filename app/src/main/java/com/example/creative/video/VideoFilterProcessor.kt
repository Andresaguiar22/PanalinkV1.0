package com.example.creative.video

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

enum class VideoFilterType(val displayName: String) {
    NONE("Normal"),
    CINEMATIC("Cinematic"),
    VINTAGE("Vintage"),
    NEON("Neon"),
    WARM("Warm"),
    BLACK_WHITE("B&W"),
    HDR("HDR"),
    COOL("Cool"),
    SUNSET("Sunset")
}

object VideoFilterProcessor {

    fun getColorMatrix(filterType: VideoFilterType): ColorMatrix {
        val matrix = ColorMatrix()
        when (filterType) {
            VideoFilterType.NONE -> {
                matrix.reset()
            }
            VideoFilterType.CINEMATIC -> {
                matrix.set(floatArrayOf(
                    0.9f, 0.1f, 0.1f, 0f, -10f,
                    0.1f, 1.1f, 0.1f, 0f, 0f,
                    0.1f, 0.2f, 1.2f, 0f, 15f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
            VideoFilterType.VINTAGE -> {
                matrix.set(floatArrayOf(
                    0.9f, 0.2f, 0.1f, 0f, 20f,
                    0.1f, 0.8f, 0.1f, 0f, 15f,
                    0.1f, 0.1f, 0.6f, 0f, 10f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
            VideoFilterType.NEON -> {
                matrix.set(floatArrayOf(
                    1.2f, 0.1f, 0.3f, 0f, 10f,
                    0.1f, 1.3f, 0.1f, 0f, 5f,
                    0.4f, 0.1f, 1.4f, 0f, 20f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
            VideoFilterType.WARM -> {
                matrix.set(floatArrayOf(
                    1.2f, 0.1f, 0.0f, 0f, 15f,
                    0.1f, 1.1f, 0.0f, 0f, 10f,
                    0.0f, 0.1f, 0.8f, 0f, 0f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
            VideoFilterType.BLACK_WHITE -> {
                matrix.setSaturation(0f)
            }
            VideoFilterType.HDR -> {
                matrix.set(floatArrayOf(
                    1.3f, -0.1f, -0.1f, 0f, 0f,
                    -0.1f, 1.3f, -0.1f, 0f, 0f,
                    -0.1f, -0.1f, 1.3f, 0f, 0f,
                    0f,    0f,    0f,    1f, 0f
                ))
            }
            VideoFilterType.COOL -> {
                matrix.set(floatArrayOf(
                    0.8f, 0.1f, 0.1f, 0f, 0f,
                    0.1f, 1.0f, 0.1f, 0f, 5f,
                    0.1f, 0.2f, 1.3f, 0f, 20f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
            VideoFilterType.SUNSET -> {
                matrix.set(floatArrayOf(
                    1.4f, 0.1f, 0.0f, 0f, 25f,
                    0.2f, 0.9f, 0.0f, 0f, 10f,
                    0.1f, 0.1f, 0.7f, 0f, -10f,
                    0f,   0f,   0f,   1f, 0f
                ))
            }
        }
        return matrix
    }

    fun getColorFilter(filterType: VideoFilterType): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(getColorMatrix(filterType))
    }
}
