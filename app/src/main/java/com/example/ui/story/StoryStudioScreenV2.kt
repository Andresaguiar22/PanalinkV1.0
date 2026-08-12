package com.example.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun StoryStudioScreenV2(
    onClose: () -> Unit,
    onPublish: (StoryStudioDraft) -> Unit,
    onPickPhoto: (Boolean) -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit
) {
    var draft by remember { mutableStateOf(StoryStudioDraft(UUID.randomUUID().toString())) }
    val duration = draft.durationMs()
    Column(Modifier.fillMaxSize().background(Color.Black).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
            Text("Nueva historia", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(enabled = draft.isValid(), onClick = { onPublish(draft) }) { Text("Publicar") }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF111318), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
            Text(if (draft.slides.isEmpty()) "Añade una foto, vídeo o texto" else "Vista previa", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text("${duration / 1000}s / 120s", color = Color.White)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tool("Foto", Icons.Default.PhotoLibrary) { onPickPhoto(false) }
            Tool("Vídeo", Icons.Default.PlayArrow) { onPickVideo() }
            Tool("Texto", Icons.Default.TextFields) { draft = draft.copy(slides = draft.slides + StoryStudioSlide(id = UUID.randomUUID().toString(), kind = StoryStudioKind.TEXT, text = "Tu texto")) }
            Tool("Audio", Icons.Default.MusicNote) { onPickAudio() }
            Tool("Carrusel", Icons.Default.AddPhotoAlternate) { if (draft.slides.size < StoryStudioDraft.MAX_SLIDES) onPickPhoto(true) }
        }
        Spacer(Modifier.height(6.dp))
        Text("Hasta 10 elementos. Máximo: 2 minutos por publicación.", color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Tool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(72.dp).clickable(onClick = onClick)) {
        Icon(icon, label, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
