import re

with open('app/src/main/java/com/example/ui/components/chat/bubble/MessageBubbleEngine.kt', 'r') as f:
    content = f.read()

# Add onSaveSticker and onToggleStickerFavorite to signature
sig_target = "    onToggleFavorite: (Message) -> Unit,"
sig_replace = """    onToggleFavorite: (Message) -> Unit,
    onSaveSticker: ((String) -> Unit)? = null,
    onToggleStickerFavorite: ((String) -> Unit)? = null,"""
content = content.replace(sig_target, sig_replace)

# Modify menuItems
menu_target = """                val menuItems = remember(isMe, isFavorited, isPinned, message) {
                    buildList {"""
menu_replace = """                val menuItems = remember(isMe, isFavorited, isPinned, message) {
                    val isStickerMsg = message.messageType == "sticker" || message.textContent.startsWith("[Sticker] ")
                    val stickerUrl = if (isStickerMsg) (message.mediaUrl ?: (if (message.textContent.startsWith("[Sticker] ")) message.textContent.substringAfter("[Sticker] ").trim() else message.textContent)) else null
                    buildList {
                        if (isStickerMsg && stickerUrl != null) {
                            add(PremiumMenuItemData(id = "save_sticker", title = "Guardar sticker", iconEmoji = "💾", onClick = { onSaveSticker?.invoke(stickerUrl) }))
                            add(PremiumMenuItemData(id = "star_sticker", title = "Favorito (Sticker)", iconEmoji = "⭐", onClick = { onToggleStickerFavorite?.invoke(stickerUrl) }))
                        }"""
content = content.replace(menu_target, menu_replace)

with open('app/src/main/java/com/example/ui/components/chat/bubble/MessageBubbleEngine.kt', 'w') as f:
    f.write(content)
print("Done")
