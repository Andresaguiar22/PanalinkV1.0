package com.example.creative.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Reusable, non-destructive crop overlay for creative editors.
 * The host owns persistence; this component only emits CropEditorState changes.
 */
@Composable
fun CropOverlay(
    initialState: CropEditorState,
    modifier: Modifier = Modifier,
    onStateChanged: (CropEditorState) -> Unit = {},
    onCancel: () -> Unit = {},
    onApply: (CropEditorState) -> Unit = {}
) {
    var state by remember(initialState) { mutableStateOf(initialState) }

    fun update(next: CropEditorState) {
        state = next
        onStateChanged(next)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.aspectRatio, state.cropRect) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        var next = state
                        val width = size.width.coerceAtLeast(1).toFloat()
                        val height = size.height.coerceAtLeast(1).toFloat()
                        next = next.moveBy(
                            deltaX = pan.x / width,
                            deltaY = pan.y / height
                        )
                        if (zoom.isFinite() && zoom > 0f) {
                            val factor = zoom.coerceIn(0.5f, 2f)
                            val rect = next.cropRect
                            val newWidth = (rect.width / factor).coerceIn(0.01f, 1f)
                            val newHeight = (rect.height / factor).coerceIn(0.01f, 1f)
                            val centerX = rect.left + rect.width / 2f
                            val centerY = rect.top + rect.height / 2f
                            next = next.copy(
                                cropRect = CropAspectRatioCalculator.constrain(
                                    CropRect(
                                        centerX - newWidth / 2f,
                                        centerY - newHeight / 2f,
                                        newWidth,
                                        newHeight
                                    )
                                )
                            )
                        }
                        if (rotation.isFinite()) {
                            next = next.rotateBy(rotation)
                        }
                        update(next)
                    }
                }
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            val rect = state.cropRect
            val left = rect.left * size.width
            val top = rect.top * size.height
            val width = rect.width * size.width
            val height = rect.height * size.height

            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(width, height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            val thirdW = width / 3f
            val thirdH = height / 3f
            repeat(2) { index ->
                val x = left + thirdW * (index + 1)
                drawLine(Color.White.copy(alpha = 0.45f), Offset(x, top), Offset(x, top + height), 1.dp.toPx())
                val y = top + thirdH * (index + 1)
                drawLine(Color.White.copy(alpha = 0.45f), Offset(left, y), Offset(left + width, y), 1.dp.toPx())
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            CropAspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = state.aspectRatio == ratio,
                    onClick = { update(state.selectAspectRatio(ratio)) },
                    label = { Text(ratio.label) }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            androidx.compose.material3.OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
            androidx.compose.material3.Button(
                onClick = { onApply(state) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aplicar")
            }
        }

        androidx.compose.material3.IconButton(
            onClick = { update(state.rotateBy(90f)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
        ) {
            Text("↻", style = MaterialTheme.typography.titleLarge)
        }
    }
}
