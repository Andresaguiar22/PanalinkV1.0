package com.example.ui.reels.editor

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/** Shared transform gesture surface for visual Reel layers. */
@Composable
fun Modifier.reelTransformGestures(
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Float) -> Unit,
): Modifier = pointerInput(Unit) {
    detectTransformGestures { _, pan, zoom, rotation ->
        if (pan != Offset.Zero) onPan(pan)
        if (zoom != 1f) onZoom(zoom)
        if (rotation != 0f) onRotate(rotation)
    }
}
