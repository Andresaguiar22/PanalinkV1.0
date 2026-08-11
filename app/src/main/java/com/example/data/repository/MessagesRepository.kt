package com.example.data.repository

import android.util.Log
import com.example.PanaApplication
import com.example.data.database.MessageEntity
import com.example.data.database.PanalinkDatabase
import com.example.data.model.*
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.UUID

import com.example.data.supabase.SessionManager

import androidx.work.*
import com.example.worker.SyncMessagesWorker
import com.example.worker.MediaUploadWorker
import com.example.util.PanalinkMediaManager
import androidx.work.Data

class MessagesRepository private constructor() {

    private val TAG = "MessagesRepository"
    private val db = PanalinkDatabase.getDatabase(PanaApplication.instance)
    private val messageDao = db.messageDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val userDeletedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun getUserDeletedMessageIds(): Set<String> = userDeletedMessageIds

    fun getEffectiveClearedAt(chatId: String, remoteClearedAt: String?): String? {
        val localCleared = localClearedAtMap[chatId]
        return when {
            remoteClearedAt != null && localCleared != null -> {
                if (isTimestampBeforeOrEqual(remoteClearedAt, localCleared)) localCleared else remoteClearedAt
            }
            remoteClearedAt != null -> remoteClearedAt
            else -> localCleared
        }
    }

    init {
        repositoryScope.launch {
            SupabaseClient.realtimeMessageDeletions.collect { messageId ->
                try {
                    messageDao.deleteMessageById(messageId)
                    Log.d(TAG, "Realtime: Deleted message $messageId from local DB")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting message $messageId from Room upon realtime signal", e)
                }
            }
        }
    }

    val localClearedAtMap = java.util.concurrent.ConcurrentHashMap<String, String>()
    val lastSyncTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    companion object {
        @Volatile
        private var instance: MessagesRepository? = null

        fun getInstance(): MessagesRepository {
            return instance ?: synchronized(this) {
                instance ?: MessagesRepository().also { instance = it }
            }
        }
    }

    enum class ChatKind {
        DM,
        CHANNEL,
        LEGACY,
        UNKNOWN
    }

    data class CanonicalChatIdentity(
        val kind: ChatKind,
        val chatId: String,
        val threadId: String? = null,
        val receiverId: String? = null
    )

