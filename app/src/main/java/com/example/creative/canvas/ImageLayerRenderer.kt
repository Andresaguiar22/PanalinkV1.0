package com.example.creative.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creative.core.CreativeLayer

@Composable
fun ImageLayerRenderer(
    layer: CreativeLayer.Image,
    isSelected: Boolean,
    onLayerTransformed: (xFraction: Float, yFraction: Float, scale: Float, rotation: Float) -> Unit,
    onClick: () -> Unit = {}
) {
    var offsetX = remember(layer.id) { mutableFloatStateOf(layer.xFraction) }
    var offsetY = remember(layer.id) { mutableFloatStateOf(layer.yFraction) }
    var currentScale = remember(layer.id) { mutableFloatStateOf(layer.scale) }
    var currentRotation = remember(layer.id) { mutableFloatStateOf(layer.rotation) }

    val colorMatrix = ColorMatrix().apply {
        val brightness = layer.brightness.coerceIn(-1f, 1f)
        val contrast = layer.contrast.coerceAtLeast(0f)
        val saturation = layer.saturation.coerceAtLeast(0f)
        val translate = (1f - contrast) * 128f + brightness * 255f
        setTo(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        setSaturation(saturation)
    }

    Box(
        modifier = Modifier
            .size(180.dp)
            .graphicsLayer(alpha = layer.opacity)
            .scale(currentScale.floatValue)
            .rotate(currentRotation.floatValue)
            .clip(RoundedCornerShape(6.dp))
            .then(if (isSelected) Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp)) else Modifier)
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    offsetX.floatValue += pan.x / size.width.toFloat()
                    offsetY.floatValue += pan.y / size.height.toFloat()
                    currentScale.floatValue = (currentScale.floatValue * zoom).coerceIn(0.2f, 5f)
                    currentRotation.floatValue += rotation
                    onLayerTransformed(
                        offsetX.floatValue,
                        offsetY.floatValue,
                        currentScale.floatValue,
                        currentRotation.floatValue
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = layer.imageUriOrPath,
            contentDescription = "Imagen de Reel",
            modifier = Modifier.size(180.dp),
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )
    }
}