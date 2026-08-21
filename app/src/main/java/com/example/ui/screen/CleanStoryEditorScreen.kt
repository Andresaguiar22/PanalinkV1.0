package com.example.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
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

private enum class StoryMode { IMAGE, VIDEO, TEXT }

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

    var mode by remember { mutableStateOf(StoryMode.IMAGE) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaMime by remember { mutableStateOf<String?>(null) }

    var textContent by remember { mutableStateOf("") }
    var overlayTextEnabled by remember { mutableStateOf(false) }
    var palette by remember { mutableStateOf(PALETTES[0]) }

    var audioEnabled by remember { mutableStateOf(false) }
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
            mediaMime = context.contentResolver.getType(uri)
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
            audioUrl = null
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

    fun launchMediaPicker() {
        val kind = when (mode) {
            StoryMode.IMAGE -> ActivityResultContracts.PickVisualMedia.ImageOnly
            StoryMode.VIDEO -> ActivityResultContracts.PickVisualMedia.VideoOnly
            StoryMode.TEXT -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }
        pickMedia.launch(PickVisualMediaRequest(kind))
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

    fun canPublish(): Boolean = when (mode) {
        StoryMode.TEXT -> textContent.isNotBlank()
        else -> mediaUri != null
    }

    fun uploadStory() {
        if (publishing) return
        if (!canPublish()) {
            Toast.makeText(
                context,
                if (mode == StoryMode.TEXT) "Escribe un texto para tu historia" else "Elige una imagen o vídeo",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (isUploadingAudio) {
            Toast.makeText(context, "Subiendo audio, espera…", Toast.LENGTH_SHORT).show()
            return
        }
        publishing = true
        scope.launch(Dispatchers.IO) {
            try {
                val isVideo = mode == StoryMode.VIDEO
                val mediaFile = if (mode != StoryMode.TEXT && mediaUri != null) {
                    val dir = File(context.filesDir, "story_editor").apply { mkdirs() }
                    val ext = if (isVideo) "mp4" else "jpg"
                    val file = File(dir, "story_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(mediaUri!!)?.use { inS ->
                        file.outputStream().use { out -> inS.copyTo(out) }
                    }
                    file
                } else null

                val resolvedMime = if (mediaFile != null) {
                    mediaMime ?: if (isVideo) "video/mp4" else "image/jpeg"
                } else null

                val caption = when (mode) {
                    StoryMode.TEXT -> textContent.trim()
                    else -> textContent.trim().takeIf { overlayTextEnabled && it.isNotBlank() }
                        ?: if (isVideo) "Pana Vídeo" else "Pana Foto"
                }

                val result = viewModelRepositoryCreate(
                    viewModel,
                    mediaType = when (mode) {
                        StoryMode.TEXT -> "text"
                        StoryMode.IMAGE -> "image"
                        StoryMode.VIDEO -> "video"
                    },
                    caption = caption,
                    mediaFile = mediaFile,
                    mediaMimeType = resolvedMime,
                    audioUrl = if (audioEnabled) audioUrl else null
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("Nueva historia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = publishing, enter = fadeIn(), modifier = Modifier.widthIn(min = 24.dp)) {
                CircularProgressIndicator(color = Color(0xFF00FF85), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            IconButton(onClick = { uploadStory() }, enabled = !publishing && canPublish()) {
                Icon(Icons.AutoMirrored.Filled.Send, "Publicar", tint = Color(0xFF00FF85))
            }
        }

        // Contenido desplazable: nada queda fuera de la pantalla
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            // Selector de modo: imagen, vídeo o texto (opciones separadas)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip("Imagen", Icons.Filled.Image, mode == StoryMode.IMAGE, Modifier.weight(1f)) {
                    mode = StoryMode.IMAGE; mediaUri = null
                }
                ModeChip("Vídeo", Icons.Filled.Videocam, mode == StoryMode.VIDEO, Modifier.weight(1f)) {
                    mode = StoryMode.VIDEO; mediaUri = null
                }
                ModeChip("Texto", Icons.Filled.TextFields, mode == StoryMode.TEXT, Modifier.weight(1f)) {
                    mode = StoryMode.TEXT
                }
            }

            Spacer(Modifier.height(12.dp))

            if (mode == StoryMode.TEXT) {
                // Vista previa de historia de texto
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(palette.top, palette.bottom))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        textContent.ifBlank { "Tu texto aparecerá aquí" },
                        color = if (textContent.isBlank()) Color.White.copy(alpha = .5f) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("Tema de fondo", color = Color.Gray, fontSize = 11.sp)
                LazyRow(
                    Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PALETTES) { p ->
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(if (palette == p) 2.dp else 0.5.dp, Color.White, CircleShape)
                                .background(Brush.linearGradient(listOf(p.top, p.bottom)))
                                .clickable { palette = p }
                        )
                    }
                }

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { if (it.length <= 240) textContent = it },
                    placeholder = { Text("Escribe tu historia…", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF85),
                        unfocusedBorderColor = Color.Gray
                    ),
                    maxLines = 4
                )
            } else {
                // Vista previa de imagen/vídeo
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(24.dp))
                        .background(Color(0xFF14171C))
                        .clickable { launchMediaPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaUri != null) {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (mode == StoryMode.VIDEO) {
                            Icon(
                                Icons.Filled.Videocam, null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        if (overlayTextEnabled && textContent.isNotBlank()) {
                            Text(
                                textContent,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = .45f))
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (mode == StoryMode.VIDEO) Icons.Filled.Videocam else Icons.Filled.Image,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (mode == StoryMode.VIDEO) "Toca para elegir un vídeo" else "Toca para elegir una imagen",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // Texto sobre la imagen/vídeo: opción separada y opcional
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Texto sobre el medio (opcional)", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = overlayTextEnabled,
                        onCheckedChange = { overlayTextEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00FF85))
                    )
                }
                if (overlayTextEnabled) {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { if (it.length <= 120) textContent = it },
                        placeholder = { Text("Texto que irá sobre el medio…", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FF85),
                            unfocusedBorderColor = Color.Gray
                        ),
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // Audio de fondo: opción separada y opcional
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Audio de fondo (opcional)", color = Color.White, fontSize = 13.sp)
                Switch(
                    checked = audioEnabled,
                    onCheckedChange = { audioEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00FF85))
                )
            }
            if (audioEnabled) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FREE_MUSIC) { item ->
                        ElevatedButton(onClick = {
                            audioUri = null
                            audioName = item.name
                            audioUrl = item.url
                        }) {
                            Icon(Icons.Filled.LibraryMusic, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(item.name, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        audioName ?: "Ningún audio seleccionado",
                        color = if (audioName == null) Color.Gray else Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (audioName != null) {
                            IconButton(onClick = { togglePreviewAudio() }) {
                                Icon(
                                    if (audioPlaying) Icons.Filled.Stop else Icons.Filled.Audiotrack,
                                    contentDescription = if (audioPlaying) "Parar" else "Reproducir",
                                    tint = if (audioPlaying) Color(0xFF00FF85) else Color.White
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { pickAudio.launch("audio/*") },
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isUploadingAudio) {
                                CircularProgressIndicator(color = Color(0xFF00FF85), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(if (audioName == null) "Elegir archivo" else "Cambiar", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { uploadStory() },
                enabled = !publishing && canPublish(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Publicar historia", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF00FF85) else Color(0xFF1C1E24))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
    }
}

private suspend fun viewModelRepositoryCreate(
    viewModel: StatesViewModel,
    mediaType: String,
    caption: String?,
    mediaFile: File?,
    mediaMimeType: String?,
    audioUrl: String?
): Result<Unit> {
    return try {
        val repository = com.example.data.repository.StatesRepository()
        val result = repository.createState(
            mediaType = mediaType,
            caption = caption,
            mediaFile = mediaFile,
            mediaMimeType = mediaMimeType,
            audioUrl = audioUrl,
            isReel = false
        )
        result.map { }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
