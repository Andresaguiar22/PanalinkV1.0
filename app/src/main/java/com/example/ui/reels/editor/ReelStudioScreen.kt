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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
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
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTrack
import com.example.ui.reels.editor.model.ReelTrackType
import com.example.ui.reels.editor.playback.ReelTimelinePreviewSurface

@Composable
fun ReelStudioScreen(modifier: Modifier = Modifier, viewModel: ReelEditorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val project = state.project
    val selected = project.selectedLayerId?.let { id ->
        project.timeline.tracks.flatMap { track -> track.layers.map { track to it } }.firstOrNull { (_, layer) -> layer.id == id }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudioTopBar(project.durationMs) { viewModel.onEvent(ReelEditorEvent.AddTrack(it, trackName(it))) }
            PreviewPanel(Modifier.fillMaxWidth().weight(1f), project)
            TimelinePanel(project.timeline.tracks, project.durationMs, project.timeline.currentTimeMs, project.selectedLayerId,
                onSeek = { viewModel.onEvent(ReelEditorEvent.Seek(it)) },
                onSelectLayer = { viewModel.onEvent(ReelEditorEvent.SelectLayer(it)) })
            selected?.let { (track, layer) ->
                SelectedLayerToolbar(layer, track, project.timeline.currentTimeMs,
                    onSplit = { viewModel.onEvent(ReelEditorEvent.SplitLayer(track.id, layer.id, project.timeline.currentTimeMs)) },
                    onTrimStart = { if (project.timeline.currentTimeMs in layer.startTimeMs until layer.endTimeMs) viewModel.onEvent(ReelEditorEvent.TrimLayer(track.id, layer.id, project.timeline.currentTimeMs, layer.endTimeMs)) },
                    onTrimEnd = { if (project.timeline.currentTimeMs > layer.startTimeMs && project.timeline.currentTimeMs <= layer.endTimeMs) viewModel.onEvent(ReelEditorEvent.TrimLayer(track.id, layer.id, layer.startTimeMs, project.timeline.currentTimeMs)) },
                    onMoveLeft = { val i = track.layers.indexOfFirst { it.id == layer.id }; if (i > 0) viewModel.onEvent(ReelEditorEvent.ReorderLayer(track.id, layer.id, i - 1)) },
                    onMoveRight = { val i = track.layers.indexOfFirst { it.id == layer.id }; if (i >= 0 && i < track.layers.lastIndex) viewModel.onEvent(ReelEditorEvent.ReorderLayer(track.id, layer.id, i + 1)) },
                    onDelete = { viewModel.onEvent(ReelEditorEvent.RemoveLayer(track.id, layer.id)) })
            }
            StudioToolbar { viewModel.onEvent(ReelEditorEvent.AddTrack(it, trackName(it))) }
        }
    }
}

@Composable
private fun StudioTopBar(durationMs: Long, onAddTrack: (ReelTrackType) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Reel Studio", style = MaterialTheme.typography.titleLarge)
        Text(formatTime(durationMs), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onAddTrack(ReelTrackType.VIDEO) }) { Icon(Icons.Default.Add, "Agregar contenido") }
    }
}

