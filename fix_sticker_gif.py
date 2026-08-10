import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
    fun sendSticker(url: String, preview: String?, replyToId: String?) {
        val chatId = currentChatId ?: return
        val otherUserId = currentOtherUserId
        val isGif = url.lowercase().contains(".gif")
        viewModelScope.launch {
            val msgId = "temp_${java.util.UUID.randomUUID()}"
            val nowStr = com.example.data.supabase.SupabaseClient.getNowIsoString()
            val mType = if (isGif) "gif" else "sticker"
            val mMime = if (isGif) "image/gif" else "image/webp"
            
            // Optimistic UI for Sticker/GIF
            val optimisticMsg = com.example.data.model.Message(
                id = msgId,
                chatId = chatId,
                senderId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: "",
                content = if (isGif) "[GIF]" else "[Sticker]",
                createdAt = nowStr,
                status = "sending",
                replyToMessageId = replyToId,
                mediaUrl = url,
                thumbnailUrl = preview ?: url,
                mediaMime = mMime,
                messageType = mType,
                isGhost = _isGhostMode.value
            )
            messagesRepo.insertLocalMessage(optimisticMsg)
            
            messagesRepo.sendMessage(
                chatId = chatId,
                content = optimisticMsg.content ?: "",
                replyToId = replyToId,
                receiverUid = otherUserId,
                messageType = mType,
                mediaUrl = url,
                thumbnailUrl = preview ?: url,
                mediaMime = mMime,
                isGhost = _isGhostMode.value,
                messageId = msgId
            )
        }
    }
"""

content = re.sub(r'    fun sendSticker\([\s\S]*?            \)[\s]*\}\n    \}', replacement.strip(), content)

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
