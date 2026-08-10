import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
    fun uploadAndSendMedia(
        uri: android.net.Uri? = null,
        file: java.io.File? = null,
        mimeType: String,
        typeLabel: String,
        replyToId: String?,
        context: android.content.Context,
        fileName: String? = null,
        onProgress: (Boolean) -> Unit
    ) {
        val chatId = currentChatId ?: return
        val otherUserId = currentOtherUserId
        val userId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: return
        
        val actualFile = file ?: uri?.let { getFileFromUri(context, it) } ?: return
        val localUri = uri?.toString() ?: android.net.Uri.fromFile(actualFile).toString()
        val tempId = "temp_${java.util.UUID.randomUUID()}"
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            onProgress(true)
            
            // 1. Insert optimistic message
            val nowStr = com.example.data.supabase.SupabaseClient.getNowIsoString()
            val safeTypeLabel = typeLabel.lowercase(java.util.Locale.ROOT)
            val optimisticMsg = com.example.data.model.Message(
                id = tempId,
                chatId = chatId,
                senderId = userId,
                content = "[$typeLabel]",
                createdAt = nowStr,
                status = "pending_media", // Use "pending_media" so sync worker skips it until upload completes
                replyToMessageId = replyToId,
                mediaUrl = localUri,
                mediaMime = mimeType,
                messageType = safeTypeLabel,
                isGhost = _isGhostMode.value
            )
            messagesRepo.insertLocalMessage(optimisticMsg)
            
            val result = com.example.util.PanalinkMediaManager.uploadMediaAndThumbnail(
                context = context,
                mediaFile = actualFile,
                mimeType = mimeType,
                typeLabel = typeLabel,
                userId = userId,
                caption = ""
            )
            
            if (result.isSuccess) {
                val uploadResult = result.getOrNull()!!
                // 2. Call sendMessage with the same tempId and remote URL!
                messagesRepo.sendMessage(
                    chatId = chatId,
                    content = "[$typeLabel]",
                    replyToId = replyToId,
                    receiverUid = otherUserId,
                    messageType = safeTypeLabel,
                    mediaUrl = uploadResult.url,
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    mediaMime = mimeType,
                    isGhost = _isGhostMode.value,
                    messageId = tempId
                )
            } else {
                messagesRepo.updateLocalMessageStatus(tempId, "failed")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        android.widget.Toast.makeText(
                            context,
                            "Error subiendo archivo",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (t: Throwable) {}
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onProgress(false)
            }
        }
    }
"""

content = re.sub(r'    fun uploadAndSendMedia\([\s\S]*?            withContext\(Dispatchers.Main\) \{\n                onProgress\(false\)\n            \}\n        \}\n    \}', replacement.strip(), content)

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
