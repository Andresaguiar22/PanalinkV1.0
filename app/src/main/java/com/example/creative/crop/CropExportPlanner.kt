package com.example.creative.crop

/**
 * Converts an interactive crop session into the existing CropState consumed
 * by ImageCropEngine. No file I/O happens here.
 */
object CropExportPlanner {
    fun toCropState(state: CropEditorState): CropState = CropState(
        rotationDegrees = state.rotationDegrees,
        cropRectFraction = CropRectFraction(
            left = state.cropRect.left,
            top = state.cropRect.top,
            right = state.cropRect.right,
            bottom = state.cropRect.bottom
        ),
        isCircular = state.isCircular
    )
}
