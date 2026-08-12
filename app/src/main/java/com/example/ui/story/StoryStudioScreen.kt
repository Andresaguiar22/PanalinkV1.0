package com.example.ui.story

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.data.supabase.SupabaseClient
import com.example.worker.SocialMediaUploadWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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
    var durationMs by remember { mutableLongStateOf(5000L) }
    var publishing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val accepted = uris.take(StoryStudioDraft.MAX_SLIDES).mapNotNull { uri ->
            val mime = context.contentResolver.getType(uri) ?: return@mapNotNull null
            if (mime.startsWith("image/") || mime.startsWith("video/")) {
                StudioMedia(uri, mime.startsWith("video/"))
            } else null
        }
        media = accepted
        durationMs = accepted.sumOf { if (it.isVideo) 0L else 5000L }.coerceAtLeast(5000L)
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> audioUri = uri }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            if (mime.startsWith("image/") || mime.startsWith("video/")) {
                media = listOf(StudioMedia(uri, mime.startsWith("video/")))
                durationMs = if (mime.startsWith("video/")) 0L else 5000L
            }
        }
    }

    fun publish() {
        if (publishing) return
        if (media.isEmpty() && text.isBlank()) {
            message = "Agrega una foto, vídeo o texto"
            return
        }
        val total = if (media.any { it.isVideo }) {
            // The real video duration is validated again by the worker.
            StoryStudioDraft.MAX_DURATION_MS
        } else {
            (durationMs * media.size.coerceAtLeast(1)).coerceAtMost(StoryStudioDraft.MAX_DURATION_MS)
        }
        if (total > StoryStudioDraft.MAX_DURATION_MS) {
            message = "La historia no puede superar 2 minutos"
            return
        }
        publishing = true
        scope.launch {
            try {
                val uid = SupabaseClient.currentUser?.id
                    ?: error("Debes iniciar sesión para publicar")
                val pendingDir = File(context.filesDir, "pending_media/stories")
                pendingDir.mkdirs()

                val selected = media.firstOrNull()
                if (selected == null) {
                    message = "El modo texto requiere el renderizador de composición"
                    return@launch
                }
                val localFile = withContext(Dispatchers.IO) {
                    copyUriToFile(context, selected.uri, pendingDir)
                }
                val id = UUID.randomUUID().toString()
                val metadata = buildString {
                    append("{\"schema\":2,\"slides\":[")
                    media.forEachIndexed { index, item ->
                        if (index > 0) append(',')
                        append("{\"uri\":\"")
                        append(item.uri.toString().replace("\\", "\\\\").replace("\"", "\\\""))
                        append("\",\"video\":${item.isVideo}}")
                    }
                    append("],\"audioUri\":")
                    append(if (audioUri == null) "null" else "\"${audioUri.toString().replace("\"", "\\\"")}\"")
                    append(",\"text\":\"")
                    append(text.replace("\\", "\\\\").replace("\"", "\\\""))
                    append("\",\"background\":\"")
                    append("#${Integer.toHexString(android.graphics.Color.rgb((background.red * 255).toInt(), (background.green * 255).toInt(), (background.blue * 255).toInt()))}")
                    append("\",\"durationMs\":$total}")
                }
                val entity = PendingUploadEntity(
                    id = id,
                    userId = uid,
                    uploadType = "STATE",
                    localFilePath = localFile.absolutePath,
                    mimeType = context.contentResolver.getType(selected.uri) ?: "image/jpeg",
                    caption = text.ifBlank { null },
                    metadataJson = metadata,
                    status = "pending"
                )
                PanalinkDatabase.getDatabase(context).pendingUploadDao().insertUpload(entity)
                val request = OneTimeWorkRequestBuilder<SocialMediaUploadWorker>()
                    .setInputData(workDataOf("uploadId" to id))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .addTag("story_upload")
                    .build()
                WorkManager.getInstance(context).enqueue(request)
                message = "Historia guardada y puesta en cola"
                onBack()
            } catch (e: Exception) {
                message = e.localizedMessage ?: "No se pudo preparar la historia"
            } finally {
                publishing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090A0F))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
            Text("Nueva historia", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Button(onClick = ::publish, enabled = !publishing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))) {
                Text(if (publishing) "Preparando…" else "Publicar", color = Color.Black)
            }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (media.isNotEmpty()) {
                    AsyncImage(
                        model = media.first().uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(470.dp)
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(470.dp).background(background, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Text(text.ifBlank { "Escribe algo para comenzar" }, color = Color.White, fontSize = 28.sp)
                    }
                }
            }
        }

        if (media.isNotEmpty()) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(media) { item ->
                    AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(58.dp))
                }
                item {
                    OutlinedButton(onClick = { multiPicker.launch("image/*") }, modifier = Modifier.size(58.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Default.Add, "Agregar foto")
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ToolButton(Icons.Default.PhotoLibrary, "Foto") { singlePicker.launch("image/*") }
            ToolButton(Icons.Default.PhotoLibrary, "Carrusel") { multiPicker.launch("image/*") }
            ToolButton(Icons.Default.TextFields, "Texto") { text = if (text.isBlank()) "Tu historia aquí" else "" }
            ToolButton(Icons.Default.MusicNote, "Audio") { audioPicker.launch("audio/*") }
        }
        Slider(value = durationMs.coerceIn(1000L, StoryStudioDraft.MAX_DURATION_MS).toFloat(), onValueChange = { durationMs = it.toLong() }, valueRange = 1000f..120000f, modifier = Modifier.padding(horizontal = 20.dp))
        Text("Duración máxima: 02:00", color = Color.Gray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        message?.let { Text(it, color = Color.LightGray, modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

private fun copyUriToFile(context: Context, uri: Uri, directory: File): File {
    val ext = when (context.contentResolver.getType(uri)) {
        "video/mp4" -> ".mp4"
        "video/webm" -> ".webm"
        "image/png" -> ".png"
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
