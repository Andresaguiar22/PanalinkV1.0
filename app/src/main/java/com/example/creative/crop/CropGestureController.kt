package com.example.creative.crop

import kotlin.math.abs

/**
 * Converts normalized drag/zoom/rotation gestures into CropEditorState updates.
 * This class is UI-toolkit agnostic and can be driven by Compose pointer input
 * or another gesture source later.
 */
class CropGestureController(
    initialState: CropEditorState
) {
    var state: CropEditorState = initialState
        private set

    fun reset() {
        state = CropEditorState(
            sourceAspectRatio = state.sourceAspectRatio,
            aspectRatio = state.aspectRatio,
            cropRect = CropAspectRatioCalculator.centeredCrop(
                state.sourceAspectRatio,
                state.aspectRatio
            )
        )
    }

    fun selectAspectRatio(aspectRatio: CropAspectRatio) {
        state = state.selectAspectRatio(aspectRatio)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        state = state.moveBy(deltaX, deltaY)
    }

    fun zoom(scaleFactor: Float) {
        if (!scaleFactor.isFinite() || scaleFactor <= 0f) return
        val factor = scaleFactor.coerceIn(0.5f, 2f)
        val current = state.cropRect
        val newWidth = (current.width / factor).coerceIn(0.01f, 1f)
        val newHeight = (current.height / factor).coerceIn(0.01f, 1f)
        val centerX = current.left + current.width / 2f
        val centerY = current.top + current.height / 2f
        state = state.copy(
            cropRect = CropAspectRatioCalculator.constrain(
                CropRect(
                    left = centerX - newWidth / 2f,
                    top = centerY - newHeight / 2f,
                    width = newWidth,
                    height = newHeight
                )
            )
        )
    }

    fun rotate(deltaDegrees: Float) {
        if (!deltaDegrees.isFinite() || abs(deltaDegrees) < 0.001f) return
        state = state.rotateBy(deltaDegrees)
    }

    fun toggleCircular() {
        state = state.toggleCircular()
    }
}
