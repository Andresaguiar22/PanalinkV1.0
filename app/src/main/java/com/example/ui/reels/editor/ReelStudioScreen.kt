package com.example.ui.reels.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.reels.editor.model.*
import com.example.ui.reels.editor.playback.ReelTimelinePreviewSurface
import kotlin.math.roundToLong

@Composable
fun ReelStudioScreen(modifier: Modifier = Modifier, viewModel: ReelEditorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val project = state.project
    val selected = project.selectedLayerId?.let { id -> project.timeline.tracks.flatMap { track -> track.layers.map { track to it } }.firstOrNull { (_, layer) -> layer.id == id } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { viewModel.onEvent(ReelEditorEvent.AddImageLayer(it.toString())) } }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            StudioTopBar(project.durationMs) { viewModel.onEvent(ReelEditorEvent.AddTrack(it, trackName(it))) }
            PreviewPanel(Modifier.fillMaxWidth().weight(1f), project,
                onSelectLayer = { viewModel.onEvent(ReelEditorEvent.SelectLayer(it)) },
                onMoveLayer = { track, layer, dx, dy -> viewModel.onEvent(ReelEditorEvent.UpdateLayer(track.id, layer.copy(x = (layer.x + dx).coerceIn(0f, 1f), y = (layer.y + dy).coerceIn(0f, 1f)))) },
                onTransformLayer = { _, layer, zoom, rotationDelta -> viewModel.onEvent(ReelEditorEvent.TransformLayer(layer.id, (layer.scale * zoom).coerceIn(0.1f, 8f), layer.rotationDegrees + rotationDelta)) }
            )
            TimelinePanel(project.timeline.tracks, project.durationMs, project.timeline.currentTimeMs, project.selectedLayerId,
                onSeek = { viewModel.onEvent(ReelEditorEvent.Seek(it)) }, onSelectLayer = { viewModel.onEvent(ReelEditorEvent.SelectLayer(it)) },
                onMoveLayer = { trackId, layerId, deltaMs -> project.timeline.tracks.firstOrNull { it.id == trackId }?.layers?.firstOrNull { it.id == layerId }?.let { layer -> val duration = layer.durationMs; val maxStart = (project.durationMs - duration).coerceAtLeast(0L); val start = (layer.startTimeMs + deltaMs).coerceIn(0L, maxStart); viewModel.onEvent(ReelEditorEvent.UpdateLayer(trackId, layer.copy(startTimeMs = start, endTimeMs = start + duration))) } },
                onTrimLayer = { trackId, layerId, startMs, endMs -> project.timeline.tracks.firstOrNull { it.id == trackId }?.layers?.firstOrNull { it.id == layerId }?.let { layer -> val safeStart = startMs.coerceIn(0L, (layer.endTimeMs - 1L).coerceAtLeast(0L)); val safeEnd = endMs.coerceIn(safeStart + 1L, project.durationMs.coerceAtLeast(safeStart + 1L)); viewModel.onEvent(ReelEditorEvent.UpdateLayer(trackId, layer.copy(startTimeMs = safeStart, endTimeMs = safeEnd))) } }
            )
            selected?.let { (track, layer) -> SelectedLayerToolbar(layer, track, project.timeline.currentTimeMs,
                onSplit = { viewModel.onEvent(ReelEditorEvent.SplitLayer(track.id, layer.id, project.timeline.currentTimeMs)) },
                onTrimStart = { if (project.timeline.currentTimeMs in layer.startTimeMs until layer.endTimeMs) viewModel.onEvent(ReelEditorEvent.TrimLayer(track.id, layer.id, project.timeline.currentTimeMs, layer.endTimeMs)) },
                onTrimEnd = { if (project.timeline.currentTimeMs > layer.startTimeMs && project.timeline.currentTimeMs <= layer.endTimeMs) viewModel.onEvent(ReelEditorEvent.TrimLayer(track.id, layer.id, layer.startTimeMs, project.timeline.currentTimeMs)) },
                onMoveLeft = { val i = track.layers.indexOfFirst { it.id == layer.id }; if (i > 0) viewModel.onEvent(ReelEditorEvent.ReorderLayer(track.id, layer.id, i - 1)) },
                onMoveRight = { val i = track.layers.indexOfFirst { it.id == layer.id }; if (i >= 0 && i < track.layers.lastIndex) viewModel.onEvent(ReelEditorEvent.ReorderLayer(track.id, layer.id, i + 1)) },
                onDelete = { viewModel.onEvent(ReelEditorEvent.RemoveLayer(track.id, layer.id)) }) }
            StudioToolbar(onAddTrack = { type -> viewModel.onEvent(ReelEditorEvent.AddTrack(type, trackName(type))) }, onAddText = { viewModel.onEvent(ReelEditorEvent.AddTextLayer()) }, onAddImage = { imagePicker.launch("image/*") })
        }
    }
}

