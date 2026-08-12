package com.example.ui.reels.editor

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.offset
import coil.compose.AsyncImage
import com.example.creative.canvas.ImageLayerRenderer
import com.example.creative.core.CreativeLayer

/**
 * Isolated renderer for interactive creative layers used by the Reel editor.
 *
 * This component owns only presentation and gesture translation. The parent
 * remains responsible for persisting the updated CreativeLayer list.
 */
@Composable
fun ReelCreativeLayers(
    layers: List<CreativeLayer>,
    selectedLayerId: String?,
    onLayerSelected: (String) -> Unit,
    onLayerUpdated: (CreativeLayer) -> Unit,
) {
    Box(modifier = Modifier) {
        layers.forEach { layer ->
            when (layer) {
                is CreativeLayer.Text -> {
                    TextLayer(
                        layer = layer,
                        selected = selectedLayerId == layer.id,
                        onSelected = { onLayerSelected(layer.id) },
                        onUpdated = onLayerUpdated,
                    )
                }

                is CreativeLayer.Sticker -> {
                    StickerLayer(
                        layer = layer,
                        onSelected = { onLayerSelected(layer.id) },
                        onUpdated = onLayerUpdated,
                    )
                }

                is CreativeLayer.Image -> {
                    ImageLayerRenderer(
                        layer = layer,
                        isSelected = selectedLayerId == layer.id,
                        onLayerTransformed = { x, y, scale, rotation ->
                            onLayerUpdated(
                                layer.copy(
                                    xFraction = x.coerceIn(0f, 1f),
                                    yFraction = y.coerceIn(0f, 1f),
                                    scale = scale,
                                    rotation = rotation,
                                )
                            )
                        },
                        onClick = { onLayerSelected(layer.id) },
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun TextLayer(
    layer: CreativeLayer.Text,
    selected: Boolean,
    onSelected: () -> Unit,
    onUpdated: (CreativeLayer) -> Unit,
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(layer.colorHex)) }
        .getOrDefault(Color.White)

    Text(
        text = layer.text,
        color = color,
        fontSize = (layer.fontSizeSp * layer.scale).sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .offset {
                IntOffset(
                    (layer.xFraction * 600f).toInt(),
                    (layer.yFraction * 1000f).toInt(),
                )
            }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    onSelected()
                    onUpdated(
                        layer.copy(
                            xFraction = (layer.xFraction + pan.x / 1000f).coerceIn(0f, 1f),
                            yFraction = (layer.yFraction + pan.y / 1000f).coerceIn(0f, 1f),
                            scale = (layer.scale * zoom).coerceIn(0.3f, 4f),
                            rotation = layer.rotation + rotation,
                        )
                    )
                }
            }
            .graphicsLayer {
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotation
                alpha = layer.opacity
            },
    )
}

@Composable
private fun StickerLayer(
    layer: CreativeLayer.Sticker,
    onSelected: () -> Unit,
    onUpdated: (CreativeLayer) -> Unit,
) {
    AsyncImage(
        model = layer.stickerUrlOrPath,
        contentDescription = "Sticker",
        modifier = Modifier
            .size(100.dp)
            .offset {
                IntOffset(
                    (layer.xFraction * 600f).toInt(),
                    (layer.yFraction * 1000f).toInt(),
                )
            }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    onSelected()
                    onUpdated(
                        layer.copy(
                            xFraction = (layer.xFraction + pan.x / 1000f).coerceIn(0f, 1f),
                            yFraction = (layer.yFraction + pan.y / 1000f).coerceIn(0f, 1f),
                            scale = (layer.scale * zoom).coerceIn(0.3f, 4f),
                            rotation = layer.rotation + rotation,
                        )
                    )
                }
            }
            .graphicsLayer {
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotation
                alpha = layer.opacity
            },
    )
}
