package com.example.creative.crop

import android.graphics.RectF

/**
 * Converts an interactive crop session into the CropState consumed by
 * ImageCropEngine. No file I/O happens here.
 */
object CropExportPlanner {
    fun toCropState(state: CropEditorState): CropState = CropState(
        rotationDegrees = state.rotationDegrees,
        cropRectFraction = RectF(
            state.cropRect.left,
            state.cropRect.top,
            state.cropRect.right,
            state.cropRect.bottom
        ),
        isCircular = state.isCircular
    )
}