@Composable private fun StudioTopBar(durationMs: Long, onAddTrack: (ReelTrackType) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) { Text("Reel Studio", style = MaterialTheme.typography.titleLarge); Text(formatTime(durationMs), style = MaterialTheme.typography.labelLarge); IconButton(onClick = { onAddTrack(ReelTrackType.VIDEO) }) { Icon(Icons.Default.Add, "Agregar contenido") } } }

@Composable private fun PreviewPanel(modifier: Modifier, project: ReelProject, onSelectLayer: (String) -> Unit, onMoveLayer: (ReelTrack, ReelLayer, Float, Float) -> Unit, onTransformLayer: (ReelTrack, ReelLayer, Float, Float) -> Unit) { Box(modifier.padding(horizontal = 12.dp).background(Color.Black), contentAlignment = Alignment.Center) { if (project.timeline.tracks.any { it.layers.isNotEmpty() }) { ReelTimelinePreviewSurface(project, Modifier.fillMaxSize()); ReelCanvasLayers(project, onSelectLayer, onMoveLayer, onTransformLayer) } else Text("Agrega un vídeo o una foto para comenzar", color = Color.White, style = MaterialTheme.typography.titleMedium) } }

@Composable private fun TimelinePanel(tracks: List<ReelTrack>, durationMs: Long, currentTimeMs: Long, selectedLayerId: String?, onSeek: (Long) -> Unit, onSelectLayer: (String?) -> Unit, onMoveLayer: (String, String, Long) -> Unit, onTrimLayer: (String, String, Long, Long) -> Unit) { val scrollState = rememberScrollState(); val safeDuration = durationMs.coerceAtLeast(1L); val timelineWidth = ((safeDuration / 1000f) * 90f).coerceIn(360f, 3600f).dp; Column(Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Timeline  •  ${formatTime(currentTimeMs)}", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall); Row(Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 8.dp), Alignment.CenterVertically) { Column(Modifier.width(76.dp).padding(start = 8.dp)) { tracks.forEach { Text(it.name, Modifier.height(42.dp), style = MaterialTheme.typography.labelSmall) } }; Box(Modifier.width(timelineWidth)) { Column { tracks.forEach { TimelineTrack(it, safeDuration, timelineWidth, selectedLayerId, onSeek, onSelectLayer, onMoveLayer, onTrimLayer) } }; val playheadX = ((currentTimeMs.toFloat() / safeDuration) * timelineWidth.value).coerceIn(0f, timelineWidth.value); Box(Modifier.offset(x = playheadX.dp).width(2.dp).height((tracks.size * 42).coerceAtLeast(42).dp).background(MaterialTheme.colorScheme.error)) } } } }

@Composable private fun TimelineTrack(track: ReelTrack, durationMs: Long, timelineWidth: Dp, selectedLayerId: String?, onSeek: (Long) -> Unit, onSelectLayer: (String?) -> Unit, onMoveLayer: (String, String, Long) -> Unit, onTrimLayer: (String, String, Long, Long) -> Unit) { Box(Modifier.width(timelineWidth).height(42.dp).padding(vertical = 2.dp).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onSeek(durationMs / 2L) }) { track.layers.forEach { layer -> TimelineLayerBlock(layer, durationMs, timelineWidth, layer.id == selectedLayerId, onSelect = { onSelectLayer(layer.id) }, onMove = { onMoveLayer(track.id, layer.id, it) }, onTrim = { start, end -> onTrimLayer(track.id, layer.id, start, end) }) } } }

@Composable private fun TimelineLayerBlock(layer: ReelLayer, durationMs: Long, timelineWidth: Dp, selected: Boolean, onSelect: () -> Unit, onMove: (Long) -> Unit, onTrim: (Long, Long) -> Unit) { val density = LocalDensity.current; val timelineWidthPx = with(density) { timelineWidth.toPx() }; val pxPerMs = timelineWidthPx / durationMs.toFloat(); val startFraction = (layer.startTimeMs.toFloat() / durationMs).coerceIn(0f, 1f); val widthFraction = ((layer.endTimeMs - layer.startTimeMs).toFloat() / durationMs).coerceIn(0.02f, (1f - startFraction).coerceAtLeast(0.02f)); Box(Modifier.offset(x = (timelineWidth.value * startFraction).dp).fillMaxWidth(widthFraction).height(38.dp).padding(2.dp).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer).pointerInput(layer.id, layer.startTimeMs, layer.endTimeMs) { detectDragGestures(onDragStart = { onSelect() }) { change, dragAmount -> change.consume(); onMove((dragAmount.x / pxPerMs).roundToLong()) } }.clickable(onClick = onSelect)) { Row(Modifier.fillMaxSize(), Alignment.CenterVertically) { if (selected) TrimHandle { deltaPx -> onTrim(layer.startTimeMs + (deltaPx / pxPerMs).roundToLong(), layer.endTimeMs) }; Text(layer.type.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall); if (selected) TrimHandle { deltaPx -> onTrim(layer.startTimeMs, layer.endTimeMs + (deltaPx / pxPerMs).roundToLong()) } } } }

