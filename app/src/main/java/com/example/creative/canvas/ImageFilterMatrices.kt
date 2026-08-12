package com.example.creative.canvas

import androidx.compose.ui.graphics.ColorMatrix

/** Local, offline image filters. No network or AI dependency. */
object ImageFilterMatrices {
    fun forName(name: String): ColorMatrix = when (name.lowercase()) {
        "mono" -> ColorMatrix().apply { setToSaturation(0f) }
        "warm" -> ColorMatrix(floatArrayOf(
            1.08f, 0f, 0f, 0f, 8f,
            0f, 1.02f, 0f, 0f, 2f,
            0f, 0f, 0.92f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f
        ))
        "cool" -> ColorMatrix(floatArrayOf(
            0.94f, 0f, 0f, 0f, -3f,
            0f, 1.01f, 0f, 0f, 1f,
            0f, 0f, 1.08f, 0f, 7f,
            0f, 0f, 0f, 1f, 0f
        ))
        "vintage" -> ColorMatrix(floatArrayOf(
            0.90f, 0.05f, 0f, 0f, 18f,
            0.03f, 0.84f, 0.02f, 0f, 10f,
            0f, 0.04f, 0.72f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        ))
        "vivid" -> ColorMatrix().apply { setToSaturation(1.35f) }
        else -> ColorMatrix()
    }
}