    suspend fun resolveChatIdentity(chatId: String, receiverHint: String? = null): CanonicalChatIdentity = withContext(Dispatchers.IO) {
        val currentUid = try { SupabaseClient.currentUser?.id } catch (e: Throwable) { null }
        val db = PanalinkDatabase.getDatabase(PanaApplication.instance)
        val chatEntity = try { db.chatDao().getChatById(chatId) } catch (e: Exception) { null }
        
        var kind = when (chatEntity?.type?.lowercase()) {
            "dm", "direct", "one_to_one" -> ChatKind.DM
            "channel", "group" -> ChatKind.CHANNEL
            "legacy" -> ChatKind.LEGACY
            else -> ChatKind.UNKNOWN
        }
        
        var receiverId = receiverHint?.takeIf { isValidUuid(it) } ?: chatEntity?.otherUserId?.takeIf { isValidUuid(it) }
        
        if (kind == ChatKind.DM && !receiverId.isNullOrEmpty() && receiverId != currentUid) {
            return@withContext CanonicalChatIdentity(
                kind = ChatKind.DM,
                chatId = chatId,
                threadId = chatId,
                receiverId = receiverId
            )
        }

        // Attempt remote resolution via one_to_one_threads
        val service = SupabaseClient.apiService
        if (service != null && !currentUid.isNullOrEmpty() && SupabaseClient.isConfigured) {
            try {
                val threadResponse = runCall { auth ->
                    service.getOneToOneThreads(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        orFilter = "(id.eq.$chatId)"
                    )
                }
                if (threadResponse != null && threadResponse.isSuccessful) {
                    val threads = threadResponse.body()
                    if (!threads.isNullOrEmpty()) {
                        val thread = threads[0]
                        val derivedReceiver = if (thread.userA == currentUid) thread.userB else thread.userA
                        if (!derivedReceiver.isNullOrEmpty() && derivedReceiver != currentUid) {
                            try {
                                if (chatEntity != null) {
                                    db.chatDao().insertChat(chatEntity.copy(type = "dm", otherUserId = derivedReceiver))
                                } else {
                                    db.chatDao().insertChat(
                                        com.example.data.database.ChatEntity(
                                            id = chatId,
                                            createdAt = SupabaseClient.getNowIsoString(),
                                            type = "dm",
                                            name = "Chat",
                                            otherUserId = derivedReceiver
                                        )
                                    )
                                }
                            } catch (_: Exception) {}
                            
                            return@withContext CanonicalChatIdentity(
                                kind = ChatKind.DM,
                                chatId = chatId,
                                threadId = thread.id,
                                receiverId = derivedReceiver
                            )
                        }
                    } else {
                        // Confirmed non-existent in one_to_one_threads
                        if (chatEntity?.type == "channel" || chatEntity?.type == "group") {
                            return@withContext CanonicalChatIdentity(kind = ChatKind.CHANNEL, chatId = chatId)
                        }
                        if (chatEntity?.type == "legacy") {
                            return@withContext CanonicalChatIdentity(kind = ChatKind.LEGACY, chatId = chatId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving canonical identity for $chatId", e)
            }
        }

        if (!receiverId.isNullOrEmpty() && receiverId != currentUid) {
            return@withContext CanonicalChatIdentity(
                kind = ChatKind.DM,
                chatId = chatId,
                threadId = chatId,
                receiverId = receiverId
            )
        }
        
        if (chatEntity?.type == "channel" || chatEntity?.type == "group") {
            return@withContext CanonicalChatIdentity(kind = ChatKind.CHANNEL, chatId = chatId)
        }
        if (chatEntity?.type == "legacy") {
            return@withContext CanonicalChatIdentity(kind = ChatKind.LEGACY, chatId = chatId)
        }

        return@withContext CanonicalChatIdentity(kind = ChatKind.UNKNOWN, chatId = chatId)
    }

    private suspend fun <R> runCall(call: suspend (String) -> retrofit2.Response<R>): retrofit2.Response<R>? {
        return com.example.util.Resilience.retry(
            times = 5,
            initialDelay = 1000L,
            maxDelay = 10000L,
            factor = 2.0,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) || (it is retrofit2.HttpException && it.code() == 408) }
        ) {
            SessionManager.validateAndRefreshSessionIfNeeded()
            var token = SupabaseClient.currentToken ?: return@retry null
            var bearer = "Bearer $token"
            
            var response = try {
                call(bearer)
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                throw e
            }
            
            if (response != null && response.code() == 401) {
                Log.i(TAG, "401/JWT expired detected. Triggering refresh session...")
                val refreshed = SessionManager.refreshSession()
                if (refreshed) {
                    val newToken = SupabaseClient.currentToken ?: ""
                    bearer = "Bearer $newToken"
                    response = try {
                        call(bearer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Retry call failed", e)
                        throw e
                    }
                }
            }
            response
        }
    }

    suspend fun getMessagesForChatPaged(
        chatId: String,
        limit: Int = 50,
        oldestTimestamp: String? = null,
        newestTimestamp: String? = null
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        // Pre-warm chat-to-user cache and other user's public key
        try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val chatEntity = db.chatDao().getChatById(chatId)
            val otherUserId = chatEntity?.otherUserId
            if (!otherUserId.isNullOrEmpty()) {
                com.example.util.CryptoManager.chatToOtherUserCache[chatId] = otherUserId
                if (!com.example.util.CryptoManager.publicKeyCache.containsKey(otherUserId)) {
                    UserKeysRepository.getPublicKeyForUser(otherUserId)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error pre-warming E2EE key cache", e)
        }

        val cachedEntities = messageDao.getMessagesForChatPaged(chatId, limit, oldestTimestamp)
        var messagesList = decryptMessages(cachedEntities.map { it.toMessage() }.sortedBy { it.createdAt })

        val lastSync = lastSyncTimestamps[chatId] ?: 0L
        val now = System.currentTimeMillis()
        if (oldestTimestamp == null && newestTimestamp == null && now - lastSync < 45000L) { // 45 seconds smart sync threshold only on initial fetch
            Log.d(TAG, "getMessagesForChatPaged: Chat $chatId was synced recently (${now - lastSync} ms ago). Skipping remote HTTP request and returning local messages directly.")
            return@withContext Result.success(messagesList)
        }

        val createdAtFilterStr = when {
            oldestTimestamp != null -> "lt.$oldestTimestamp"
            newestTimestamp != null -> "gt.$newestTimestamp"
            else -> {
                val newestMsgTimestamp = messageDao.getNewestMessageTimestamp(chatId) ?: messagesList.lastOrNull()?.createdAt
                if (!newestMsgTimestamp.isNullOrEmpty()) "gt.$newestMsgTimestamp" else null
            }
        }

        if (!SupabaseClient.isConfigured) {
            if (oldestTimestamp == null && newestTimestamp == null && messagesList.isEmpty()) {
                val demoList = SupabaseClient.demoMessages.filter { it.chatId == chatId }.sortedBy { it.createdAt }
                val entities = demoList.map { MessageEntity.fromMessage(it) }
                messageDao.insertOrMergeMessages(entities)
                messagesList = decryptMessages(demoList.takeLast(limit))
            }
            return@withContext Result.success(messagesList)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(messagesList)
            
            if (SupabaseClient.currentToken == null) {
                Log.w(TAG, "getMessagesForChatPaged: SupabaseClient.currentToken is null! Fetching may return unauthenticated or fail.")
            }

            var lastClearedAt: String? = null
            val currentUid = SupabaseClient.currentUser?.id
            val userDeletedIds = mutableSetOf<String>()
            userDeletedIds.addAll(userDeletedMessageIds)

            if (currentUid != null) {
                val participantRes = runCall { authHeader ->
                    service.getChatParticipant(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        chatIdFilter = "eq.$chatId",
                        userIdFilter = "eq.$currentUid"
                    )
                }
                if (participantRes?.isSuccessful == true && !participantRes.body().isNullOrEmpty()) {
                    lastClearedAt = participantRes.body()!![0].lastClearedAt
                }

                val localCleared = localClearedAtMap[chatId]
                if (localCleared != null) {
                    if (lastClearedAt == null || isTimestampBeforeOrEqual(lastClearedAt, localCleared)) {
                        lastClearedAt = localCleared
                    }
                }

                // Fetch messages deleted specifically for current user from user_deleted_messages
                val deletedRes = runCall { authHeader ->
                    service.getUserDeletedMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        userIdFilter = "eq.$currentUid"
                    )
                }
                if (deletedRes?.isSuccessful == true) {
                    deletedRes.body()?.forEach { item ->
                        val msgId = item["message_id"] as? String
                        if (!msgId.isNullOrEmpty()) {
                            userDeletedIds.add(msgId)
                            userDeletedMessageIds.add(msgId)
                        }
                    }
                }
            }

            if (userDeletedIds.isNotEmpty()) {
                try {
                    val allLocal = messageDao.getMessagesForChat(chatId)
                    val deletedLocal = allLocal.filter { userDeletedIds.contains(it.id) }
                    deletedLocal.forEach { messageDao.deleteMessageById(it.id) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error purging user deleted local messages", e)
                }
            }

            val identity = resolveChatIdentity(chatId)
            val isDm = identity.kind == ChatKind.DM

            val response = if (isDm) {
                runCall { authHeader ->
                    Log.d(TAG, "getMessagesForChatPaged: Querying thread_messages for DM $chatId with createdAtFilter=$createdAtFilterStr")
                    service.getThreadMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        threadIdFilter = "eq.${identity.threadId ?: chatId}",
                        createdAtFilter = createdAtFilterStr,
                        order = "created_at.desc",
                        limit = limit
                    )
                }
            } else {
                null
            }

            if (response != null) {
                if (response.isSuccessful) {
                    lastSyncTimestamps[chatId] = System.currentTimeMillis()
                    val remoteList = response.body() ?: emptyList()
                    Log.i(TAG, "getMessagesForChatPaged (thread_messages) SUCCESS: code=${response.code()}, size=${remoteList.size} filas")
                    if (remoteList.isNotEmpty()) {
                        val decryptedList = remoteList.mapNotNull { item ->
                            val msg = (item as? com.example.data.model.ThreadMessage)?.toMessage()
                            msg?.let { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }
                        }.filter { msg ->
                            com.example.util.MessageFilter.shouldKeepMessage(
                                messageId = msg.id,
                                messageClientUuid = msg.clientMessageUuid,
                                messageCreatedAt = msg.createdAt,
                                lastClearedAt = lastClearedAt,
                                deletedMessageIds = userDeletedIds
                            )
                        }
                        val entities = decryptedList.map { MessageEntity.fromMessage(it) }
                        if (entities.isNotEmpty()) {
                            messageDao.insertOrMergeMessages(entities)
                        }
                    }
                    if (!lastClearedAt.isNullOrEmpty()) {
                        try {
                            val allLocal = messageDao.getMessagesForChat(chatId)
                            val staleLocal = allLocal.filter { isTimestampBeforeOrEqual(it.createdAt, lastClearedAt) }
                            staleLocal.forEach { messageDao.deleteMessageById(it.id) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error purging stale local messages", e)
                        }
                    }
                    
                    val freshLocal = messageDao.getMessagesForChatPaged(chatId, limit, oldestTimestamp)
                    val validLocal = freshLocal.filter { local ->
                        com.example.util.MessageFilter.shouldKeepMessage(
                            messageId = local.id,
                            messageClientUuid = local.clientMessageUuid,
                            messageCreatedAt = local.createdAt,
                            lastClearedAt = lastClearedAt,
                            deletedMessageIds = userDeletedIds
                        )
                    }
                    return@withContext Result.success(decryptMessages(validLocal.map { it.toMessage() }.sortedBy { it.createdAt }))
                } else {
                    val errBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "🚨 getMessagesForChatPaged (thread_messages) FAILED: code=${response.code()}, error=$errBody")
                    // DM strictly returns local cache and NEVER falls through to messages table
                    if (isDm) return@withContext Result.success(messagesList)
                }
            }

            // If CHANNEL or LEGACY
            if (identity.kind == ChatKind.CHANNEL || identity.kind == ChatKind.LEGACY) {
                val legacyResponse = runCall { authHeader ->
                    Log.d(TAG, "getMessagesForChatPaged: Querying legacy messages for Channel $chatId with createdAtFilter=$createdAtFilterStr")
                    service.getMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        chatIdFilter = "eq.$chatId",
                        createdAtFilter = createdAtFilterStr,
                        order = "created_at.desc",
                        limit = limit
                    )
                }

                if (legacyResponse != null && legacyResponse.isSuccessful) {
                    lastSyncTimestamps[chatId] = System.currentTimeMillis()
                    val remoteList = legacyResponse.body() ?: emptyList()
                    Log.i(TAG, "getMessagesForChatPaged (legacy messages) SUCCESS: code=${legacyResponse.code()}, size=${remoteList.size} filas")
                    if (remoteList.isNotEmpty()) {
                        val decryptedList = remoteList.map { it: Message -> com.example.util.CryptoManager.decryptMessageIfNeeded(it) }.filter { msg ->
                            com.example.util.MessageFilter.shouldKeepMessage(
                                messageId = msg.id,
                                messageClientUuid = msg.clientMessageUuid,
                                messageCreatedAt = msg.createdAt,
                                lastClearedAt = lastClearedAt,
                                deletedMessageIds = userDeletedIds
                            )
                        }
                        val entities = decryptedList.map { MessageEntity.fromMessage(it) }
                        if (entities.isNotEmpty()) {
                            messageDao.insertOrMergeMessages(entities)
                        }
                    }
                    if (!lastClearedAt.isNullOrEmpty()) {
                        try {
                            val allLocal = messageDao.getMessagesForChat(chatId)
                            val staleLocal = allLocal.filter { isTimestampBeforeOrEqual(it.createdAt, lastClearedAt) }
                            staleLocal.forEach { messageDao.deleteMessageById(it.id) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error purging stale local messages", e)
                        }
                    }
                    val freshLocal = messageDao.getMessagesForChatPaged(chatId, limit, oldestTimestamp)
                    val validLocal = freshLocal.filter { local ->
                        com.example.util.MessageFilter.shouldKeepMessage(
                            messageId = local.id,
                            messageClientUuid = local.clientMessageUuid,
                            messageCreatedAt = local.createdAt,
                            lastClearedAt = lastClearedAt,
                            deletedMessageIds = userDeletedIds
                        )
                    }
                    return@withContext Result.success(decryptMessages(validLocal.map { it.toMessage() }.sortedBy { it.createdAt }))
                } else {
                    val errBody = legacyResponse?.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "getMessagesForChatPaged (legacy messages) FAILED: code=${legacyResponse?.code()}, error=$errBody")
                }
            }

            Result.success(messagesList)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesForChatPaged exception, loading cache", e)
            Result.success(messagesList)
        }
    }

    suspend fun getMessagesForChat(chatId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        getMessagesForChatPaged(chatId, 100, null, null)
    }

    suspend fun syncUpdatedMessages(chatId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        val context = com.example.PanaApplication.instance
        val prefs = context.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
        val prefKey = "last_sync_updated_at_$chatId"
        
        var lastUpdatedAt = prefs.getString(prefKey, null)
        if (lastUpdatedAt.isNullOrEmpty()) {
            val allLocal = messageDao.getMessagesForChat(chatId)
            lastUpdatedAt = allLocal.mapNotNull { it.updatedAt }.maxOrNull()
        }
        
        val timestamp = if (!lastUpdatedAt.isNullOrEmpty()) lastUpdatedAt else "1970-01-01T00:00:00Z"
        Log.d(TAG, "syncUpdatedMessages: Starting incremental sync for chat $chatId since $timestamp")
        
        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(emptyList())
        }
        
        val service = SupabaseClient.apiService ?: return@withContext Result.success(emptyList())
        
        try {
            var lastClearedAt: String? = null
            val currentUid = SupabaseClient.currentUser?.id
            val userDeletedIds = mutableSetOf<String>()
            userDeletedIds.addAll(userDeletedMessageIds)

            if (currentUid != null) {
                val participantRes = runCall { authHeader ->
                    service.getChatParticipant(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        chatIdFilter = "eq.$chatId",
                        userIdFilter = "eq.$currentUid"
                    )
                }
                if (participantRes?.isSuccessful == true && !participantRes.body().isNullOrEmpty()) {
                    lastClearedAt = participantRes.body()!![0].lastClearedAt
                }

                val localCleared = localClearedAtMap[chatId]
                if (localCleared != null) {
                    if (lastClearedAt == null || isTimestampBeforeOrEqual(lastClearedAt, localCleared)) {
                        lastClearedAt = localCleared
                    }
                }

                val deletedRes = runCall { authHeader ->
                    service.getUserDeletedMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        userIdFilter = "eq.$currentUid"
                    )
                }
                if (deletedRes?.isSuccessful == true) {
                    deletedRes.body()?.forEach { item ->
                        val msgId = item["message_id"] as? String
                        if (!msgId.isNullOrEmpty()) {
                            userDeletedIds.add(msgId)
                            userDeletedMessageIds.add(msgId)
                        }
                    }
                }
            }

            var remoteMessages = emptyList<Message>()
            
            val identity = resolveChatIdentity(chatId)
            val isDm = identity.kind == ChatKind.DM

            if (isDm) {
                val response = runCall { authHeader ->
                    Log.d(TAG, "syncUpdatedMessages: Querying thread_messages for DM $chatId with updatedAtFilter=gt.$timestamp")
                    service.getIncrementalThreadMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        threadIdFilter = "eq.${identity.threadId ?: chatId}",
                        updatedAtFilter = "gt.$timestamp"
                    )
                }

                if (response != null && response.isSuccessful) {
                    val remoteList = response.body() ?: emptyList()
                    Log.i(TAG, "syncUpdatedMessages (thread_messages) SUCCESS: code=${response.code()}, size=${remoteList.size} filas")
                    remoteMessages = remoteList.mapNotNull { it.toMessage() }
                } else {
                    val errBody = response?.errorBody()?.string() ?: "No response or error body"
                    Log.e(TAG, "🚨 syncUpdatedMessages (thread_messages) FAILED: error=$errBody")
                    return@withContext Result.failure(Exception("DM sync failed: $errBody"))
                }
            } else if (identity.kind == ChatKind.CHANNEL || identity.kind == ChatKind.LEGACY) {
                val legacyResponse = runCall { authHeader ->
                    Log.d(TAG, "syncUpdatedMessages: Querying legacy messages for Channel $chatId with updatedAtFilter=gt.$timestamp")
                    service.getIncrementalMessages(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = authHeader,
                        chatIdFilter = "eq.$chatId",
                        updatedAtFilter = "gt.$timestamp"
                    )
                }

                if (legacyResponse != null && legacyResponse.isSuccessful) {
                    val remoteList = legacyResponse.body() ?: emptyList()
                    Log.i(TAG, "syncUpdatedMessages (legacy messages) SUCCESS: code=${legacyResponse.code()}, size=${remoteList.size} filas")
                    remoteMessages = remoteList
                } else {
                    val errBody = legacyResponse?.errorBody()?.string() ?: "No legacy response or error body"
                    Log.e(TAG, "🚨 syncUpdatedMessages (legacy messages) FAILED: error=$errBody")
                }
            } else {
                Log.w(TAG, "syncUpdatedMessages: Chat $chatId identity is UNKNOWN. Skipping remote incremental query.")
                return@withContext Result.success(emptyList())
            }

            val processedMessages = remoteMessages.map { msg ->
                com.example.util.CryptoManager.decryptMessageIfNeeded(msg)
            }.filter { msg ->
                com.example.util.MessageFilter.shouldKeepMessage(
                    messageId = msg.id,
                    messageClientUuid = msg.clientMessageUuid,
                    messageCreatedAt = msg.createdAt,
                    lastClearedAt = lastClearedAt,
                    deletedMessageIds = userDeletedIds
                )
            }

            if (processedMessages.isNotEmpty()) {
                val entities = processedMessages.map { MessageEntity.fromMessage(it) }
                entities.forEach { entity ->
                    messageDao.mergeAndSaveMessage(entity)
                }

                val newestUpdatedAt = processedMessages.mapNotNull { it.updatedAt }.maxOrNull()
                if (!newestUpdatedAt.isNullOrEmpty()) {
                    prefs.edit().putString(prefKey, newestUpdatedAt).apply()
                    Log.d(TAG, "syncUpdatedMessages: Updated last_sync_updated_at cursor for chat $chatId to $newestUpdatedAt")
                }
            }

            return@withContext Result.success(processedMessages)

        } catch (e: Exception) {
            Log.e(TAG, "syncUpdatedMessages exception occurred", e)
            return@withContext Result.failure(e)
        }
    }