@Composable
private fun PreviewPanel(modifier: Modifier, project: ReelProject) {
    Box(modifier.padding(horizontal = 12.dp).background(Color.Black), contentAlignment = Alignment.Center) {
        if (project.timeline.tracks.any { it.layers.isNotEmpty() }) ReelTimelinePreviewSurface(project, Modifier.fillMaxSize())
        else Text("Agrega un vídeo o una foto para comenzar", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TimelinePanel(tracks: List<ReelTrack>, durationMs: Long, currentTimeMs: Long, selectedLayerId: String?, onSeek: (Long) -> Unit, onSelectLayer: (String?) -> Unit) {
    val scrollState = rememberScrollState()
    val safeDuration = durationMs.coerceAtLeast(1L)
    val timelineWidth = ((safeDuration / 1000f) * 90f).coerceIn(360f, 3600f).dp
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text("Timeline  •  ${formatTime(currentTimeMs)}", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(76.dp).padding(start = 8.dp)) { tracks.forEach { Text(it.name, Modifier.height(42.dp), style = MaterialTheme.typography.labelSmall) } }
            Box(Modifier.width(timelineWidth)) {
                Column { tracks.forEach { TimelineTrack(it, safeDuration, timelineWidth, selectedLayerId, onSeek, onSelectLayer) } }
                val playheadX = ((currentTimeMs.toFloat() / safeDuration.toFloat()) * timelineWidth.value).coerceIn(0f, timelineWidth.value)
                Box(Modifier.offset(x = playheadX.dp).width(2.dp).height((tracks.size * 42).coerceAtLeast(42).dp).background(MaterialTheme.colorScheme.error))
            }
        }
    }
}

@Composable
private fun TimelineTrack(track: ReelTrack, durationMs: Long, timelineWidth: Dp, selectedLayerId: String?, onSeek: (Long) -> Unit, onSelectLayer: (String?) -> Unit) {
    Box(Modifier.width(timelineWidth).height(42.dp).padding(vertical = 2.dp).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onSeek(durationMs / 2L) }) {
        track.layers.forEach { layer -> TimelineLayerBlock(layer, durationMs, timelineWidth, layer.id == selectedLayerId) { onSelectLayer(layer.id) } }
    }
}

@Composable
private fun TimelineLayerBlock(layer: ReelLayer, durationMs: Long, timelineWidth: Dp, selected: Boolean, onClick: () -> Unit) {
    val startFraction = (layer.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val widthFraction = ((layer.endTimeMs - layer.startTimeMs).toFloat() / durationMs.toFloat()).coerceIn(0.02f, (1f - startFraction).coerceAtLeast(0.02f))
    Box(Modifier.offset(x = (timelineWidth.value * startFraction).dp).fillMaxWidth(widthFraction).height(38.dp).padding(2.dp)
        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onClick)) {
        Text(layer.type.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(horizontal = 6.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SelectedLayerToolbar(layer: ReelLayer, track: ReelTrack, currentTimeMs: Long, onSplit: () -> Unit, onTrimStart: () -> Unit, onTrimEnd: () -> Unit, onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Seleccionado: ${layer.type.name.lowercase()}  •  ${formatTime(layer.durationMs)}", Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSplit, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs < layer.endTimeMs) { Icon(Icons.Default.ContentCut, "Dividir") }
            IconButton(onClick = onTrimStart, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs < layer.endTimeMs) { Icon(Icons.Default.ArrowForward, "Recortar inicio") }
            IconButton(onClick = onTrimEnd, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs <= layer.endTimeMs) { Icon(Icons.Default.ArrowBack, "Recortar final") }
            IconButton(onClick = onMoveLeft, enabled = track.layers.indexOfFirst { it.id == layer.id } > 0) { Icon(Icons.Default.ArrowBack, "Mover a la izquierda") }
            IconButton(onClick = onMoveRight, enabled = track.layers.indexOfFirst { it.id == layer.id } in 0 until track.layers.lastIndex) { Icon(Icons.Default.ArrowForward, "Mover a la derecha") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
        }
    }
}

@Composable
private fun StudioToolbar(onAddTrack: (ReelTrackType) -> Unit) {
    val items = listOf(ReelTrackType.VIDEO to Pair(Icons.Default.Add, "Medios"), ReelTrackType.TEXT to Pair(Icons.Default.TextFields, "Texto"), ReelTrackType.STICKER to Pair(Icons.Default.EmojiEmotions, "Sticker"), ReelTrackType.AUDIO to Pair(Icons.Default.AudioFile, "Audio"), ReelTrackType.EFFECT to Pair(Icons.Default.FilterAlt, "Filtros"))
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (type, item) -> Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = { onAddTrack(type) }) { Icon(item.first, item.second) }; Text(item.second, style = MaterialTheme.typography.labelSmall) } }
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