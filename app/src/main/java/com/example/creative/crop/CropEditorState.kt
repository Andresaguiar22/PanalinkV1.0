package com.example.creative.crop

/**
 * Non-destructive state for an interactive crop session.
 * It contains only normalized values, so it can be used by Compose UI,
 * previews, or another editor surface without depending on Android views.
 */
data class CropEditorState(
    val sourceAspectRatio: Float,
    val aspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    val cropRect: CropRect = CropRect(0f, 0f, 1f, 1f),
    val rotationDegrees: Float = 0f,
    val isCircular: Boolean = false
) {
    init {
        require(sourceAspectRatio > 0f) { "sourceAspectRatio must be positive" }
    }

    fun selectAspectRatio(target: CropAspectRatio): CropEditorState =
        copy(
            aspectRatio = target,
            cropRect = CropAspectRatioCalculator.centeredCrop(sourceAspectRatio, target)
        )

    fun moveBy(deltaX: Float, deltaY: Float): CropEditorState =
        copy(
            cropRect = CropAspectRatioCalculator.constrain(
                cropRect.copy(
                    left = cropRect.left + deltaX,
                    top = cropRect.top + deltaY
                )
            )
        )

    fun resize(width: Float, height: Float): CropEditorState =
        copy(
            cropRect = CropAspectRatioCalculator.constrain(
                cropRect.copy(width = width, height = height)
            )
        )

    fun rotateBy(deltaDegrees: Float): CropEditorState =
        copy(rotationDegrees = (rotationDegrees + deltaDegrees) % 360f)

    fun toggleCircular(): CropEditorState =
        copy(isCircular = !isCircular)
}
