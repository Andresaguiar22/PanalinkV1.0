import re

with open('app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt', 'r') as f:
    content = f.read()

target = """    LaunchedEffect(Unit) {
        // Sync remote data
        com.example.data.repository.StickerRepository.syncStickersFromRemote(context)
        
        val loadedPacks = StickerRepository.getCatalog(context).toMutableList()
        
        val saved = com.example.data.repository.StickerRepository.getSavedStickers(context)
        if (saved.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("saved", "Guardados", "", saved.map { Sticker(it.url, "Guardado", it.url, "💾", "saved") }))
        }
        
        val favs = com.example.data.repository.StickerRepository.getFavoriteStickers(context)
        if (favs.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("favs", "Favoritos", "", favs.map { Sticker(it.url, "Favorito", it.url, "⭐", "favs") }))
        }
        
        val recents = com.example.data.repository.StickerRepository.getRecentStickers(context)
        if (recents.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("recent", "Recientes", "", recents.map { Sticker(it.url, "Reciente", it.url, "🕒", "recent") }))
        }
        
        packs = loadedPacks
        if (loadedPacks.isNotEmpty()) {
            selectedPackId = loadedPacks.first().id
        }
        isLoading = false
    }"""

replace = """    LaunchedEffect(Unit) {
        // Load cache first
        suspend fun updatePacksUI() {
            val loadedPacks = StickerRepository.getCatalog(context).toMutableList()
            val saved = com.example.data.repository.StickerRepository.getSavedStickers(context)
            if (saved.isNotEmpty()) {
                loadedPacks.add(0, StickerPack("saved", "Guardados", "", saved.map { Sticker(it.url, "Guardado", it.url, "💾", "saved") }))
            }
            val favs = com.example.data.repository.StickerRepository.getFavoriteStickers(context)
            if (favs.isNotEmpty()) {
                loadedPacks.add(0, StickerPack("favs", "Favoritos", "", favs.map { Sticker(it.url, "Favorito", it.url, "⭐", "favs") }))
            }
            val recents = com.example.data.repository.StickerRepository.getRecentStickers(context)
            if (recents.isNotEmpty()) {
                loadedPacks.add(0, StickerPack("recent", "Recientes", "", recents.map { Sticker(it.url, "Reciente", it.url, "🕒", "recent") }))
            }
            packs = loadedPacks
            if (selectedPackId == null && loadedPacks.isNotEmpty()) {
                selectedPackId = loadedPacks.first().id
            }
        }
        
        // Show cached immediately
        updatePacksUI()
        isLoading = false
        
        // Sync in background and refresh
        kotlinx.coroutines.launch {
            com.example.data.repository.StickerRepository.syncStickersFromRemote(context)
            updatePacksUI()
        }
    }"""

content = content.replace(target, replace)

with open('app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt', 'w') as f:
    f.write(content)
print("Done")
