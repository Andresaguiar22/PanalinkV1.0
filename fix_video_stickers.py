import re

# 1. Update Repository
with open("app/src/main/java/com/example/features/stickers/editor/StickerCreationRepository.kt", "r") as f:
    repo_content = f.read()

repo_content = repo_content.replace(
    "suspend fun uploadAndCreateSticker(\n        context: Context,\n        file: File,\n        name: String,\n        emoji: String\n    ): String?",
    "suspend fun uploadAndCreateSticker(\n        context: Context,\n        file: File,\n        name: String,\n        emoji: String,\n        mimeType: String = \"image/webp\"\n    ): String?"
)
repo_content = repo_content.replace('mimeType = "image/webp"', 'mimeType = mimeType')
repo_content = repo_content.replace('put("media_type", "image/webp")', 'put("media_type", mimeType)')

with open("app/src/main/java/com/example/features/stickers/editor/StickerCreationRepository.kt", "w") as f:
    f.write(repo_content)

# 2. Update Processor
with open("app/src/main/java/com/example/features/stickers/editor/StickerProcessor.kt", "r") as f:
    proc_content = f.read()

new_proc = """
    suspend fun processVideoToSticker(context: Context, videoUri: android.net.Uri): File? = withContext(Dispatchers.IO) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLongOrNull() ?: 0L
            retriever.release()

            if (timeInMillis > 10000) {
                // Límite de 10 segundos
                return@withContext null
            }

            val inputStream = context.contentResolver.openInputStream(videoUri) ?: return@withContext null
            val outputFile = File(context.cacheDir, "sticker_${UUID.randomUUID()}.mp4")
            java.io.FileOutputStream(outputFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
"""
proc_content = proc_content.replace("}\n}", "}\n" + new_proc)

with open("app/src/main/java/com/example/features/stickers/editor/StickerProcessor.kt", "w") as f:
    f.write(proc_content)

# 3. Update ViewModel
with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorViewModel.kt", "r") as f:
    vm_content = f.read()

vm_content = vm_content.replace(
    "private val _successUrl = MutableStateFlow<String?>(null)",
    "private val _mediaType = MutableStateFlow(\"image/webp\")\n\n    private val _successUrl = MutableStateFlow<String?>(null)"
)

new_vm_video = """
    fun processVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val file = StickerProcessor.processVideoToSticker(context, uri)
            if (file != null) {
                _processedImageUri.value = Uri.fromFile(file)
                _mediaType.value = "video/mp4"
            }
            _isProcessing.value = false
        }
    }
"""
vm_content = vm_content.replace("    fun saveSticker", new_vm_video + "\n    fun saveSticker")
vm_content = vm_content.replace("emoji = emoji\n            )", "emoji = emoji,\n                mimeType = _mediaType.value\n            )")
vm_content = vm_content.replace(
    "_processedImageUri.value = Uri.fromFile(file)\n            }",
    "_processedImageUri.value = Uri.fromFile(file)\n                _mediaType.value = \"image/webp\"\n            }"
)

with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorViewModel.kt", "w") as f:
    f.write(vm_content)

# 4. Update Screen UI
with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorScreen.kt", "r") as f:
    ui_content = f.read()

ui_content = ui_content.replace(
    "import androidx.compose.material.icons.filled.CameraAlt",
    "import androidx.compose.material.icons.filled.CameraAlt\nimport androidx.compose.material.icons.filled.Videocam\nimport android.widget.Toast"
)

new_launcher = """
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.processVideo(context, it) }
    }
"""
ui_content = ui_content.replace("val cameraLauncher", new_launcher + "\n    val cameraLauncher")

buttons_ui = """Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Imagen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imagen")
                        }
                        Button(
                            onClick = { videoLauncher.launch("video/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Vídeo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vídeo")
                        }
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cámara")
                        }
                    }"""

ui_content = re.sub(r"Row\(horizontalArrangement = Arrangement\.spacedBy\(16\.dp\)\) \{.*?\n                    \}", buttons_ui, ui_content, flags=re.DOTALL)
ui_content = ui_content.replace('Text("El soporte para videos cortos se activará pronto.", color = Color.Gray, fontSize = 12.sp)', "")

preview_box = """Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A3942))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = processedImageUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }"""

new_preview_box = """Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A3942))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = processedImageUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        if (processedImageUri?.path?.endsWith(".mp4") == true) {
                            Icon(Icons.Default.Videocam, contentDescription = "Sticker Animado", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }"""
ui_content = ui_content.replace(preview_box, new_preview_box)

with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorScreen.kt", "w") as f:
    f.write(ui_content)

