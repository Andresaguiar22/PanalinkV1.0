import re

with open('app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt', 'r') as f:
    content = f.read()

target = """    LaunchedEffect(Unit) {
        val loadedPacks = StickerRepository.getCatalog(context)
        packs = loadedPacks
        if (loadedPacks.isNotEmpty()) {
            selectedPackId = loadedPacks.first().id
        }
        isLoading = false
    }"""

replace = """    LaunchedEffect(Unit) {
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

content = content.replace(target, replace)

# Handle coverUrl being blank for pseudo packs
# We can just show emoji for pseudo packs in the tabs instead of image
target_cover = """                            if (pack.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pack.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }"""

replace_cover = """                            if (pack.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pack.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val icon = when(pack.id) {
                                    "recent" -> "🕒"
                                    "favs" -> "⭐"
                                    "saved" -> "💾"
                                    else -> "📦"
                                }
                                Text(text = icon, fontSize = 16.sp)
                            }"""
content = content.replace(target_cover, replace_cover)

with open('app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt', 'w') as f:
    f.write(content)
print("Done")