@Composable private fun TrimHandle(onDeltaPx: (Float) -> Unit) { Box(Modifier.width(10.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)).pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDeltaPx(dragAmount.x) } }) }

@Composable private fun SelectedLayerToolbar(layer: ReelLayer, track: ReelTrack, currentTimeMs: Long, onSplit: () -> Unit, onTrimStart: () -> Unit, onTrimEnd: () -> Unit, onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onDelete: () -> Unit) { Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) { Text("Seleccionado: ${layer.type.name.lowercase()}  •  ${formatTime(layer.durationMs)}", Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium); Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) { IconButton(onClick = onSplit, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs < layer.endTimeMs) { Icon(Icons.Default.ContentCut, "Dividir") }; IconButton(onClick = onTrimStart, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs < layer.endTimeMs) { Icon(Icons.Default.ArrowForward, "Recortar inicio") }; IconButton(onClick = onTrimEnd, enabled = currentTimeMs > layer.startTimeMs && currentTimeMs <= layer.endTimeMs) { Icon(Icons.Default.ArrowBack, "Recortar final") }; IconButton(onClick = onMoveLeft, enabled = track.layers.indexOfFirst { it.id == layer.id } > 0) { Icon(Icons.Default.ArrowBack, "Mover a la izquierda") }; IconButton(onClick = onMoveRight, enabled = track.layers.indexOfFirst { it.id == layer.id } in 0 until track.layers.lastIndex) { Icon(Icons.Default.ArrowForward, "Mover a la derecha") }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") } } } }

@Composable private fun StudioToolbar(onAddTrack: (ReelTrackType) -> Unit, onAddText: () -> Unit, onAddImage: () -> Unit) { val items = listOf(ReelTrackType.VIDEO to Pair(Icons.Default.Add, "Medios"), ReelTrackType.AUDIO to Pair(Icons.Default.AudioFile, "Audio"), ReelTrackType.EFFECT to Pair(Icons.Default.FilterAlt, "Filtros")); Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), Arrangement.SpaceEvenly, Alignment.CenterVertically) { items.forEach { (type, item) -> Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = { onAddTrack(type) }) { Icon(item.first, item.second) }; Text(item.second, style = MaterialTheme.typography.labelSmall) } }; Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = onAddText) { Icon(Icons.Default.TextFields, "Texto") }; Text("Texto", style = MaterialTheme.typography.labelSmall) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = onAddImage) { Icon(Icons.Default.Image, "Foto") }; Text("Foto", style = MaterialTheme.typography.labelSmall) } } }

private fun trackName(type: ReelTrackType): String = when (type) { ReelTrackType.VIDEO -> "Video"; ReelTrackType.IMAGE -> "Fotos"; ReelTrackType.AUDIO -> "Audio"; ReelTrackType.TEXT -> "Texto"; ReelTrackType.STICKER -> "Stickers"; ReelTrackType.EFFECT -> "Efectos"; ReelTrackType.SUBTITLE -> "Subtítulos" }
private fun formatTime(timeMs: Long): String { val totalSeconds = timeMs.coerceAtLeast(0L) / 1000L; return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L) }