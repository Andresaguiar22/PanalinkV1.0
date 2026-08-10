package com.example.creative.beauty

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

data class BeautyConfig(
    val skinSmoothing: Float = 0.5f,  // 0.0 to 1.0
    val shineReduction: Float = 0.3f, // 0.0 to 1.0
    val warmthAdjustment: Float = 0.5f, // 0.0 to 1.0
    val smartLighting: Float = 0.4f,   // 0.0 to 1.0
    val shadowCorrection: Float = 0.3f, // 0.0 to 1.0
    val sharpness: Float = 0.2f,        // 0.0 to 1.0
    val eyeEnhancement: Float = 0.4f    // 0.0 to 1.0
)

object BeautyStudioProcessor {

    fun createBeautyColorFilter(config: BeautyConfig): ColorMatrixColorFilter {
        val matrix = ColorMatrix()

        // Warmth & Smart Lighting adjustment
        val warmthFactor = (config.warmthAdjustment - 0.5f) * 30f
        val lightingFactor = config.smartLighting * 20f
        val shadowFactor = config.shadowCorrection * 15f

        val rOffset = warmthFactor + lightingFactor + shadowFactor
        val gOffset = lightingFactor * 0.8f
        val bOffset = -warmthFactor * 0.5f + lightingFactor

        matrix.set(floatArrayOf(
            1.05f, 0.0f,  0.0f,  0f, rOffset,
            0.0f,  1.02f, 0.0f,  0f, gOffset,
            0.0f,  0.0f,  1.00f, 0f, bOffset,
            0.0f,  0.0f,  0.0f,  1f, 0f
        ))

        return ColorMatrixColorFilter(matrix)
    }

    fun applyBeautyFilterToBitmap(sourceBitmap: Bitmap, config: BeautyConfig): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outputBitmap)
        val paint = android.graphics.Paint().apply {
            colorFilter = createBeautyColorFilter(config)
            isAntiAlias = true
        }
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
        return outputBitmap
    }
}
