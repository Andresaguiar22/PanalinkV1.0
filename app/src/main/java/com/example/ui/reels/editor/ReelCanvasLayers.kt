package com.example.ui.reels.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelLayerContent
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTrack
import com.example.ui.reels.editor.model.ReelTrackType
import kotlin.math.roundToInt

@Composable
fun ReelCanvasLayers(
    project: ReelProject,
    onSelect: (String) -> Unit,
    onMove: (ReelTrack, ReelLayer, Float, Float) -> Unit,
    onTransform: (ReelTrack, ReelLayer, Float, Float) -> Unit
) {
    val visibleLayers = project.timeline.tracks.flatMap { track -> track.layers.map { track to it } }
        .filter { (_, layer) ->
            layer.visible &&
                project.timeline.currentTimeMs in layer.startTimeMs..layer.endTimeMs &&
                layer.type in setOf(ReelTrackType.TEXT, ReelTrackType.STICKER)
        }
        .sortedBy { (_, layer) -> layer.zIndex }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        Box(Modifier.fillMaxSize()) {
            visibleLayers.forEach { (track, layer) ->
                CanvasLayerItem(
                    layer = layer,
                    selected = layer.id == project.selectedLayerId,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    onSelect = onSelect,
                    onPan = { dx, dy -> onMove(track, layer, dx / widthPx, dy / heightPx) },
                    onTransform = { zoom, rotation -> onTransform(track, layer, zoom, rotation) }
                )
            }
        }
    }
}

@Composable
private fun CanvasLayerItem(
    layer: ReelLayer,
    selected: Boolean,
    widthPx: Float,
    heightPx: Float,
    onSelect: (String) -> Unit,
    onPan: (Float, Float) -> Unit,
    onTransform: (Float, Float) -> Unit
) {
    val label = when (val content = layer.content) {
        is ReelLayerContent.Text -> content.value
        is ReelLayerContent.Sticker -> "😀"
        else -> return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .reelTransformGestures(
                onPan = { onSelect(layer.id); onPan(it.x, it.y) },
                onZoom = { onSelect(layer.id); onTransform(it, 0f) },
                onRotate = { onSelect(layer.id); onTransform(1f, it) }
            )
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        (layer.x.coerceIn(0f, 1f) * widthPx).roundToInt(),
                        (layer.y.coerceIn(0f, 1f) * heightPx).roundToInt()
                    )
                }
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else Color.Transparent
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
