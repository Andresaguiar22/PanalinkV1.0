package com.example.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.UploadRepository
import com.example.data.supabase.SupabaseClient
import com.example.ui.viewmodel.StatesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class StoryMediaKind { IMAGE, VIDEO }

private data class StoryPalette(val name: String, val top: Color, val bottom: Color)

private val PALETTES = listOf(
    StoryPalette("Pana Link", Color(0xFF0F1C26), Color(0xFF1A3B2A)),
    StoryPalette("Venezuela", Color(0xFF001A33), Color(0xFF003366)),
    StoryPalette("Atardecer", Color(0xFF2B0A3D), Color(0xFF4A1A6B)),
    StoryPalette("Caribe", Color(0xFF002D2D), Color(0xFF005D67)),
    StoryPalette("Noticias", Color(0xFF1E1E1E), Color(0xFF3D3D3D)),
    StoryPalette("Rojo Pana", Color(0xFF330A0A), Color(0xFF5D1B1B)),
)

private data class FreeMusicOption(val name: String, val url: String)

private val FREE_MUSIC = listOf(
    FreeMusicOption("Lofi Joropo", "https://assets.mixkit.co/music/preview/mixkit-lofi-band-925.mp3"),
    FreeMusicOption("Tambor Remix", "https://assets.mixkit.co/music/preview/mixkit-tribal-drums-958.mp3"),
    FreeMusicOption("Gaita Pop", "https://assets.mixkit.co/music/preview/mixkit-pop-05-1522.mp3"),
    FreeMusicOption("Atardecer", "https://assets.mixkit.co/music/preview/mixkit-dreaming-big-31.mp3"),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CleanStoryEditorScreen(
    viewModel: StatesViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaKind by remember { mutableStateOf<StoryMediaKind?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }

    var textContent by remember { mutableStateOf("") }
    var palette by remember { mutableStateOf(PALETTES[0]) }

    var audioName by remember { mutableStateOf<String?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingAudio by remember { mutableStateOf(false) }
    var publishing by remember { mutableStateOf(false) }

    var audioPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var audioPlaying by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            mediaKind = if (mime.startsWith("video/")) StoryMediaKind.VIDEO else StoryMediaKind.IMAGE
        } else {
            Toast.makeText(context, "Sin medio seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            audioName = uri.lastPathSegment ?: "Audio personalizado"
            scope.launch(Dispatchers.IO) {
                try {
                    isUploadingAudio = true
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                    val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
                    val result = UploadRepository().uploadVideo(
                        bytes, mime, "Audio de historia: $audioName",
                        SupabaseClient.currentUser?.id ?: return@launch
                    )
                    if (result.isSuccess) withContext(Dispatchers.Main) { audioUrl = result.getOrThrow().url }
                } finally {
                    withContext(Dispatchers.Main) { isUploadingAudio = false }
                }
            }
        }
    }

    fun togglePreviewAudio() {
        if (audioPlaying) {
            audioPlayer?.stop()
            audioPlayer?.reset()
            audioPlaying = false
            return
        }
        val source = audioUrl ?: audioUri?.toString() ?: return
        try {
            audioPlayer?.stop(); audioPlayer?.reset()
            audioPlayer = android.media.MediaPlayer().apply {
                setDataSource(source)
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnCompletionListener { audioPlaying = false }
            }
            audioPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo reproducir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.stop()
            audioPlayer?.release()
        }
    }

    fun resolveIsVideo(): Boolean = mediaKind == StoryMediaKind.VIDEO
    fun resolveMediaType(): String = when (resolveIsVideo()) {
        true -> "video"
        false -> "image"
    }

    fun uploadStory() {
        if (publishing) return
        if (mediaUri == null && textContent.isBlank()) {
            Toast.makeText(context, "Elige foto/vídeo o escribe texto", Toast.LENGTH_SHORT).show()
            return
        }
        if (isUploadingAudio) {
            Toast.makeText(context, "Subiendo audio, espera…", Toast.LENGTH_SHORT).show()
            return
        }
        publishing = true
        scope.launch(Dispatchers.IO) {
            try {
                val mediaFile = if (mediaUri != null) {
                    val mime = context.contentResolver.getType(mediaUri!!)
                        ?: if (resolveIsVideo()) "video/mp4" else "image/jpeg"
                    val dir = File(context.filesDir, "story_editor").apply { mkdirs() }
                    val ext = if (resolveIsVideo()) "mp4" else "jpg"
                    val file = File(dir, "story_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(mediaUri!!)?.use { inS ->
                        file.outputStream().use { out -> inS.copyTo(out) }
                    }
                    file
                } else null

                val resolvedText = textContent.trim().ifBlank { null }
                val result = viewModelRepositoryCreate(
                    viewModel,
                    mediaType = if (mediaFile != null) resolveMediaType() else "text",
                    caption = resolvedText
                        ?: if (resolveIsVideo()) "Pana Vídeo" else "Pana Foto",
                    mediaFile = mediaFile,
                    audioUrl = audioUrl
                )
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(context, "Historia publicada para tus contactos", Toast.LENGTH_LONG).show()
                        onBack()
                    } else {
                        Toast.makeText(context, result.exceptionOrNull()?.localizedMessage ?: "Fallo al publicar", Toast.LENGTH_SHORT).show()
                        publishing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error publicando: ${e.message}", Toast.LENGTH_SHORT).show()
                    publishing = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0D10))) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text("Nueva historia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(visible = publishing, enter = fadeIn(), modifier = Modifier.widthIn(min = 40.dp)) {
                    CircularProgressIndicator(color = Color(0xFF00FF85), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                IconButton(
                    onClick = { uploadStory() },
                    enabled = !publishing && (mediaUri != null || textContent.isNotBlank())
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Publicar", tint = Color(0xFF00FF85))
                }
            }

            // Preview canvas
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(24.dp))
                    .background(brush = Brush.verticalGradient(listOf(palette.top, palette.bottom)))
                    .clickable { showMediaPicker = true }
            ) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (mediaKind == StoryMediaKind.VIDEO) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).size(64.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = .54f)
                    ) {
                        Icon(Icons.Filled.Videocam, null, tint = Color.White, modifier = Modifier.padding(14.dp))
                    }
                }
                Text(
                    text = when {
                        mediaUri != null -> textContent.ifBlank { "Toca para cambiar el medio" }
                        textContent.isNotBlank() -> textContent
                        else -> "Toca para agregar foto, vídeo o escribe texto abajo"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = if (textContent.length > 90) 18.sp else 24.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = if (mediaUri != null || textContent.isNotBlank()) 0.28f else 0f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                if (audioName != null) {
                    Row(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                            .background(Color.Black.copy(alpha = .48f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            audioName ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tools strip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolChip(icon = Icons.Filled.Image, label = "Imagen/Vídeo", selected = mediaUri != null, onClick = { showMediaPicker = true })
                ToolChip(icon = Icons.Filled.TextFields, label = "Texto", selected = textContent.isNotBlank(), onClick = {
                    // Keep focus-free typing: toggle editor
                })
                ToolChip(icon = Icons.Filled.Audiotrack, label = "Audio", selected = audioName != null, onClick = {
                    // Audio panel stays visible below
                })
            }

            // Palette selector (para texto)
            Text("Tema de texto", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp))
            LazyRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PALETTES) { p ->
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .border(if (palette == p) 2.dp else 0.5.dp, Color.White, CircleShape)
                            .background(Brush.linearGradient(listOf(p.top, p.bottom)))
                            .clickable { palette = p }
                    )
                }
            }

            // Audio strip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Audio de fondo", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        audioName ?: "Ningún audio (opcional)",
                        color = if (audioName == null) Color.Gray else Color.White,
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (audioName != null) {
                        IconButton(onClick = { togglePreviewAudio() }) {
                            Icon(if (audioPlaying) Icons.Filled.Stop else Icons.Filled.Audiotrack,
                                contentDescription = if (audioPlaying) "Parar" else "Reproducir",
                                tint = if (audioPlaying) Color(0xFF00FF85) else Color.White)
                        }
                    }
                    OutlinedButton(
                        onClick = { pickAudio.launch("audio/*") },
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isUploadingAudio) {
                            CircularProgressIndicator(color = Color(0xFF00FF85), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.DragHandle, null, Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(if (audioName == null) "Elegir archivo" else "Cambiar", fontSize = 12.sp)
                    }
                }
            }

            // Text input inline
            OutlinedTextField(
                value = textContent,
                onValueChange = { if (it.length <= 240) textContent = it },
                placeholder = { Text("Escribe un texto breve…", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(max = 80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FF85),
                    unfocusedBorderColor = Color.Gray
                ),
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))
        }

        if (showMediaPicker) {
            ModalBottomSheet(onDismissRequest = { showMediaPicker = false }) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Elige el medio", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FREE_MUSIC) { item ->
                            ElevatedButton(onClick = {
                                audioUri = null
                                audioName = item.name
                                audioUrl = item.url
                                showMediaPicker = false
                            }) {
                                Icon(Icons.Filled.LibraryMusic, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(item.name, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("o elige de tus archivos", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(onClick = { showMediaPicker = false; pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
                            Icon(Icons.Filled.Image, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Foto / Vídeo", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF00FF85) else Color(0xFF1C1E24))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 11.sp)
    }
}

private suspend fun viewModelRepositoryCreate(
    viewModel: StatesViewModel,
    mediaType: String,
    caption: String?,
    mediaFile: File?,
    audioUrl: String?
): Result<Unit> {
    return try {
        val repository = com.example.data.repository.StatesRepository()
        val result = repository.createState(
            mediaType = mediaType,
            caption = caption,
            mediaFile = mediaFile,
            audioUrl = audioUrl,
            isReel = false
        )
        result.map { }
    } catch (e: Exception) {
        Result.failure(e)
    }
}