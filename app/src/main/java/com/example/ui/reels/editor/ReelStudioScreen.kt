package com.example.ui.reels.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelTrack
import com.example.ui.reels.editor.model.ReelTrackType

@Composable
fun ReelStudioScreen(
    modifier: Modifier = Modifier,
    viewModel: ReelEditorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val project = state.project

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudioTopBar(
                durationMs = project.durationMs,
                onAddTrack = { viewModel.onEvent(ReelEditorEvent.AddTrack(it, trackName(it))) }
            )
            PreviewPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                currentTimeMs = project.timeline.currentTimeMs,
                durationMs = project.durationMs
            )
            TimelinePanel(
                tracks = project.timeline.tracks,
                durationMs = project.durationMs,
                currentTimeMs = project.timeline.currentTimeMs,
                onSeek = { viewModel.onEvent(ReelEditorEvent.Seek(it)) }
            )
            StudioToolbar(
                onAddTrack = { viewModel.onEvent(ReelEditorEvent.AddTrack(it, trackName(it))) }
            )
        }
    }
}

@Composable
private fun StudioTopBar(durationMs: Long, onAddTrack: (ReelTrackType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Reel Studio", style = MaterialTheme.typography.titleLarge)
        Text(formatTime(durationMs), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onAddTrack(ReelTrackType.VIDEO) }) {
            Icon(Icons.Default.Add, contentDescription = "Agregar contenido")
        }
    }
}

@Composable
private fun PreviewPanel(modifier: Modifier, currentTimeMs: Long, durationMs: Long) {
    Box(
        modifier = modifier.padding(horizontal = 12.dp).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Vista previa\n${formatTime(currentTimeMs)} / ${formatTime(durationMs)}",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun TimelinePanel(
    tracks: List<ReelTrack>,
    durationMs: Long,
    currentTimeMs: Long,
    onSeek: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val safeDuration = durationMs.coerceAtLeast(1L)
    val timelineWidth = ((safeDuration / 1000f) * 90f).coerceIn(360f, 3600f).dp

    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            text = "Timeline  •  ${formatTime(currentTimeMs)}",
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(76.dp).padding(start = 8.dp)) {
                tracks.forEach { track ->
                    Text(track.name, modifier = Modifier.height(42.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(modifier = Modifier.width(timelineWidth)) {
                Column {
                    tracks.forEach { track ->
                        TimelineTrack(track, safeDuration, timelineWidth, onSeek)
                    }
                }
                val playheadX = ((currentTimeMs.toFloat() / safeDuration.toFloat()) * timelineWidth.value)
                    .coerceIn(0f, timelineWidth.value)
                Box(
                    modifier = Modifier.offset(x = playheadX.dp)
                        .width(2.dp)
                        .height((tracks.size * 42).coerceAtLeast(42).dp)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun TimelineTrack(track: ReelTrack, durationMs: Long, timelineWidth: Dp, onSeek: (Long) -> Unit) {
    Box(
        modifier = Modifier.width(timelineWidth).height(42.dp).padding(vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onSeek(durationMs / 2L) }
    ) {
        track.layers.forEach { layer -> TimelineLayerBlock(layer, durationMs, timelineWidth) }
    }
}

@Composable
private fun TimelineLayerBlock(layer: ReelLayer, durationMs: Long, timelineWidth: Dp) {
    val startFraction = (layer.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val widthFraction = ((layer.endTimeMs - layer.startTimeMs).toFloat() / durationMs.toFloat())
        .coerceIn(0.02f, (1f - startFraction).coerceAtLeast(0.02f))
    Box(
        modifier = Modifier.offset(x = (timelineWidth.value * startFraction).dp)
            .fillMaxWidth(widthFraction)
            .height(38.dp)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
    )
}

@Composable
private fun StudioToolbar(onAddTrack: (ReelTrackType) -> Unit) {
    val items = listOf(
        ReelTrackType.VIDEO to Pair(Icons.Default.Add, "Medios"),
        ReelTrackType.TEXT to Pair(Icons.Default.TextFields, "Texto"),
        ReelTrackType.STICKER to Pair(Icons.Default.EmojiEmotions, "Sticker"),
        ReelTrackType.AUDIO to Pair(Icons.Default.AudioFile, "Audio"),
        ReelTrackType.EFFECT to Pair(Icons.Default.FilterAlt, "Filtros")
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (type, item) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onAddTrack(type) }) {
                    Icon(item.first, contentDescription = item.second)
                }
                Text(item.second, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun trackName(type: ReelTrackType): String = when (type) {
    ReelTrackType.VIDEO -> "Video"
    ReelTrackType.IMAGE -> "Fotos"
    ReelTrackType.AUDIO -> "Audio"
    ReelTrackType.TEXT -> "Texto"
    ReelTrackType.STICKER -> "Stickers"
    ReelTrackType.EFFECT -> "Efectos"
    ReelTrackType.SUBTITLE -> "Subtítulos"
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