suspend fun insertLocalMessage(msg: Message) = withContext(Dispatchers.IO) {
        val effectiveClearedAt = getEffectiveClearedAt(msg.chatId, null)
        val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
            messageId = msg.id,
            messageClientUuid = msg.clientMessageUuid,
            messageCreatedAt = msg.createdAt,
            lastClearedAt = effectiveClearedAt,
            deletedMessageIds = getUserDeletedMessageIds()
        )
        if (shouldKeep) {
            messageDao.insertMessage(MessageEntity.fromMessage(msg))
        } else {
            Log.d(TAG, "insertLocalMessage: Message filtered out by MessageFilter")
        }
    }

    suspend fun updateLocalMessageStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        messageDao.updateMessageStatus(id, status)
    }

    fun getMessagesFlow(chatId: String): kotlinx.coroutines.flow.Flow<List<Message>> {
        return messageDao.getMessagesForChatFlow(chatId).map { entities: List<MessageEntity> ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun getCachedMessages(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        val entities = messageDao.getMessagesForChat(chatId)
        decryptMessages(entities.map { it.toMessage() }.sortedBy { it.createdAt })
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncMessagesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .addTag("sync_messages_work")
            .build()

        WorkManager.getInstance(PanaApplication.instance)
            .enqueueUniqueWork(
                "sync_messages_unique",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        Log.i(TAG, "Scheduled background sync with WorkManager")
    }

    fun scheduleMediaUpload(messageId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("messageId", messageId)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .addTag("media_upload_work")
            .build()

        WorkManager.getInstance(PanaApplication.instance)
            .enqueueUniqueWork(
                "upload_$messageId",
                ExistingWorkPolicy.KEEP,
                uploadRequest
            )
        Log.i(TAG, "Scheduled background media upload for message $messageId")
    }

    suspend fun sendMultimediaMessage(
        chatId: String,
        context: android.content.Context,
        sourceUri: android.net.Uri? = null,
        sourceFile: java.io.File? = null,
        mimeType: String,
        typeLabel: String,
        content: String = "",
        replyToId: String? = null,
        isGhost: Boolean = false,
        receiverId: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: "me_demo_id"
        val nowStr = SupabaseClient.getNowIsoString()
        val tempId = "temp_" + java.util.UUID.randomUUID().toString()
        val clientUuid = java.util.UUID.randomUUID().toString()

        try {
            // 1. Save media to local storage (persistent)
            val localMediaUri = PanalinkMediaManager.saveMediaToLocal(
                context,
                sourceUri,
                sourceFile,
                "msg_${tempId}_orig"
            )
            
            if (localMediaUri == null) {
                return@withContext Result.failure(Exception("Failed to save media locally"))
            }
            
            val localFile = java.io.File(localMediaUri)
            
            // 2. Generate local thumbnail for immediate UI feedback
            val thumbFile = if (typeLabel.equals("Video", ignoreCase = true)) {
                PanalinkMediaManager.generateVideoThumbnail(context, localFile)
            } else if (typeLabel.equals("Image", ignoreCase = true)) {
                PanalinkMediaManager.generateImageThumbnail(localFile)
            } else null

            val localThumbUri = thumbFile?.absolutePath

            val formattedContent = if (isGhost && !content.startsWith("[Ghost]")) {
                "[Ghost] $content"
            } else {
                content
            }

            val identity = resolveChatIdentity(chatId, receiverId)
            val resolvedReceiver = identity.receiverId ?: receiverId

            // 3. Create and insert MessageEntity with local paths and "sending" status
            val entity = MessageEntity(
                id = tempId,
                chatId = chatId,
                senderId = currentUid,
                receiverId = resolvedReceiver,
                content = formattedContent,
                createdAt = nowStr,
                status = "sending",
                replyToMessageId = replyToId,
                clientMessageUuid = clientUuid,
                messageType = typeLabel.lowercase(),
                mediaMime = mimeType,
                localMediaUri = localMediaUri,
                localThumbnailUri = localThumbUri,
                thumbnailUrl = localThumbUri,
                isGhost = isGhost
            )
            
            val effectiveClearedAt = getEffectiveClearedAt(chatId, null)
            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                messageId = entity.id,
                messageClientUuid = entity.clientMessageUuid,
                messageCreatedAt = entity.createdAt,
                lastClearedAt = effectiveClearedAt,
                deletedMessageIds = getUserDeletedMessageIds()
            )
            if (shouldKeep) {
                messageDao.insertMessage(entity)
                Log.i(TAG, "Inserted pending multimedia message: $tempId")
            } else {
                Log.w(TAG, "Multimedia message creation filtered out by MessageFilter")
            }

            // 4. Enqueue WorkManager job instead of uploading immediately
            scheduleMediaUpload(tempId)

            val msg = entity.toMessage()
            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending multimedia message", e)
            Result.failure(e)
        }
    }

    suspend fun sendPlaylistShareMessage(
        chatId: String,
        playlistId: String,
        playlistName: String,
        coverUrl: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: "me_demo_id"
        val nowStr = SupabaseClient.getNowIsoString()
        val tempId = "temp_" + java.util.UUID.randomUUID().toString()
        val clientUuid = java.util.UUID.randomUUID().toString()

        val entity = MessageEntity(
            id = tempId,
            chatId = chatId,
            senderId = currentUid,
            content = "Shared a playlist: $playlistName",
            createdAt = nowStr,
            status = "sending",
            clientMessageUuid = clientUuid,
            messageType = "playlist_share",
            mediaUrl = coverUrl,
            musicPlaylistId = playlistId
        )

        try {
            val effectiveClearedAt = getEffectiveClearedAt(chatId, null)
            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                messageId = entity.id,
                messageClientUuid = entity.clientMessageUuid,
                messageCreatedAt = entity.createdAt,
                lastClearedAt = effectiveClearedAt,
                deletedMessageIds = getUserDeletedMessageIds()
            )
            if (shouldKeep) {
                messageDao.insertMessage(entity)
            }
            
            scheduleSync()
            Result.success(entity.toMessage())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending playlist share message", e)
            Result.failure(e)
        }
    }

    suspend fun syncPendingMessages(): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext true
        val pending = messageDao.getPendingMessages()
        try {
            val service = SupabaseClient.apiService
            if (service != null) {
                runCall { auth ->
                    service.createDebugLog(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        logMap = mapOf(
                            "fecha" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                            "etapa" to "START_SYNC_PENDING",
                            "response_body" to "pending_count=${pending.size}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging START_SYNC_PENDING", e)
        }
        if (pending.isEmpty()) return@withContext true
        Log.i(TAG, "Syncing ${pending.size} pending offline messages...")

                val parseMessage = { jsonStr: String? ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.Message::class.java)
                    val adapter = SupabaseClient.moshi.adapter<List<com.example.data.model.Message>>(listType)
                    val msgs = adapter.fromJson(jsonStr)
                    msgs?.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
        }

        var allSuccessful = true
        for (entity in pending) {
            try {
                if (entity.messageType != null && entity.messageType != "text" && entity.mediaUrl.isNullOrEmpty()) {
                    Log.w(TAG, "Skipping sync for multimedia message ${entity.id} because mediaUrl is missing (upload incomplete/failed)")
                    if (entity.localMediaUri != null) scheduleMediaUpload(entity.id)
                    continue
                }
                
                val service = SupabaseClient.apiService ?: run { allSuccessful = false; return@withContext false }
                val currentUid = SupabaseClient.currentUser?.id ?: run { allSuccessful = false; return@withContext false }

                // Resolve receiver and determine canonical identity
                val identity = resolveChatIdentity(entity.chatId, entity.receiverId)
                if (identity.kind == ChatKind.UNKNOWN) {
                    Log.w(TAG, "Chat ${entity.chatId} identity UNKNOWN during pending sync. Staying pending.")
                    allSuccessful = false
                    continue
                }

                val isDmSync = identity.kind == ChatKind.DM
                val receiverUid: String? = identity.receiverId ?: entity.receiverId

                if (isDmSync && !receiverUid.isNullOrEmpty() && entity.receiverId.isNullOrEmpty()) {
                    try {
                        messageDao.updateMessageReceiverId(entity.id, receiverUid)
                    } catch (_: Exception) {}
                }

                // VALIDACIÓN PARA DMs
                if (isDmSync) {
                    if (receiverUid.isNullOrEmpty() || !isValidUuid(receiverUid)) {
                        Log.e(TAG, "SYNC_FAILED: receiver_id is missing or invalid for DM ${entity.id}. Skipping to prevent RLS/Constraint violation.")
                        allSuccessful = false
                        continue
                    }
                    
                    if (receiverUid == currentUid) {
                        Log.e(TAG, "SYNC_FAILED: receiver_id cannot be the same as sender_id for DM ${entity.id}")
                        allSuccessful = false
                        continue
                    }
                }

                // Retrieve receiver's E2EE public key (Only for DMs)
                val receiverPublicKey = if (isDmSync && !receiverUid.isNullOrEmpty()) {
                    com.example.data.repository.UserKeysRepository.getPublicKeyForUser(receiverUid!!)
                } else null

                val contentToUpload = if (isDmSync && !receiverPublicKey.isNullOrEmpty() && !entity.content.isNullOrEmpty()) {
                    Log.d(TAG, "Encrypting pending message content using E2EE during sync")
                    com.example.util.CryptoManager.encrypt(entity.content!!, receiverPublicKey)
                } else {
                    entity.content
                }

                val remoteId = if (isValidUuid(entity.id)) entity.id else java.util.UUID.randomUUID().toString()
                val normalizedMessageType = when {
                    entity.messageType == "voice" || entity.messageType == "voice_note" || entity.messageType?.startsWith("audio/") == true -> "audio"
                    entity.messageType in listOf("image", "video", "audio", "document", "sticker", "gif", "text", "location", "call") -> entity.messageType
                    entity.messageType?.startsWith("image/") == true -> if (entity.messageType == "image/gif") "gif" else if (entity.messageType == "image/webp") "sticker" else "image"
                    entity.messageType?.startsWith("video/") == true -> "video"
                    entity.messageType?.startsWith("application/") == true || entity.messageType?.startsWith("text/") == true -> "document"
                    else -> entity.messageType?.lowercase() ?: "text"
                }

                val msgMap = mutableMapOf<String, Any?>(
                    "id" to remoteId,
                    "sender_id" to currentUid,
                    "reply_to" to entity.replyToMessageId?.takeIf { isValidUuid(it) },
                    "text_content" to contentToUpload,
                    "client_message_uuid" to entity.clientMessageUuid,
                    "created_at" to entity.createdAt,
                    "media_url" to entity.mediaUrl,
                    "thumbnail_url" to entity.thumbnailUrl,
                    "media_mime" to entity.mediaMime,
                    "message_type" to normalizedMessageType,
                    "file_size" to entity.mediaSize,
                    "duration" to entity.mediaDuration,
                    "width" to entity.mediaWidth,
                    "height" to entity.mediaHeight,
                    "music_playlist_id" to entity.musicPlaylistId
                )

                if (isDmSync) {
                    msgMap["thread_id"] = entity.chatId
                    msgMap["receiver_id"] = receiverUid
                } else {
                    msgMap["chat_id"] = entity.chatId
                }
                if (entity.isGhost || entity.content?.startsWith("[Ghost]") == true) {
                    msgMap["is_ghost"] = true
                }
                val cleanMsgMap = msgMap.filterValues { it != null }

                Log.i(TAG, "PANALINK_SYNC: chatId=${entity.chatId}, message_type=${entity.messageType}, media_url=${entity.mediaUrl}")

                // Pre-POST Reconciliation Check (Rule 1)
                var wasReconciled = false
                if (!entity.clientMessageUuid.isNullOrBlank()) {
                    try {
                        val verifyResponse = runCall { auth ->
                            service.getThreadMessageByClientUuid(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = auth,
                                clientUuidFilter = "eq.${entity.clientMessageUuid}"
                            )
                        }
                        if (verifyResponse?.isSuccessful == true && verifyResponse.body()?.isNotEmpty() == true) {
                            val serverMsgList = verifyResponse.body()!!
                            val threadMsg = serverMsgList.first()
                            val mappedMsg = threadMsg.toMessage()
                            val finalMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(mappedMsg).copy(
                                status = "sent",
                                clientMessageUuid = entity.clientMessageUuid
                            )
                            val effectiveClearedAt = getEffectiveClearedAt(finalMsg.chatId, null)
                            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                                messageId = finalMsg.id,
                                messageClientUuid = finalMsg.clientMessageUuid,
                                messageCreatedAt = finalMsg.createdAt,
                                lastClearedAt = effectiveClearedAt,
                                deletedMessageIds = getUserDeletedMessageIds()
                            )
                            if (shouldKeep) {
                                if (entity.id.startsWith("temp_") && finalMsg.id != entity.id) {
                                    messageDao.replaceTemporaryMessage(entity.id, MessageEntity.fromMessage(finalMsg))
                                } else {
                                    messageDao.insertMessage(MessageEntity.fromMessage(finalMsg))
                                }
                            } else {
                                messageDao.deleteMessageById(entity.id)
                            }
                            Log.i(TAG, "TRACE_SYNC: Reconciled message ${entity.clientMessageUuid} before POST. Marked as SENT.")
                            wasReconciled = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking reconciliation before POST for msg ${entity.clientMessageUuid}", e)
                    }
                }

                if (wasReconciled) {
                    continue
                }

                var successful = false
                var is409OrTimeout = false
                var threadResponse: retrofit2.Response<okhttp3.ResponseBody>? = null
                
                try {
                    threadResponse = runCall { auth ->
                        if (isDmSync) {
                            service.createThreadMessage(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = auth,
                                message = cleanMsgMap
                            )
                        } else {
                            service.createMessage(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = auth,
                                message = cleanMsgMap
                            )
                        }
                    }
                    if (threadResponse?.code() == 409 || threadResponse?.code() == 408 || (threadResponse?.code() ?: 0) >= 500) {
                        is409OrTimeout = true
                    }
                } catch (e: Exception) {
                    val isTransient = e is java.io.IOException || 
                                      e is java.net.SocketTimeoutException || 
                                      e is java.net.ConnectException || 
                                      (e as? retrofit2.HttpException)?.code() in listOf(408, 409) || 
                                      ((e as? retrofit2.HttpException)?.code() ?: 0) >= 500
                    if (isTransient) {
                        is409OrTimeout = true
                    } else {
                        throw e
                    }
                }
                
                var code = threadResponse?.code()
                var isSuccessful = threadResponse?.isSuccessful == true
                var errBody = threadResponse?.errorBody()?.string()
                var respBody = if (isSuccessful) threadResponse?.body()?.string() else null
                
                if (is409OrTimeout && !entity.clientMessageUuid.isNullOrBlank()) {
                    Log.i(TAG, "TRACE_SYNC: 409, timeout or transient error detected for msg ${entity.clientMessageUuid}. Reconciling...")
                    try {
                        val verifyResponse = runCall { auth ->
                            service.getThreadMessageByClientUuid(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = auth,
                                clientUuidFilter = "eq.${entity.clientMessageUuid}"
                            )
                        }
                        if (verifyResponse?.isSuccessful == true && verifyResponse.body()?.isNotEmpty() == true) {
                            val serverMsgList = verifyResponse.body()!!
                            val threadMsg = serverMsgList.first()
                            val mappedMsg = threadMsg.toMessage()
                            val finalMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(mappedMsg).copy(
                                status = "sent",
                                clientMessageUuid = entity.clientMessageUuid
                            )
                            val effectiveClearedAt = getEffectiveClearedAt(finalMsg.chatId, null)
                            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                                messageId = finalMsg.id,
                                messageClientUuid = finalMsg.clientMessageUuid,
                                messageCreatedAt = finalMsg.createdAt,
                                lastClearedAt = effectiveClearedAt,
                                deletedMessageIds = getUserDeletedMessageIds()
                            )
                            if (shouldKeep) {
                                if (entity.id.startsWith("temp_") && finalMsg.id != entity.id) {
                                    messageDao.replaceTemporaryMessage(entity.id, MessageEntity.fromMessage(finalMsg))
                                } else {
                                    messageDao.insertMessage(MessageEntity.fromMessage(finalMsg))
                                }
                            } else {
                                messageDao.deleteMessageById(entity.id)
                            }
                            Log.i(TAG, "TRACE_SYNC: Reconciled message ${entity.clientMessageUuid} after failed POST. Marked as SENT.")
                            continue
                        } else {
                            Log.w(TAG, "TRACE_SYNC: Message ${entity.clientMessageUuid} not found on remote after error/timeout. Keeping as pending/sending.")
                            allSuccessful = false
                            continue
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reconciling message after failed POST", e)
                        allSuccessful = false
                        continue
                    }
                }
                   
                Log.i(TAG, "TRACE_SYNC_AFTER_CREATE_THREAD: httpStatus=$code, isSuccessful=$isSuccessful, supabaseResponse=$respBody, errorBody=$errBody")

                try {
                    runCall { auth ->
                        val afterMap = mapOf<String, Any?>(
                            "fecha" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                            "message_id" to entity.id,
                            "message_type" to entity.messageType,
                            "media_url" to entity.mediaUrl,
                            "media_mime" to entity.mediaMime,
                            "media_size" to entity.mediaSize,
                            "media_width" to entity.mediaWidth,
                            "media_height" to entity.mediaHeight,
                            "client_message_uuid" to entity.clientMessageUuid,
                            "http_status" to code,
                            "is_successful" to isSuccessful,
                            "response_body" to respBody,
                            "error_body" to errBody,
                            "etapa" to "AFTER_CREATE_THREAD"
                        )
                        service.createDebugLog(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            logMap = afterMap
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error logging AFTER_CREATE_THREAD debug log", e)
                }

                if (successful || threadResponse?.isSuccessful == true) {
                    successful = true
                    val responseBody = respBody ?: threadResponse?.body()?.string()
                    Log.i(TAG, "PANALINK_SYNC_RESULT: $responseBody, markedAsSent=true")
                } else {
                    val code = threadResponse?.code()
                    val errorBody = threadResponse?.errorBody()?.string() ?: ""
                    Log.w(TAG, "PANALINK_SYNC_RESULT: thread_messages failed (Code: $code, Error: $errorBody).")
                    
                    // IF IT IS A DM, WE DO NOT FALLBACK TO LEGACY MESSAGES
                    if (isDmSync) {
                        Log.e(TAG, "DM sync failed for message ${entity.id}. Staying pending.")
                        allSuccessful = false
                        continue
                    }

                    Log.i(TAG, "Channel/Legacy chat detected. Trying legacy fallback for message ${entity.id}.")
                    val legacyMsgMap = mutableMapOf<String, Any?>(
                        "id" to remoteId,
                        "thread_id" to entity.chatId,
                        "sender_id" to currentUid,
                        "receiver_id" to receiverUid,
                        "text_content" to contentToUpload,
                        "client_message_uuid" to entity.clientMessageUuid,
                        "created_at" to entity.createdAt,
                        "media_url" to entity.mediaUrl,
                        "thumbnail_url" to entity.thumbnailUrl,
                        "media_mime" to entity.mediaMime,
                        "message_type" to normalizedMessageType,
                        "file_size" to entity.mediaSize,
                        "duration" to entity.mediaDuration,
                        "width" to entity.mediaWidth,
                        "height" to entity.mediaHeight,
                        "music_playlist_id" to entity.musicPlaylistId
                    )
                    if (entity.replyToMessageId != null && isValidUuid(entity.replyToMessageId)) {
                        legacyMsgMap["reply_to"] = entity.replyToMessageId
                    }
                    val cleanLegacyMsgMap = legacyMsgMap.filterValues { it != null }

                    val legacyResponse = runCall { auth ->
                        service.createMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            message = cleanLegacyMsgMap
                        )
                    }
                    if (legacyResponse != null && legacyResponse.isSuccessful) {
                        successful = true
                    } else {
                        allSuccessful = false
                    }
                }

                if (successful) {
                    val serverMsg = parseMessage(respBody)
                    if (serverMsg != null) {
                        val finalMsgRaw = serverMsg
                        val finalMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(finalMsgRaw).copy(
                            status = "sent",
                            clientMessageUuid = entity.clientMessageUuid
                        )
                        val effectiveClearedAt = getEffectiveClearedAt(finalMsg.chatId, null)
                        val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                            messageId = finalMsg.id,
                            messageClientUuid = finalMsg.clientMessageUuid,
                            messageCreatedAt = finalMsg.createdAt,
                            lastClearedAt = effectiveClearedAt,
                            deletedMessageIds = getUserDeletedMessageIds()
                        )
                        if (shouldKeep) {
                            if (entity.id.startsWith("temp_") && finalMsg.id != entity.id) {
                                messageDao.replaceTemporaryMessage(entity.id, MessageEntity.fromMessage(finalMsg))
                            } else {
                                messageDao.insertMessage(MessageEntity.fromMessage(finalMsg))
                            }
                        } else {
                            messageDao.deleteMessageById(entity.id)
                        }
                    } else {
                        messageDao.updateMessageStatus(entity.id, "sent")
                    }
                    Log.d(TAG, "Successfully synchronized message: ${entity.id}")
                    if (!receiverUid.isNullOrEmpty()) {
                        triggerSendPushNotification(
                            chatId = entity.chatId,
                            recipientUserId = receiverUid,
                            title = SupabaseClient.currentProfile?.displayName ?: "Mensaje nuevo",
                            bodyText = entity.content ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync pending message: ${entity.id}", e)
                allSuccessful = false
            }
        }
        return@withContext allSuccessful
    }

    suspend fun syncAllPendingAndUpdatedMessages(): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext true

        Log.i(TAG, "syncAllPendingAndUpdatedMessages: Starting full hybrid sync cycle...")
        var allSuccessful = true

        try {
            // A) Upload de mensajes locales pendientes (status = sending / failed)
            val pendingMsgs = messageDao.getPendingMessages()
            val pendingSuccess = syncPendingMessages()
            if (!pendingSuccess) {
                allSuccessful = false
                Log.w(TAG, "syncAllPendingAndUpdatedMessages: syncPendingMessages failed or partially failed")
            }

            val service = SupabaseClient.apiService
            if (service == null) {
                return@withContext false
            }

            // B) Upload de ediciones: editPending = true
            val editPendingMsgs = messageDao.getEditPendingMessages()
            Log.d(TAG, "syncAllPendingAndUpdatedMessages: Found ${editPendingMsgs.size} messages with editPending = true")
            for (msg in editPendingMsgs) {
                try {
                    val response = runCall { auth ->
                        service.updateThreadMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            idFilter = "eq.${msg.id}",
                            updates = mapOf(
                                "text_content" to msg.content
                            )
                        )
                    }
                    if (response != null && response.isSuccessful) {
                        messageDao.clearMessageEditPending(msg.id)
                        Log.d(TAG, "syncAllPendingAndUpdatedMessages: Cleared editPending for msg ${msg.id}")
                    } else {
                        allSuccessful = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending edit for msg ${msg.id}", e)
                    allSuccessful = false
                }
            }

            // C) Upload de reacciones: reactionPending = true
            val reactionPendingMsgs = messageDao.getReactionPendingMessages()
            Log.d(TAG, "syncAllPendingAndUpdatedMessages: Found ${reactionPendingMsgs.size} messages with reactionPending = true")
            for (msg in reactionPendingMsgs) {
                try {
                    val currentUid = SupabaseClient.currentUser?.id
                    if (currentUid != null) {
                        val reaction = db.reactionDao().getReaction(msg.id, currentUid)
                        if (reaction != null) {
                            val body = mapOf(
                                "thread_message_id" to msg.id,
                                "user_id" to currentUid,
                                "emoji" to reaction.emoji
                            )
                            val response = runCall { authHeader ->
                                service.upsertMessageReaction(
                                    apiKey = SupabaseClient.supabaseAnonKey,
                                    authorization = authHeader,
                                    prefer = "resolution=merge-duplicates",
                                    reaction = body
                                )
                            }
                            if (response != null && response.isSuccessful) {
                                messageDao.clearMessageReactionPending(msg.id)
                                Log.d(TAG, "syncAllPendingAndUpdatedMessages: Cleared reactionPending (upsert) for msg ${msg.id}")
                            } else {
                                allSuccessful = false
                            }
                        } else {
                            val response = runCall { authHeader ->
                                service.deleteMessageReaction(
                                    apiKey = SupabaseClient.supabaseAnonKey,
                                    authorization = authHeader,
                                    threadMessageIdFilter = "eq.${msg.id}",
                                    userIdFilter = "eq.$currentUid"
                                )
                            }
                            if (response != null && response.isSuccessful) {
                                messageDao.clearMessageReactionPending(msg.id)
                                Log.d(TAG, "syncAllPendingAndUpdatedMessages: Cleared reactionPending (delete) for msg ${msg.id}")
                            } else {
                                allSuccessful = false
                            }
                        }
                    } else {
                        messageDao.clearMessageReactionPending(msg.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending reaction for msg ${msg.id}", e)
                    allSuccessful = false
                }
            }

            // D) Upload de eliminaciones: deletePending = true
            val deletePendingMsgs = messageDao.getDeletePendingMessages()
            Log.d(TAG, "syncAllPendingAndUpdatedMessages: Found ${deletePendingMsgs.size} messages with deletePending = true")
            for (msg in deletePendingMsgs) {
                try {
                    val nowStr = msg.deletedAt ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { 
                        timeZone = java.util.TimeZone.getTimeZone("UTC") 
                    }.format(java.util.Date())
                    
                    val response = runCall { auth ->
                        service.updateThreadMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            idFilter = "eq.${msg.id}",
                            updates = mapOf(
                                "status" to "deleted",
                                "deleted_at" to nowStr
                            )
                        )
                    }
                    if (response != null && response.isSuccessful) {
                        messageDao.clearMessageDeletePending(msg.id)
                        Log.d(TAG, "syncAllPendingAndUpdatedMessages: Cleared deletePending for msg ${msg.id}")
                    } else {
                        allSuccessful = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending delete for msg ${msg.id}", e)
                    allSuccessful = false
                }
            }

            // Gather all chat IDs to sync delta updates for
            val activeChatIds = mutableSetOf<String>()
            activeChatIds.addAll(pendingMsgs.map { it.chatId })
            activeChatIds.addAll(editPendingMsgs.map { it.chatId })
            activeChatIds.addAll(reactionPendingMsgs.map { it.chatId })
            activeChatIds.addAll(deletePendingMsgs.map { it.chatId })
            
            try {
                activeChatIds.addAll(messageDao.getDistinctChatIds())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching distinct chatIds from local database", e)
            }

            val finalChatIds = activeChatIds.filter { it.isNotEmpty() }
            Log.d(TAG, "syncAllPendingAndUpdatedMessages: Starting incremental delta updates (E, F, G) for chats: $finalChatIds")
            
            for (chatId in finalChatIds) {
                val syncResult = syncUpdatedMessages(chatId)
                if (syncResult.isFailure) {
                    allSuccessful = false
                    Log.e(TAG, "syncAllPendingAndUpdatedMessages: syncUpdatedMessages failed for chat $chatId")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "syncAllPendingAndUpdatedMessages: Global sync cycle exception", e)
            allSuccessful = false
        }

        return@withContext allSuccessful
    }

    suspend fun saveReaction(messageId: String, chatId: String, userId: String, emoji: String) = withContext(Dispatchers.IO) {
        // 1. Update auxiliary table as requested by user
        val reactionEntity = com.example.data.database.ReactionEntity(messageId, userId, emoji, SupabaseClient.getNowIsoString())
        db.reactionDao().insertReaction(reactionEntity)

        // 2. Update reactionsJson in MessageEntity for fast UI rendering
        val messages = messageDao.getMessagesForChat(chatId)
        val msg = messages.find { it.id == messageId }
        val reactionsMap = if (msg != null && msg.reactionsJson.isNotEmpty()) {
            try {
                org.json.JSONObject(msg.reactionsJson)
            } catch (e: Exception) {
                org.json.JSONObject()
            }
        } else {
            org.json.JSONObject()
        }

        reactionsMap.put(userId, emoji)
        val updatedJson = reactionsMap.toString()

        messageDao.markMessageReactionPending(messageId, updatedJson)
        SupabaseClient.broadcastReaction(messageId, chatId, emoji)

        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService
                if (service != null) {

                    val body = mapOf(
                            "thread_message_id" to messageId,
                            "user_id" to userId,
                            "emoji" to emoji
                        )
                    val response = runCall { authHeader ->
                        service.upsertMessageReaction(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = authHeader,
                            prefer = "resolution=merge-duplicates",
                            reaction = body
                        )
                    }
                    if (response != null && response.isSuccessful) {
                        messageDao.clearMessageReactionPending(messageId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist reaction on Supabase: ${e.localizedMessage}", e)
            }
        }
    }

    suspend fun deleteReaction(messageId: String, chatId: String, userId: String) = withContext(Dispatchers.IO) {
        // 1. Update auxiliary table
        db.reactionDao().deleteReaction(messageId, userId)

        // 2. Update reactionsJson in MessageEntity
        val messages = messageDao.getMessagesForChat(chatId)
        val msg = messages.find { it.id == messageId }
        val reactionsMap = if (msg != null && msg.reactionsJson.isNotEmpty()) {
            try {
                org.json.JSONObject(msg.reactionsJson)
            } catch (e: Exception) {
                org.json.JSONObject()
            }
        } else {
            org.json.JSONObject()
        }

        reactionsMap.remove(userId)
        val updatedJson = reactionsMap.toString()

        messageDao.markMessageReactionPending(messageId, updatedJson)
        SupabaseClient.broadcastReaction(messageId, chatId, "")

        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService
                if (service != null) {

                    val response = runCall { authHeader ->
                        service.deleteMessageReaction(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = authHeader,
                                threadMessageIdFilter = "eq.$messageId",
                                userIdFilter = "eq.$userId"
                            )
                    }
                    if (response != null && response.isSuccessful) {
                        messageDao.clearMessageReactionPending(messageId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete reaction on Supabase: ${e.localizedMessage}", e)
            }
        }
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToId: String? = null,
        receiverUid: String? = null,
        messageId: String? = null,
        messageType: String = "text",
        mediaUrl: String? = null,
        thumbnailUrl: String? = null,
        mediaMime: String? = null,
        mediaSize: Long? = null,
        duration: Long? = null,
        width: Int? = null,
        height: Int? = null,
        clientMessageUuid: String? = null,
        isGhost: Boolean = false
    ): Result<Message> = withContext(Dispatchers.IO) {
        val currentUid = if (SupabaseClient.isConfigured) {
            val uid = SupabaseClient.currentUser?.id
            if (uid.isNullOrEmpty()) {
                Log.e(TAG, "sendMessage: Authenticated user ID is missing or null!")
                return@withContext Result.failure(Exception("No autenticado (UID faltante)"))
            }
            uid
        } else {
            "me_demo_id"
        }
        val nowStr = SupabaseClient.getNowIsoString()
        val clientUuid = clientMessageUuid ?: UUID.randomUUID().toString()
        val tempId = messageId ?: if (SupabaseClient.isConfigured) {
            "temp_${UUID.randomUUID()}"
        } else {
            "msg_${UUID.randomUUID()}"
        }

        val formattedContent = if (isGhost && !content.startsWith("[Ghost]")) {
            "[Ghost] $content"
        } else {
            content
        }

        val message = Message(
            id = tempId,
            chatId = chatId,
            senderId = currentUid,
            content = formattedContent,
            createdAt = nowStr,
            status = "sending",
            replyToMessageId = replyToId,
            clientMessageUuid = clientUuid,
            thumbnailUrl = thumbnailUrl,
            mediaUrl = mediaUrl,
            mediaMime = mediaMime,
            mediaSize = mediaSize,
            duration = duration,
            width = width,
            height = height,
            messageType = messageType,
            isGhost = isGhost
        )

        // Save to local Room first for true offline-first
        messageDao.insertMessage(MessageEntity.fromMessage(message))

        if (!SupabaseClient.isConfigured) {
            delay(300)
            messageDao.updateMessageStatus(tempId, "sent")
            val updatedMsg = message.copy(status = "sent")
            SupabaseClient.demoMessages.add(updatedMsg)
            simulateDemoReply(chatId, content)
            return@withContext Result.success(updatedMsg)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(message)
            SessionManager.validateAndRefreshSessionIfNeeded()

            val identity = resolveChatIdentity(chatId, receiverUid)
            var finalReceiverUid = identity.receiverId ?: receiverUid
            val isDm = identity.kind == ChatKind.DM

            if (identity.kind == ChatKind.UNKNOWN) {
                Log.w(TAG, "sendMessage: Chat $chatId identity UNKNOWN. Keeping message local and scheduling sync.")
                scheduleSync()
                return@withContext Result.success(message)
            }

            // We need to use Supabase API to insert the message
            val receiverPublicKey = if (isDm && !finalReceiverUid.isNullOrEmpty()) {
                com.example.data.repository.UserKeysRepository.getPublicKeyForUser(finalReceiverUid)
            } else {
                null
            }

            val contentToUpload = if (!receiverPublicKey.isNullOrEmpty()) {
                Log.d(TAG, "Encrypting message content using E2EE")
                com.example.util.CryptoManager.encrypt(formattedContent, receiverPublicKey)
            } else {
                formattedContent
            }

            // Rule: If it's a multimedia message and the URL is still NULL, 
            // we SKIP the sync for now. The MediaUploadWorker will update the URL and trigger sync later.
            if (messageType.lowercase() != "text" && mediaUrl.isNullOrBlank()) {
                Log.d(TAG, "Multimedia message without URL, letting MediaUploadWorker handle it later.")
                // Unhide the chat for the sender
                try {
                    runCall { auth ->
                        service.updateChatParticipant(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            chatIdFilter = "eq.$chatId",
                            userIdFilter = "eq.$currentUid",
                            updates = mapOf<String, Any>("is_hidden" to false)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error unhiding chat", e)
                }
                return@withContext Result.success(message)
            }

            // Unhide the chat for the sender
            try {
                runCall { auth ->
                    service.updateChatParticipant(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        chatIdFilter = "eq.$chatId",
                        userIdFilter = "eq.$currentUid",
                        updates = mapOf<String, Any>("is_hidden" to false)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unhiding chat", e)
            }

            val remoteId = if (isValidUuid(tempId)) tempId else java.util.UUID.randomUUID().toString()
            val normalizedMessageType = when {
                messageType == "voice" || messageType == "voice_note" || messageType.startsWith("audio/") -> "audio"
                messageType in listOf("image", "video", "audio", "document", "sticker", "gif", "text", "location", "call") -> messageType
                messageType.startsWith("image/") -> if (messageType == "image/gif") "gif" else if (messageType == "image/webp") "sticker" else "image"
                messageType.startsWith("video/") -> "video"
                messageType.startsWith("application/") || messageType.startsWith("text/") -> "document"
                else -> messageType.lowercase()
            }

            val msgMap = mutableMapOf<String, Any?>(
                "id" to remoteId,
                "thread_id" to if (isDm) (identity.threadId ?: chatId) else null,
                "chat_id" to if (isDm) null else chatId,
                "sender_id" to currentUid,
                "receiver_id" to if (isDm) finalReceiverUid?.takeIf { isValidUuid(it) } else null,
                "reply_to" to replyToId?.takeIf { isValidUuid(it) },
                "text_content" to contentToUpload,
                "client_message_uuid" to clientUuid,
                "created_at" to nowStr,
                "media_url" to mediaUrl,
                "thumbnail_url" to thumbnailUrl,
                "media_mime" to mediaMime,
                "message_type" to normalizedMessageType,
                "file_size" to mediaSize,
                "duration" to duration,
                "width" to width,
                "height" to height
            )
            if (isGhost) {
                msgMap["is_ghost"] = true
            }
            val cleanMsgMap = msgMap.filterValues { it != null }

            var successful = false
            var errorStr = ""

            val response = runCall { auth ->
                if (isDm) {
                    service.createThreadMessage(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        message = cleanMsgMap
                    )
                } else {
                    service.createMessage(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        message = cleanMsgMap
                    )
                }
            }

            var returnedId: String? = null
            if (response != null && response.isSuccessful) {
                successful = true
                try {
                    val bodyStr = response.body()?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val jsonArray = org.json.JSONArray(bodyStr)
                        if (jsonArray.length() > 0) {
                            returnedId = jsonArray.getJSONObject(0).optString("id", null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response body", e)
                }
            } else {
                val errorCode = response?.code()
                val errorBody = response?.errorBody()?.string() ?: "Response is null"
                errorStr = "Code: $errorCode, Body: $errorBody"
                Log.w(TAG, "createThreadMessage failed: $errorStr.")
                
                // FALLBACK TO LEGACY MESSAGES TABLE (ONLY FOR NON-DM)
                if (!isDm) {
                    Log.i(TAG, "Non-DM chat detected. Trying legacy createMessage fallback.")
                    val legacyMsgMap = mutableMapOf<String, Any?>(
                        "id" to remoteId,
                        "thread_id" to null,
                        "chat_id" to chatId,
                        "sender_id" to currentUid,
                        "receiver_id" to null,
                        "text_content" to contentToUpload,
                        "client_message_uuid" to clientUuid,
                        "created_at" to nowStr,
                        "media_url" to mediaUrl,
                        "thumbnail_url" to thumbnailUrl,
                        "media_mime" to mediaMime,
                        "message_type" to normalizedMessageType,
                        "file_size" to mediaSize,
                        "duration" to duration,
                        "width" to width,
                        "height" to height
                    )
                    if (replyToId != null && isValidUuid(replyToId)) {
                        legacyMsgMap["reply_to"] = replyToId
                    }
                    val cleanLegacyMsgMap = legacyMsgMap.filterValues { it != null }

                    val legacyResponse = runCall { auth ->
                        service.createMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            message = cleanLegacyMsgMap
                        )
                    }

                    if (legacyResponse != null && legacyResponse.isSuccessful) {
                        successful = true
                        try {
                            val bodyStr = legacyResponse.body()?.string()
                            if (!bodyStr.isNullOrBlank()) {
                                val jsonArray = org.json.JSONArray(bodyStr)
                                if (jsonArray.length() > 0) {
                                    returnedId = jsonArray.getJSONObject(0).optString("id", null)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing legacy response body", e)
                        }
                    } else {
                        val legacyCode = legacyResponse?.code()
                        val legacyError = legacyResponse?.errorBody()?.string() ?: "Legacy response is null"
                        errorStr = "ThreadMsgErr: $errorStr | LegacyErr: (Code: $legacyCode, Body: $legacyError)"
                        Log.e(TAG, "Legacy createMessage also failed: $errorStr")
                    }
                } else {
                    Log.e(TAG, "DM creation failed: $errorStr. No legacy fallback allowed.")
                }
            }

            if (successful) {
                val finalId = returnedId?.takeIf { it.isNotBlank() } ?: remoteId
                val updated = message.copy(id = finalId, status = "sent")
                val effectiveClearedAt = getEffectiveClearedAt(updated.chatId, null)
                val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                    messageId = updated.id,
                    messageClientUuid = updated.clientMessageUuid,
                    messageCreatedAt = updated.createdAt,
                    lastClearedAt = effectiveClearedAt,
                    deletedMessageIds = getUserDeletedMessageIds()
                )
                if (shouldKeep) {
                    messageDao.replaceTemporaryMessage(tempId, MessageEntity.fromMessage(updated))
                } else {
                    messageDao.deleteMessageById(tempId)
                }
                return@withContext Result.success(updated)
            } else {
                messageDao.updateMessageStatus(tempId, "failed")
                scheduleSync()
                withContext(Dispatchers.Main) {
                    try {
                        android.widget.Toast.makeText(
                            com.example.PanaApplication.instance,
                            "Error Supabase: $errorStr",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (t: Throwable) {}
                }
                return@withContext Result.failure(Exception("Failed to send message: $errorStr"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage exception", e)
            withContext(Dispatchers.Main) {
                try {
                    android.widget.Toast.makeText(
                        com.example.PanaApplication.instance,
                        "Error de envío: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (t: Throwable) {}
            }
            messageDao.updateMessageStatus(tempId, "failed")
            scheduleSync()
            Result.failure(e)
        }
    }

    private suspend fun decryptMessages(messages: List<Message>): List<Message> {
        if (messages.isEmpty() || !com.example.util.CryptoManager.ENABLE_E2EE) return messages

        // Group messages by chatId to pre-fetch chat and profile details efficiently
        val chatGroups = messages.groupBy { it.chatId }
        val decryptedList = mutableListOf<Message>()

        val currentUid = try { SupabaseClient.currentUser?.id ?: "" } catch (e: Throwable) { "" }

        for ((chatId, groupMessages) in chatGroups) {
            try {
                // 1. Get chat details
                val chatEntity = db.chatDao().getChatById(chatId)
                val chatType = chatEntity?.type
                val otherUserId = chatEntity?.otherUserId

                // 2. Resolve other participant's public key
                var otherPubKey: String? = null
                if (!otherUserId.isNullOrEmpty()) {
                    otherPubKey = com.example.util.CryptoManager.publicKeyCache[otherUserId]
                    if (otherPubKey.isNullOrEmpty()) {
                        // Pre-fetch using UserKeysRepository
                        val fetchedKey = com.example.data.repository.UserKeysRepository.getPublicKeyForUser(otherUserId)
                        if (!fetchedKey.isNullOrEmpty()) {
                            otherPubKey = fetchedKey
                        }
                    }
                }

                // Decrypt each message using decryptMessagePure
                for (msg in groupMessages) {
                    var finalPubKey = otherPubKey
                    // If msg sender is not me and not otherUserId, check that sender's key (e.g. group chats / community)
                    if (msg.senderId != currentUid && msg.senderId.isNotEmpty() && msg.senderId != otherUserId) {
                        val msgSenderId = msg.senderId
                        var senderKey = com.example.util.CryptoManager.publicKeyCache[msgSenderId]
                        if (senderKey.isNullOrEmpty()) {
                            val fetchedKey = com.example.data.repository.UserKeysRepository.getPublicKeyForUser(msgSenderId)
                            if (!fetchedKey.isNullOrEmpty()) {
                                senderKey = fetchedKey
                            }
                        }
                        if (!senderKey.isNullOrEmpty()) {
                            finalPubKey = senderKey
                        }
                    }

                    val decryptedMsg = com.example.util.CryptoManager.decryptMessagePure(msg, chatType, finalPubKey)
                    decryptedList.add(decryptedMsg)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error batch decrypting messages for chat $chatId: ${e.localizedMessage}", e)
                // Fallback to individual decryption if anything goes wrong to ensure robustness (Cero Regresiones)
                for (msg in groupMessages) {
                    decryptedList.add(com.example.util.CryptoManager.decryptMessageIfNeeded(msg))
                }
            }
        }

        // Return messages sorted/positioned in their original order
        val messageOrderMap = messages.withIndex().associate { it.value.id to it.index }
        return decryptedList.sortedBy { messageOrderMap[it.id] ?: Int.MAX_VALUE }
    }

    // Stream new messages from Supabase Realtime channel and insert into Room (ciphertext) and emit


    suspend fun markThreadDelivered(chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (com.example.data.repository.PrivacyManager.isPremiumFeatureActive("hide_double_ticks_received")) {
            return@withContext Result.success(true)
        }
        if (!SupabaseClient.isConfigured) return@withContext Result.success(true)
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val response = runCall { auth ->
                service.markThreadDelivered(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    params = mapOf("p_thread_id" to chatId)
                )
            }
            if (response != null && response.isSuccessful) {
                return@withContext Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking thread delivered: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun markThreadRead(chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: ""
        val nowStr = SupabaseClient.getNowIsoString()
        try {
            messageDao.markChatMessagesAsRead(chatId, currentUid, nowStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local messages as read: ${e.localizedMessage}", e)
        }
        
        val isBlueTicksHidden = com.example.data.repository.PrivacyManager.isPremiumFeatureActive("hide_blue_ticks")
        val sendOnReply = com.example.data.repository.PrivacyManager.isPremiumFeatureActive("send_blue_tick_on_reply")
        
        if (isBlueTicksHidden || sendOnReply) {
            return@withContext Result.success(true)
        }
        if (!SupabaseClient.isConfigured) return@withContext Result.success(true)
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val response = runCall { auth ->
                service.markThreadRead(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    params = mapOf("p_thread_id" to chatId)
                )
            }

            if (response != null && response.isSuccessful) {
                return@withContext Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking thread read: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun consumeGhostMessage(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        try {
            // Update local Room database immediately
            messageDao.updateGhostOpenedAt(messageId, nowStr)
            
            val currentUid = SupabaseClient.currentUser?.id
            if (currentUid != null && SupabaseClient.isConfigured) {
                val service = SupabaseClient.apiService
                if (service != null) {
                    runCall { auth ->
                        service.updateThreadMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            idFilter = "eq.$messageId",
                            updates = mapOf("ghost_opened_at" to nowStr)
                        )
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming ghost message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageDefinitively(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            messageDao.deleteMessageById(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local message: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) return@withContext Result.success(true)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val response = runCall { auth ->
                service.deleteThreadMessage(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    idFilter = "eq.$messageId"
                )
            }
            if (response != null && response.isSuccessful) {
                return@withContext Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error definitively deleting message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun clearChat(chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }.format(java.util.Date())
        localClearedAtMap[chatId] = nowStr

        try {
            messageDao.clearChatMessages(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local chat messages: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) return@withContext Result.success(true)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val apiKey = SupabaseClient.supabaseAnonKey
            val currentUser = com.example.data.supabase.SupabaseClient.currentUser?.id 
                ?: return@withContext Result.failure(Exception("No user logged in"))

            // Upsert chat member row to update last_cleared_at
            val upsertRes = runCall { auth ->
                service.upsertChatMemberMap(
                    apiKey = apiKey,
                    authorization = auth,
                    prefer = "resolution=merge-duplicates",
                    memberData = mapOf("chat_id" to chatId, "user_id" to currentUser, "last_cleared_at" to nowStr)
                )
            }

            if (upsertRes?.isSuccessful == true) {
                return@withContext Result.success(true)
            }

            // Fallback to updateChatParticipant
            val result = runCall { auth -> 
                service.updateChatParticipant(
                    apiKey = apiKey,
                    authorization = auth,
                    chatIdFilter = "eq.$chatId",
                    userIdFilter = "eq.$currentUser",
                    updates = mapOf<String, Any>("last_cleared_at" to nowStr)
                ) 
            }
            
            if (result?.isSuccessful == true) {
                Result.success(true)
            } else {
                Log.e(TAG, "Error from updateChatParticipant (clearChat): ${result?.errorBody()?.string()}")
                // Try fallback
                val fallbackResult = runCall { auth -> 
                    service.clearChatRpc(
                        apiKey = apiKey,
                        authorization = auth,
                        params = mapOf("p_chat_id" to chatId)
                    ) 
                }
                if (fallbackResult?.isSuccessful == true) {
                    Result.success(true)
                } else {
                    Result.success(true) // Treat as success since local cache & map updated
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error personal clearing chat messages: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            messageDao.markMessageEditPending(messageId, newContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing local message content: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            val index = SupabaseClient.demoMessages.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val old = SupabaseClient.demoMessages[index]
                val updated = old.copy(content = newContent, isEdited = true)
                SupabaseClient.demoMessages[index] = updated
                SupabaseClient.emitRealtimeMessage(updated)
            }
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val response = runCall { auth ->
                service.updateThreadMessage(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    idFilter = "eq.$messageId",
                    updates = mapOf(
                        "text_content" to newContent
                    )
                )
            }
            if (response != null && response.isSuccessful) {
                messageDao.clearMessageEditPending(messageId)
                return@withContext Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }.format(java.util.Date())
        
        try {
            messageDao.markMessageDeletePending(messageId, nowStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local message to deleted: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            val index = SupabaseClient.demoMessages.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val old = SupabaseClient.demoMessages[index]
                val updated = old.copy(status = "deleted")
                SupabaseClient.demoMessages[index] = updated
                SupabaseClient.emitRealtimeMessage(updated)
            }
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(false)
            val response = runCall { auth ->
                service.updateThreadMessage(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    idFilter = "eq.$messageId",
                    updates = mapOf(
                        "status" to "deleted",
                        "deleted_at" to nowStr
                    )
                )
            }
            if (response != null && response.isSuccessful) {
                messageDao.clearMessageDeletePending(messageId)
                return@withContext Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message for everyone: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        userDeletedMessageIds.add(messageId)
        try {
            messageDao.deleteMessageById(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local message for me: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val response = runCall { auth ->
                service.deleteMessageForMeRpc(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    params = mapOf("p_message_id" to messageId)
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(true)
            } else {
                Log.w(TAG, "deleteMessageForMe: RPC returned code ${response?.code()}")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing delete_message_for_me RPC: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {
        val newFavorited = !message.isFavorited
        
        // 1. Update locally first (Optimistic update)
        try {
            messageDao.updateMessageFavoriteStatus(message.id, newFavorited)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update local favorite status", e)
            return@withContext Result.failure(e)
        }

        // 2. Sync to Supabase in background, don't fail if it fails
        if (!SupabaseClient.isConfigured) return@withContext Result.success(newFavorited)
        val myUserId = SupabaseClient.currentUser?.id ?: return@withContext Result.success(newFavorited)

        try {
            val service = SupabaseClient.apiService
            if (service != null) {
                if (newFavorited) {
                    runCall { auth ->
                        service.addFavoriteMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            body = mapOf(
                                "user_id" to myUserId,
                                "message_id" to message.id
                            )
                        )
                    }
                } else {
                    runCall { auth ->
                        service.removeFavoriteMessage(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authorization = auth,
                            userIdFilter = "eq.$myUserId",
                            messageIdFilter = "eq.${message.id}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing favorite", e)
        }
        
        Result.success(newFavorited)
    }

    fun getFavoritedMessagesFlow(): Flow<List<Message>> {
        return messageDao.getFavoritedMessagesFlow().map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun syncFavorites(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext Result.success(Unit)
        val myUserId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not logged in"))
        
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { auth ->
                service.getThreadMessageFavorites(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    userId = "eq.$myUserId"
                )
            }

            if (response?.isSuccessful == true) {
                val favorites = response.body() ?: emptyList()
                val favoritedMessageIds = favorites.mapNotNull { it["message_id"] as? String }
                
                // Update local DB: set isFavorited = 1 for these IDs, and 0 for others that were favorited
                val currentFavorited = messageDao.getFavoritedMessages()
                currentFavorited.forEach { entity ->
                    if (!favoritedMessageIds.contains(entity.id)) {
                        messageDao.updateMessageFavoriteStatus(entity.id, false)
                    }
                }
                
                favoritedMessageIds.forEach { id ->
                    messageDao.updateMessageFavoriteStatus(id, true)
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception(formatSupabaseError(response?.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun simulateDemoReply(chatId: String, userMessage: String) {
        val otherMemberId = SupabaseClient.demoChatMembers
            .firstOrNull { it.chatId == chatId && it.userId != "me_demo_id" }?.userId ?: return

        val replyText = when {
            userMessage.contains("hola", ignoreCase = true) || userMessage.contains("épa", ignoreCase = true) -> {
                "¡Épa mi pana! ¿Todo fino? ¿Cómo van las cosas por allá?"
            }
            userMessage.contains("cerveza", ignoreCase = true) || userMessage.contains("fría", ignoreCase = true) || userMessage.contains("🍻") -> {
                "¡Uff de una! Activemoooos 🍻 unas polarcitas bien frías, tú pones los hielos mano!"
            }
            userMessage.contains("arepa", ignoreCase = true) || userMessage.contains("comida", ignoreCase = true) -> {
                "De pana que me comería una de carne mechada con full queso amarillo, ¡qué sabroso! 🤤"
            }
            else -> {
                "¡Buenísimo mi pana! Háblame, ¿qué más cuentas?"
            }
        }

        repositoryScope.launch {
            SupabaseClient.emitRealtimeTyping(SupabaseClient.TypingStatus(chatId, otherMemberId, true))
            delay(2000)
            SupabaseClient.emitRealtimeTyping(SupabaseClient.TypingStatus(chatId, otherMemberId, false))
            delay(300)

            val replyMessage = Message(
                id = "msg_sim_${UUID.randomUUID().toString().take(6)}",
                chatId = chatId,
                senderId = otherMemberId,
                content = replyText,
                createdAt = SupabaseClient.getNowIsoString(),
                status = "read"
            )
            
            messageDao.insertMessage(MessageEntity.fromMessage(replyMessage))
            SupabaseClient.demoMessages.add(replyMessage)
            SupabaseClient.emitRealtimeMessage(replyMessage)
        }
    }

    private fun triggerSendPushNotification(chatId: String, recipientUserId: String, title: String, bodyText: String) {
        repositoryScope.launch {
            if (!SupabaseClient.isConfigured) {
                Log.d(TAG, "[Demo Mode] triggerSendPushNotification to $recipientUserId, title=$title, body=$bodyText")
                return@launch
            }
            try {
                val service = SupabaseClient.apiService ?: return@launch
                
                val baseUrl = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
                val edgeFunctionUrl = if (baseUrl.contains(".supabase.co")) {
                    baseUrl.replace(".supabase.co", ".functions.supabase.co") + "/send-push"
                } else {
                    "$baseUrl/functions/v1/send-push"
                }
                
                val body = mapOf(
                    "user_id" to recipientUserId,
                    "title" to title,
                    "body" to bodyText,
                                "chat_id" to chatId,
                    "chatId" to chatId,
                    "notification_type" to "new_message",
                    "notificationType" to "new_message"
                )
                
                val authHeader = if (!SupabaseClient.currentToken.isNullOrEmpty()) {
                    "Bearer ${SupabaseClient.currentToken}"
                } else {
                    "Bearer ${SupabaseClient.supabaseAnonKey}"
                }

                Log.d(TAG, "Calling send-push edge function at $edgeFunctionUrl for recipient $recipientUserId")
                val response = service.callEdgeFunction(
                    url = edgeFunctionUrl,
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = authHeader,
                    body = body
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully invoked send-push Edge Function")
                } else {
                    val errMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to invoke send-push Edge Function: $errMsg (code=${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling send-push edge function", e)
            }
        }
    }

    private fun isValidUuid(uuidStr: String?): Boolean {
        if (uuidStr.isNullOrEmpty()) return false
        return try {
            java.util.UUID.fromString(uuidStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun formatSupabaseError(errorBodyStr: String?): String {
        if (errorBodyStr.isNullOrEmpty()) return "Unknown error"
        return try {
            val json = org.json.JSONObject(errorBodyStr)
            val message = json.optString("message", "")
            val code = json.optString("code", "")
            val details = json.optString("details", "")
            val hint = json.optString("hint", "")
            
            val sb = StringBuilder()
            if (message.isNotEmpty()) sb.append("Message: ").append(message)
            if (code.isNotEmpty()) sb.append(" (Code: ").append(code).append(")")
            if (details.isNotEmpty() && details != "null") sb.append(" | Details: ").append(details)
            if (hint.isNotEmpty() && hint != "null") sb.append(" | Hint: ").append(hint)
            
            if (sb.isEmpty()) errorBodyStr else sb.toString()
        } catch (e: Exception) {
            errorBodyStr
        }
    }

    suspend fun logDebug(etapa: String, info: String) {
        try {
            val service = SupabaseClient.apiService
            if (service != null) {
                runCall { auth ->
                    service.createDebugLog(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = auth,
                        logMap = mapOf(
                            "fecha" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                            "etapa" to etapa,
                            "response_body" to info
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging debug $etapa", e)
        }
    }

    private fun parseToEpochMilli(ts: String?): Long {
        if (ts.isNullOrEmpty()) return 0L
        return try {
            java.time.Instant.parse(ts).toEpochMilli()
        } catch (e1: Exception) {
            try {
                java.time.OffsetDateTime.parse(ts).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                try {
                    val cleaned = ts.replace(" ", "T")
                    if (!cleaned.contains("Z") && !cleaned.contains("+") && !cleaned.contains("-")) {
                        java.time.LocalDateTime.parse(cleaned).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                    } else {
                        java.time.OffsetDateTime.parse(cleaned).toInstant().toEpochMilli()
                    }
                } catch (e3: Exception) {
                    0L
                }
            }
        }
    }

    private fun isTimestampBeforeOrEqual(ts1: String?, ts2: String?): Boolean {
        if (ts1.isNullOrEmpty() || ts2.isNullOrEmpty()) return false
        val t1 = parseToEpochMilli(ts1)
        val t2 = parseToEpochMilli(ts2)
        if (t1 > 0L && t2 > 0L) {
            return t1 <= t2
        }
        return ts1 <= ts2
    }
}
