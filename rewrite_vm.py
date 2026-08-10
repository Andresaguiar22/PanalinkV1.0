import re

with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorViewModel.kt", "r") as f:
    content = f.read()

new_save_method = """
    fun saveSticker(context: Context, emoji: String) {
        val uri = _processedImageUri.value ?: return
        val file = java.io.File(uri.path!!)
        
        viewModelScope.launch {
            _isProcessing.value = true
            val result = StickerCreationRepository.uploadAndCreateSticker(
                context = context,
                file = file,
                name = "Sticker",
                emoji = emoji,
                mimeType = _mediaType.value
            )
            
            if (result.isSuccess) {
                val url = result.getOrThrow()
                // Save locally so it appears in recent/saved
                val stickerResult = StickerResult(url = url, preview = url)
                StickerRepository.saveSticker(context, stickerResult)
                _successUrl.value = url
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            _isProcessing.value = false
        }
    }
"""

content = re.sub(r"    fun saveSticker\(context: Context, emoji: String\) \{.*?\n    \}\n\n    fun reset", new_save_method + "\n    fun reset", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/features/stickers/editor/StickerEditorViewModel.kt", "w") as f:
    f.write(content)
