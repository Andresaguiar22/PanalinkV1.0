import re

with open('app/src/main/java/com/example/ui/screen/ChatScreen.kt', 'r') as f:
    content = f.read()

target = """                                        onToggleFavorite = { viewModel.toggleFavorite(it) },"""
replace = """                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onSaveSticker = { url -> viewModel.saveSticker(url, context) },
                                        onToggleStickerFavorite = { url -> viewModel.toggleStickerFavorite(url, context) },"""

content = content.replace(target, replace)

target2 = """        onToggleFavorite = { viewModel.toggleFavorite(it) },"""
replace2 = """        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onSaveSticker = { url -> viewModel.saveSticker(url, context) },
        onToggleStickerFavorite = { url -> viewModel.toggleStickerFavorite(url, context) },"""
content = content.replace(target2, replace2)

with open('app/src/main/java/com/example/ui/screen/ChatScreen.kt', 'w') as f:
    f.write(content)
print("Done")
