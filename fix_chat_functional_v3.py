from pathlib import Path

REPO = Path("app/src/main/java/com/example/data/repository/MessagesRepository.kt")
CLIENT = Path("app/src/main/java/com/example/data/supabase/SupabaseClient.kt")

for p in (REPO, CLIENT):
    if not p.exists():
        raise SystemExit(f"ERROR: no existe {p}")

def replace_once(text, old, new, label):
    n = text.count(old)
    if n != 1:
        raise SystemExit(f"ERROR: {label}: esperaba 1 coincidencia y encontré {n}. No se modificó ningún archivo.")
    return text.replace(old, new, 1)

repo = REPO.read_text()
client = CLIENT.read_text()

# A) Upload-backed media detection must not classify playlist_share as binary media.
old = '''                if (entity.messageType != null && entity.messageType != "text" && entity.mediaUrl.isNullOrEmpty()) {
                    Log.w(TAG, "Skipping sync for multimedia message ${entity.id} because mediaUrl is missing (upload incomplete/failed)")
                    if (entity.localMediaUri != null) scheduleMediaUpload(entity.id)
                    continue
                }
'''
new = '''                if (requiresMediaUpload(entity.messageType) && entity.mediaUrl.isNullOrEmpty()) {
                    Log.w(TAG, "Skipping sync for upload-backed media message ${entity.id} because mediaUrl is missing")
                    if (entity.localMediaUri != null) scheduleMediaUpload(entity.id)
                    continue
                }
'''
repo = replace_once(repo, old, new, "pending media gate")

old = '''    private fun isValidUuid(uuidStr: String?): Boolean {
'''
new = '''    private fun requiresMediaUpload(messageType: String?): Boolean {
        val type = messageType?.lowercase()?.trim().orEmpty()
        return type in setOf("image", "video", "audio", "voice", "voice_note", "document") ||
            type.startsWith("image/") ||
            type.startsWith("video/") ||
            type.startsWith("audio/") ||
            type.startsWith("application/") ||
            type.startsWith("text/")
    }

    private fun isValidUuid(uuidStr: String?): Boolean {
'''
repo = replace_once(repo, old, new, "media type helper")

old = '''            if (messageType.lowercase() != "text" && mediaUrl.isNullOrBlank()) {
'''
new = '''            if (requiresMediaUpload(messageType) && mediaUrl.isNullOrBlank()) {
'''
repo = replace_once(repo, old, new, "send media gate")

# B) Delete-for-me must survive a failed RPC and retry after reconnect.
old = '''    private val userDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun getUserDeletedMessageIds(): Set<String> = userDeletedMessageIds
'''
new = '''    private val userDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val pendingUserDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val userDeletedPrefs by lazy {
        PanaApplication.instance.getSharedPreferences("message_privacy", android.content.Context.MODE_PRIVATE)
    }
    private var lastUserDeleteSyncAt = 0L

    fun getUserDeletedMessageIds(): Set<String> = userDeletedMessageIds
'''
repo = replace_once(repo, old, new, "delete-for-me fields")

old = '''    init {
        repositoryScope.launch {
'''
new = '''    init {
        try {
            userDeletedMessageIds.addAll(userDeletedPrefs.getStringSet("deleted_for_me_ids", emptySet()).orEmpty())
            pendingUserDeletedMessageIds.addAll(userDeletedPrefs.getStringSet("pending_deleted_for_me_ids", emptySet()).orEmpty())
        } catch (e: Exception) {
            Log.w(TAG, "Unable to restore persisted delete-for-me state", e)
        }
        repositoryScope.launch {
'''
repo = replace_once(repo, old, new, "restore delete-for-me state")

old = '''            if (currentUid != null) {
                val participantRes = runCall { authHeader ->
'''
new = '''            if (currentUid != null) {
                syncPendingUserDeletedMessages()
                val participantRes = runCall { authHeader ->
'''
repo = replace_once(repo, old, new, "delete-for-me retry hook")

old = '''    suspend fun deleteMessageForMe(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        userDeletedMessageIds.add(messageId)
'''
new = '''    suspend fun deleteMessageForMe(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        userDeletedMessageIds.add(messageId)
        pendingUserDeletedMessageIds.add(messageId)
        try {
            userDeletedPrefs.edit()
                .putStringSet("deleted_for_me_ids", userDeletedMessageIds.toSet())
                .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to persist delete-for-me state", e)
        }
'''
repo = replace_once(repo, old, new, "persist delete-for-me")

old = '''            if (response != null && response.isSuccessful) {
                Result.success(true)
            } else {
                Log.w(TAG, "deleteMessageForMe: RPC returned code ${response?.code()}")
                Result.success(true)
            }
'''
new = '''            if (response != null && response.isSuccessful) {
                pendingUserDeletedMessageIds.remove(messageId)
                try {
                    userDeletedPrefs.edit()
                        .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())
                        .apply()
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to persist delete-for-me sync state", e)
                }
                Result.success(true)
            } else {
                Log.w(TAG, "deleteMessageForMe: RPC returned code ${response?.code()}; queued for retry")
                Result.success(true)
            }
'''
repo = replace_once(repo, old, new, "delete-for-me RPC result")

old = '''    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {
'''
new = '''    private suspend fun syncPendingUserDeletedMessages() {
        val now = System.currentTimeMillis()
        if (pendingUserDeletedMessageIds.isEmpty() || now - lastUserDeleteSyncAt < 60000L) return
        lastUserDeleteSyncAt = now

        val service = SupabaseClient.apiService ?: return
        for (messageId in pendingUserDeletedMessageIds.toList()) {
            try {
                val response = runCall { auth ->
                    service.deleteMessageForMeRpc(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        params = mapOf("p_message_id" to messageId)
                    )
                }
                if (response?.isSuccessful == true) {
                    pendingUserDeletedMessageIds.remove(messageId)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Pending delete-for-me retry failed for $messageId", e)
            }
        }

        try {
            userDeletedPrefs.edit()
                .putStringSet("pending_deleted_for_me_ids", pendingUserDeletedMessageIds.toSet())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to persist pending delete-for-me queue", e)
        }
    }

    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {
'''
repo = replace_once(repo, old, new, "delete-for-me retry helper")

# C) Realtime DELETE must also remove thread_messages locally.
old = '''                                    if (table == "messages") {
                                        Log.d(TAG, "Realtime DELETE event received for message ID: $deletedId in table $table")
                                        clientScope.launch {
                                            _realtimeMessageDeletions.emit(deletedId)
                                        }
                                    } else if (table.contains("likes")'''
new = '''                                    if (table == "messages" || table == "thread_messages" || table == "channel_messages") {
                                        Log.d(TAG, "Realtime DELETE event received for message ID: $deletedId in table $table")
                                        clientScope.launch {
                                            _realtimeMessageDeletions.emit(deletedId)
                                        }
                                    } else if (table.contains("likes")'''
client = replace_once(client, old, new, "Realtime DELETE message tables")

# D) Realtime UPDATE/INSERT for messages must include reply/edit/delete metadata already present in the row;
# calculated status is retained from seen_at/delivered_at, so no additional local mutation is needed.

REPO.write_text(repo)
CLIENT.write_text(client)

print("OK: v3 chat delivery/delete/media fixes applied")
print(" - playlist_share no longer blocked by media URL gate")
print(" - delete-for-me persists and retries")
print(" - Realtime DELETE covers messages/thread_messages/channel_messages")
