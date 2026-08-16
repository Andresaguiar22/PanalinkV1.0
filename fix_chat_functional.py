from pathlib import Path

FILES = {
    "dao": Path("app/src/main/java/com/example/data/database/MessageDao.kt"),
    "repo": Path("app/src/main/java/com/example/data/repository/MessagesRepository.kt"),
    "vm": Path("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt"),
}

for key, path in FILES.items():
    if not path.exists():
        raise SystemExit(f"ERROR: no existe {path}")


def replace_exact(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ERROR: {label}: esperaba 1 coincidencia y encontré {count}")
    return text.replace(old, new, 1)


dao = FILES["dao"].read_text()
repo = FILES["repo"].read_text()
vm = FILES["vm"].read_text()

# 1) Normalize Realtime thread_messages to the local chatId in Room.
old = '''    @Query("SELECT * FROM local_messages WHERE id = :id")\n    suspend fun getMessageById(id: String): MessageEntity?\n'''
new = '''    @Query("SELECT * FROM local_messages WHERE id = :id")\n    suspend fun getMessageById(id: String): MessageEntity?\n\n    @Query("SELECT id FROM local_chats WHERE threadId = :threadId LIMIT 1")\n    suspend fun getChatIdByThreadId(threadId: String): String?\n'''
dao = replace_exact(dao, old, new, "MessageDao.getChatIdByThreadId")

old = '''    @Transaction\n    suspend fun insertMessage(message: MessageEntity) {\n        insertMessageRaw(message)\n        updateChatMetadataForMessage(message)\n    }\n\n    @Transaction\n    suspend fun insertMessages(messages: List<MessageEntity>) {\n        insertMessagesRaw(messages)\n        messages.forEach { updateChatMetadataForMessage(it) }\n    }\n'''
new = '''    private suspend fun normalizeChatIdentity(message: MessageEntity): MessageEntity {\n        // The realtime payload for thread_messages carries thread_id, while\n        // Room/local UI uses the canonical chat id. Keep an actual local chat\n        // id untouched and only resolve unknown ids through local_chats.threadId.\n        if (hasChat(message.chatId) > 0) return message\n        val localChatId = getChatIdByThreadId(message.chatId)\n        return if (!localChatId.isNullOrBlank()) message.copy(chatId = localChatId) else message\n    }\n\n    @Transaction\n    suspend fun insertMessage(message: MessageEntity) {\n        val normalized = normalizeChatIdentity(message)\n        insertMessageRaw(normalized)\n        updateChatMetadataForMessage(normalized)\n    }\n\n    @Transaction\n    suspend fun insertMessages(messages: List<MessageEntity>) {\n        val normalizedMessages = messages.map { normalizeChatIdentity(it) }\n        insertMessagesRaw(normalizedMessages)\n        normalizedMessages.forEach { updateChatMetadataForMessage(it) }\n    }\n'''
dao = replace_exact(dao, old, new, "MessageDao chat identity normalization")

# 2) Persistent local delete-for-me state + retry queue.
old = '''    private val userDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()\n\n    fun getUserDeletedMessageIds(): Set<String> = userDeletedMessageIds\n'''
new = '''    private val userDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()\n    private val pendingUserDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()\n    private val userDeletedPrefs by lazy {\n        PanaApplication.instance.getSharedPreferences("message_privacy", android.content.Context.MODE_PRIVATE)\n    }\n    private var lastUserDeleteSyncAt = 0L\n\n    fun getUserDeletedMessageIds(): Set<String> = userDeletedMessageIds\n'''
repo = replace_exact(repo, old, new, "persistent delete-for-me fields")

old = '''    init {\n        repositoryScope.launch {\n'''
new = '''    init {\n        try {\n            userDeletedMessageIds.addAll(userDeletedPrefs.getStringSet("deleted_for_me_ids", emptySet()).orEmpty())\n            pendingUserDeletedMessageIds.addAll(userDeletedPrefs.getStringSet("pending_deleted_for_me_ids", emptySet()).orEmpty())\n        } catch (e: Exception) {\n            Log.w(TAG, "Unable to restore persisted delete-for-me state", e)\n        }\n        repositoryScope.launch {\n'''
repo = replace_exact(repo, old, new, "restore persistent delete-for-me state")

# 3) Shared media-type helper so playlist messages are not blocked as uploads.
old = '''    private fun isValidUuid(uuidStr: String?): Boolean {\n'''
new = '''    private fun requiresMediaUpload(messageType: String?): Boolean {\n        val type = messageType?.lowercase()?.trim().orEmpty()\n        return type in setOf("image", "video", "audio", "voice", "voice_note", "document") ||\n            type.startsWith("image/") ||\n            type.startsWith("video/") ||\n            type.startsWith("audio/") ||\n            type.startsWith("application/") ||\n            type.startsWith("text/")\n    }\n\n    private fun isValidUuid(uuidStr: String?): Boolean {\n'''
repo = replace_exact(repo, old, new, "requiresMediaUpload helper")

old = '''                if (entity.messageType != null && entity.messageType != "text" && entity.mediaUrl.isNullOrEmpty()) {\n                    Log.w(TAG, "Skipping sync for multimedia message ${entity.id} because mediaUrl is missing (upload incomplete/failed)")\n                    if (entity.localMediaUri != null) scheduleMediaUpload(entity.id)\n                    continue\n                }\n'''
new = '''                if (requiresMediaUpload(entity.messageType) && entity.mediaUrl.isNullOrEmpty()) {\n                    Log.w(TAG, "Skipping sync for upload-backed media message ${entity.id} because mediaUrl is missing")\n                    if (entity.localMediaUri != null) scheduleMediaUpload(entity.id)\n                    continue\n                }\n'''
repo = replace_exact(repo, old, new, "pending multimedia gate")

old = '''            if (messageType.lowercase() != "text" && mediaUrl.isNullOrBlank()) {\n'''
new = '''            if (requiresMediaUpload(messageType) && mediaUrl.isNullOrBlank()) {\n'''
repo = replace_exact(repo, old, new, "sendMessage media gate")

# 4) Persistent delete-for-me + retry on the next sync window.
old = '''            if (currentUid != null) {\n                val participantRes = runCall { authHeader ->\n'''
new = '''            if (currentUid != null) {\n                syncPendingUserDeletedMessages()\n                val participantRes = runCall { authHeader ->\n'''
repo = replace_exact(repo, old, new, "delete-for-me sync hook")

old = '''    suspend fun deleteMessageForMe(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {\n        userDeletedMessageIds.add(messageId)\n'''
new = '''    suspend fun deleteMessageForMe(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {\n        userDeletedMessageIds.add(messageId)\n        pendingUserDeletedMessageIds.add(messageId)\n        try {\n            userDeletedPrefs.edit()\n                .putStringSet("deleted_for_me_ids", userDeletedMessageIds.toSet())\n                .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())\n                .apply()\n        } catch (e: Exception) {\n            Log.w(TAG, "Unable to persist delete-for-me state", e)\n        }\n'''
repo = replace_exact(repo, old, new, "delete-for-me persistence")

old = '''            if (response != null && response.isSuccessful) {\n                Result.success(true)\n            } else {\n                Log.w(TAG, "deleteMessageForMe: RPC returned code ${response?.code()}")\n                Result.success(true)\n            }\n'''
new = '''            if (response != null && response.isSuccessful) {\n                pendingUserDeletedMessageIds.remove(messageId)\n                try {\n                    userDeletedPrefs.edit()\n                        .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())\n                        .apply()\n                } catch (e: Exception) {\n                    Log.w(TAG, "Unable to persist delete-for-me sync state", e)\n                }\n                Result.success(true)\n            } else {\n                Log.w(TAG, "deleteMessageForMe: RPC returned code ${response?.code()}; kept in local pending-delete queue")\n                Result.success(true)\n            }\n'''
repo = replace_exact(repo, old, new, "delete-for-me RPC result handling")

# Insert helper before toggleMessageFavorite.
old = '''    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {\n'''
new = '''    private suspend fun syncPendingUserDeletedMessages() {\n        val now = System.currentTimeMillis()\n        if (pendingUserDeletedMessageIds.isEmpty() || now - lastUserDeleteSyncAt < 60000L) return\n        lastUserDeleteSyncAt = now\n\n        val service = SupabaseClient.apiService ?: return\n        for (messageId in pendingUserDeletedMessageIds.toList()) {\n            try {\n                val response = runCall { auth ->\n                    service.deleteMessageForMeRpc(\n                        apiKey = SupabaseClient.supabaseAnonKey,\n                        authorization = auth,\n                        params = mapOf("p_message_id" to messageId)\n                    )\n                }\n                if (response?.isSuccessful == true) {\n                    pendingUserDeletedMessageIds.remove(messageId)\n                }\n            } catch (e: Exception) {\n                Log.d(TAG, "Pending delete-for-me retry failed for $messageId", e)\n            }\n        }\n\n        try {\n            userDeletedPrefs.edit()\n                .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())\n                .apply()\n        } catch (e: Exception) {\n            Log.w(TAG, "Unable to persist pending delete-for-me queue", e)\n        }\n    }\n\n    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {\n'''
repo = replace_exact(repo, old, new, "pending delete-for-me helper insertion")

# 5) Keep message text in the composer when an actual send failure occurs.
old = '''        viewModelScope.launch {\n            messagesRepo.sendMessage(chatId, text, replyToId = replyToId, receiverUid = otherId, isGhost = _isGhostMode.value)\n            _inputMessage.value = ""\n        }\n'''
new = '''        viewModelScope.launch {\n            val result = messagesRepo.sendMessage(\n                chatId,\n                text,\n                replyToId = replyToId,\n                receiverUid = otherId,\n                isGhost = _isGhostMode.value\n            )\n            if (result.isSuccess) {\n                _inputMessage.value = ""\n            } else {\n                Log.w("ChatViewModel", "Message send failed; preserving composer text")\n            }\n        }\n'''
vm = replace_exact(vm, old, new, "composer preservation on send failure")

# Write only after every assertion above passed.
FILES["dao"].write_text(dao)
FILES["repo"].write_text(repo)
FILES["vm"].write_text(vm)

print("OK: chat functional fixes applied")
print(" - Realtime threadId -> local chatId normalization")
print(" - playlist/playlist_share no longer treated as upload-blocked media")
print(" - delete-for-me persisted and retried after reconnect")
print(" - composer text preserved on real send failure")
