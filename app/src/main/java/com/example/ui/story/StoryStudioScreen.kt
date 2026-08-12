package com.example.ui.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.data.supabase.SupabaseClient
import com.example.worker.SocialMediaUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private data class StudioMedia(val uri: Uri, val isVideo: Boolean)

@Composable
fun StoryStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var media by remember { mutableStateOf<List<StudioMedia>>(emptyList()) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var text by remember { mutableStateOf("") }
    var background by remember { mutableStateOf(Color(0xFF161824)) }
    var photoDurationMs by remember { mutableLongStateOf(5000L) }
    var publishing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val accepted = uris.take(StoryStudioDraft.MAX_SLIDES).mapNotNull { uri ->
            val mime = context.contentResolver.getType(uri) ?: return@mapNotNull null
            when {
                mime.startsWith("image/") -> StudioMedia(uri, false)
                mime.startsWith("video/") -> StudioMedia(uri, true)
                else -> null
            }
        }
        media = accepted
    }

    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("image/") || mime.startsWith("video/")) {
                media = listOf(StudioMedia(uri, mime.startsWith("video/")))
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && context.contentResolver.getType(uri).orEmpty().startsWith("audio/")) audioUri = uri
    }

    fun publish() {
        if (publishing) return
        if (media.isEmpty() && text.isBlank()) {
            message = "Agrega una foto, vídeo o texto"
            return
        }
        publishing = true
        message = "Preparando historia..."
        scope.launch {
            try {
                val uid = SupabaseClient.currentUser?.id ?: error("Debes iniciar sesión para publicar")
                val pendingDir = File(context.filesDir, "pending_media/stories").apply { mkdirs() }

                val localSlides = withContext(Dispatchers.IO) {
                    if (media.isNotEmpty()) {
                        media.map { item ->
                            val local = copyUriToFile(context, item.uri, pendingDir)
                            StoryStudioSlide(
                                id = UUID.randomUUID().toString(),
                                kind = if (item.isVideo) StoryStudioKind.VIDEO else StoryStudioKind.PHOTO,
                                uri = local.absolutePath,
                                text = text,
                                backgroundHex = background.toHex(),
                                durationMs = if (item.isVideo) videoDurationMs(local) else photoDurationMs
                            )
                        }
                    } else {
                        val textImage = renderTextSlide(context, pendingDir, text, background)
                        listOf(
                            StoryStudioSlide(
                                id = UUID.randomUUID().toString(),
                                kind = StoryStudioKind.TEXT,
                                uri = textImage.absolutePath,
                                text = "",
                                backgroundHex = background.toHex(),
                                durationMs = photoDurationMs
                            )
                        )
                    }
                }

                val localAudio = audioUri?.let { withContext(Dispatchers.IO) { copyUriToFile(context, it, pendingDir) } }
                val draft = StoryStudioDraft(
                    id = UUID.randomUUID().toString(),
                    slides = localSlides,
                    audioUri = localAudio?.absolutePath
                )
                require(draft.isValid()) { "La historia debe durar entre 1 segundo y 2 minutos" }

                val composed = withContext(Dispatchers.IO) {
                    message = "Componiendo ${formatDuration(draft.durationMs())}..."
                    StoryMediaComposer(context).compose(
                        draft = draft,
                        outputFile = File(pendingDir, "story_${draft.id}.mp4"),
                        onProgress = { progress -> message = "Componiendo historia: $progress%" }
                    )
                }

                val id = draft.id
                val entity = PendingUploadEntity(
                    id = id,
                    userId = uid,
                    uploadType = "STATE",
                    localFilePath = composed.absolutePath,
                    mimeType = "video/mp4",
                    caption = text.ifBlank { null },
                    metadataJson = "{\"schema\":3,\"source\":\"story_studio\",\"durationMs\":${draft.durationMs()},\"slides\":${draft.slides.size}}",
                    status = "pending"
                )
                PanalinkDatabase.getDatabase(context).pendingUploadDao().insertUpload(entity)

                val request = OneTimeWorkRequestBuilder<SocialMediaUploadWorker>()
                    .setInputData(workDataOf("uploadId" to id))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .addTag("story_upload")
                    .build()
                WorkManager.getInstance(context).enqueue(request)

                message = "Historia lista. Se subirá automáticamente."
                onBack()
            } catch (e: Exception) {
                message = e.localizedMessage ?: "No se pudo preparar la historia"
            } finally {
                publishing = false
            }
        }
    }

    val estimatedDuration = media.sumOf { if (it.isVideo) 0L else photoDurationMs }
    Column(Modifier.fillMaxSize().background(Color(0xFF090A0F))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !publishing) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
            Text("Nueva historia", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Button(onClick = ::publish, enabled = !publishing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))) {
                Text(if (publishing) "Preparando…" else "Publicar", color = Color.Black)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            if (media.isNotEmpty()) {
                AsyncImage(model = media.first().uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(470.dp))
            } else {
                Box(Modifier.fillMaxWidth().height(470.dp).background(background, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Text(text.ifBlank { "Escribe algo para comenzar" }, color = Color.White, fontSize = 28.sp)
                }
            }
        }

        if (media.isNotEmpty()) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(media) { _, item ->
                    AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(58.dp))
                }
                item {
                    OutlinedButton(onClick = { multiPicker.launch("*/*") }, modifier = Modifier.size(58.dp), contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Add, "Agregar")
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ToolButton(Icons.Default.PhotoLibrary, "Foto") { singlePicker.launch("image/*") }
            ToolButton(Icons.Default.PhotoLibrary, "Carrusel") { multiPicker.launch("*/*") }
            ToolButton(Icons.Default.TextFields, "Texto") { text = if (text.isBlank()) "Tu historia aquí" else "" }
            ToolButton(Icons.Default.MusicNote, "Audio") { audioPicker.launch("audio/*") }
            ToolButton(Icons.Default.PlayArrow, "Preview") { message = "Vista previa: ${formatDuration(estimatedDuration.coerceAtLeast(photoDurationMs))}" }
        }

        Slider(
            value = photoDurationMs.toFloat(),
            onValueChange = { photoDurationMs = it.toLong() },
            valueRange = 1000f..30_000f,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text("Fotos: ${photoDurationMs / 1000}s · máximo total 02:00", color = Color.Gray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        message?.let { Text(it, color = Color.LightGray, modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(6.dp)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

private fun copyUriToFile(context: Context, uri: Uri, directory: File): File {
    val mime = context.contentResolver.getType(uri).orEmpty()
    val ext = when {
        mime.contains("mp4") -> ".mp4"
        mime.contains("webm") -> ".webm"
        mime.contains("png") -> ".png"
        mime.startsWith("audio/") && mime.contains("mpeg") -> ".mp3"
        mime.startsWith("audio/") -> ".m4a"
        else -> ".jpg"
    }
    val target = File(directory, "story_${UUID.randomUUID()}$ext")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo leer el archivo seleccionado" }
        target.outputStream().use { output -> input.copyTo(output) }
    }
    require(target.exists() && target.length() > 0) { "El archivo seleccionado está vacío" }
    return target
}

private fun videoDurationMs(file: File): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(1L)
            ?: throw IllegalArgumentException("No se pudo determinar la duración del vídeo")
    } finally {
        retriever.release()
    }
}

private fun renderTextSlide(context: Context, directory: File, text: String, color: Color): File {
    val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val lines = text.chunked(22)
    val startY = 960f - ((lines.size - 1) * 45f)
    lines.forEachIndexed { index, line -> canvas.drawText(line, 540f, startY + index * 90f, paint) }
    val file = File(directory, "story_text_${UUID.randomUUID()}.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

private fun Color.toHex(): String = "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
private fun formatDuration(ms: Long): String = "%02d:%02d".format(ms / 60_000, (ms / 1_000) % 60)
