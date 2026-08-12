package com.example.ui.reels.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelLayerContent
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTrack
import kotlin.math.roundToInt

@Composable
fun ReelCanvasLayers(
    project: ReelProject,
    onSelect: (String) -> Unit,
    onMove: (ReelTrack, ReelLayer, Float, Float) -> Unit
) {
    val visibleLayers = project.timeline.tracks
        .flatMap { track -> track.layers.map { track to it } }
        .filter { (_, layer) -> layer.visible }
        .sortedBy { (_, layer) -> layer.zIndex }

    Box(Modifier.fillMaxSize()) {
        visibleLayers.forEach { (track, layer) ->
            if (layer.type == com.example.ui.reels.editor.model.ReelTrackType.TEXT || layer.type == com.example.ui.reels.editor.model.ReelTrackType.STICKER) {
                CanvasLayerItem(layer, layer.id == project.selectedLayerId, onSelect) { dx, dy ->
                    onMove(track, layer, dx, dy)
                }
            }
        }
    }
}

@Composable
private fun CanvasLayerItem(
    layer: ReelLayer,
    selected: Boolean,
    onSelect: (String) -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val content = layer.content
    val label = when (content) {
        is ReelLayerContent.Text -> content.value
        is ReelLayerContent.Sticker -> "😀"
        else -> return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(layer.id, layer.x, layer.y) {
                detectDragGestures(
                    onDragStart = { onSelect(layer.id) }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offsetFraction(layer.x, layer.y)
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) Color.White else Color.White,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

private fun Modifier.offsetFraction(x: Float, y: Float): Modifier = this.then(
    Modifier.offset {
        IntOffset((x.coerceIn(0f, 1f) * 1080f).roundToInt(), (y.coerceIn(0f, 1f) * 1920f).roundToInt())
    }
)