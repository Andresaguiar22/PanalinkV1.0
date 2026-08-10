with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

new_methods = """
    fun saveSticker(stickerUrl: String, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.model.StickerResult(url = stickerUrl, preview = stickerUrl)
            com.example.data.repository.StickerRepository.saveSticker(context, result)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Sticker guardado", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleStickerFavorite(stickerUrl: String, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.model.StickerResult(url = stickerUrl, preview = stickerUrl)
            val isFav = com.example.data.repository.StickerRepository.toggleFavoriteSticker(context, result)
            withContext(Dispatchers.Main) {
                val msg = if (isFav) "Añadido a favoritos" else "Quitado de favoritos"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}"""

content = content.replace("}\n", "}\n" + new_methods)
# actually, let's just insert it before the last brace.
idx = content.rfind("}")
content = content[:idx] + new_methods + "\n"

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
    f.write(content)
print("Done")
