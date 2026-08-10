package com.example.creative.effects

import android.graphics.Canvas
import android.graphics.Paint

object FaceMaskRenderer {

    fun renderMaskOverlay(
        canvas: Canvas,
        face: DetectedFaceData,
        maskType: ARMaskType
    ) {
        if (maskType == ARMaskType.NONE) return

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = android.graphics.Color.CYAN
        }

        canvas.drawRect(
            face.boundingBoxLeft,
            face.boundingBoxTop,
            face.boundingBoxLeft + face.boundingBoxWidth,
            face.boundingBoxTop + face.boundingBoxHeight,
            paint
        )
    }
}
