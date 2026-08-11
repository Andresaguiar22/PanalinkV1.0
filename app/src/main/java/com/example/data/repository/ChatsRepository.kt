package com.example.data.repository

import com.example.data.database.ChatEntity
import com.example.data.database.ProfileEntity
import android.util.Log
import com.example.data.model.*
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatsRepository {
    private val TAG = "ChatsRepository"
    private val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()

    private suspend fun <R> runCall(call: suspend (String) -> retrofit2.Response<R>): retrofit2.Response<R>? {
        return com.example.util.Resilience.retry(
            times = 5,
            initialDelay = 1000L,
            maxDelay = 10000L,
            factor = 2.0,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) || (it is retrofit2.HttpException && it.code() == 408) }
        ) {
            com.example.data.supabase.SessionManager.validateAndRefreshSessionIfNeeded()
            val token = SupabaseClient.currentToken ?: return@retry null
            val bearer = "Bearer $token"
            
            try {
                call(bearer)
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                throw e
            }
        }
    }

    suspend fun getChatIdByOtherUserId(otherUserId: String): String? = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext null
        val chats = chatDao.getAllChats()
        val existing = chats.firstOrNull { it.otherUserId == otherUserId }
        if (existing != null) return@withContext existing.id
        
        if (!SupabaseClient.isConfigured) return@withContext null
        
        // If not in cache, try finding via API
        val result = createDirectChat(otherUserId)
        return@withContext result.getOrNull()?.id
    }

    suspend fun getChatsWithDetails(): Result<List<ChatWithDetails>> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))

        // Load from local DB first
        val publicProfileDao = db.publicProfileDao()
        val cachedChats = chatDao.getAllChats()
        val localList = mutableListOf<ChatWithDetails>()
        for (chatEntity in cachedChats) {
            val otherProfile = chatEntity.otherUserId?.let { otherId ->
                val pubEntity = publicProfileDao.getById(otherId)
                if (pubEntity != null) {
                    PublicProfileResolver.toProfile(com.example.data.mapper.PublicProfileMapper.entityToModel(pubEntity))
                } else {
                    null
                }
            }
            // Actually, we can get the last message directly from messageDao for this chat
            val realLastMsg = messageDao.getLastMessageForChat(chatEntity.id)?.toMessage()
            val decryptedLastMsg = realLastMsg?.let { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }

            localList.add(ChatWithDetails(
                chat = chatEntity.toChat(),
                otherMember = otherProfile,
                lastMessage = decryptedLastMsg,
                unreadCount = chatEntity.unreadCount
            ))
        }
        localList.sortWith(
            compareByDescending<ChatWithDetails> { it.chat.isPinned }
                .thenByDescending { it.chat.pinnedAt ?: "" }
                .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
        )

        if (!SupabaseClient.isConfigured) {
            if (localList.isEmpty()) {
                // Initialize demo data if empty
                delay(800)
                val userChatIds = SupabaseClient.demoChatMembers
                    .filter { it.userId == currentUid }
                    .map { it.chatId }

                val demoList = mutableListOf<ChatWithDetails>()
                for (cid in userChatIds) {
                    val chat = SupabaseClient.demoChats[cid] ?: continue
                    val otherMemberId = SupabaseClient.demoChatMembers
                        .firstOrNull { it.chatId == cid && it.userId != currentUid }?.userId
                    val otherProfile = otherMemberId?.let { SupabaseClient.demoProfiles[it] }
                    val lastMsg = SupabaseClient.demoMessages
                        .filter { it.chatId == cid }
                        .maxByOrNull { it.createdAt }
                    
                    demoList.add(ChatWithDetails(chat, otherProfile, lastMsg))
                    
                    // Cache them
                    chatDao.insertChat(ChatEntity.fromChat(chat, otherMemberId, lastMsg?.id))
                    otherProfile?.let { p: com.example.data.model.Profile ->
                        val pubEntity = com.example.data.database.PublicProfileEntity(
                            id = p.id,
                            displayName = p.displayName,
                            firstName = p.firstName,
                            lastName = p.lastName,
                            avatarUrl = p.avatarUrl,
                            updatedAt = p.lastProfileEdit
                        )
                        publicProfileDao.upsert(pubEntity)
                    }
                }
                demoList.sortWith(
                    compareByDescending<ChatWithDetails> { it.chat.isPinned }
                        .thenByDescending { it.chat.pinnedAt ?: "" }
                        .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
                )
                return@withContext Result.success(demoList)
            }
            return@withContext Result.success(localList)
        }

        return@withContext Result.success(localList)
    }

    suspend fun syncChatsWithSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        val publicProfileDao = db.publicProfileDao()
        if (!SupabaseClient.isConfigured) return@withContext Result.success(Unit)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            SessionManager.validateAndRefreshSessionIfNeeded()
            var token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            var bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey

            suspend fun <R> runCallLocal(call: suspend (String) -> retrofit2.Response<R>): retrofit2.Response<R>? {
                var response = try {
                    call(bearer)
                } catch (e: Exception) {
                    Log.e(TAG, "Network call failed", e)
                    null
                }
                
                if (response != null && response.code() == 401) {
                    val refreshed = SessionManager.refreshSession()
                    if (refreshed) {
                        val newToken = SupabaseClient.currentToken ?: ""
                        bearer = "Bearer $newToken"
                        response = try {
                            call(bearer)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                return response
            }

            // Load from network
            val threadsResponse = runCallLocal { b ->
                service.getOneToOneThreads(apiKey = apiKey, authorization = b, orFilter = "(user_a.eq.$currentUid,user_b.eq.$currentUid)")
            }

            val membersResponse = runCallLocal { b ->
                service.getChatMembers(apiKey = apiKey, authorization = b, userIdFilter = "eq.$currentUid")
            }
            val membersMap = membersResponse?.body()?.associateBy { it.chatId } ?: emptyMap()

            if (threadsResponse != null && threadsResponse.isSuccessful) {
                val threads = threadsResponse.body() ?: emptyList()
                
                // Collect unique other member IDs to fetch their profiles specifically
                val otherMemberIds = threads.map { if (it.userA == currentUid) it.userB else it.userA }.toSet()
                val publicProfileRepo = PublicProfileRepository.getInstance()
                val profilesMap = if (otherMemberIds.isNotEmpty()) {
                    val publicResult = publicProfileRepo.getPublicProfiles(otherMemberIds.toList())
                    val map = mutableMapOf<String, Profile>()
                    if (publicResult is PublicProfileFetchResult.Success) {
                        for ((id, pubResult) in publicResult.data) {
                            if (pubResult is PublicProfileFetchResult.Success) {
                                map[id] = PublicProfileResolver.toProfile(pubResult.data)
                            }
                        }
                    }
                    map
                } else {
                    emptyMap()
                }

                for (thread in threads) {
                    val myMember = membersMap[thread.id]
                    if (myMember?.isHidden == true) {
                        try {
                            chatDao.deleteChatById(thread.id)
                            messageDao.clearChatMessages(thread.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing hidden chat locally: ${e.localizedMessage}")
                        }
                        continue
                    }

                    val otherMemberId = if (thread.userA == currentUid) thread.userB else thread.userA
                    val otherProfile = profilesMap[otherMemberId]

                    val localLastEntity = messageDao.getLastMessageForChat(thread.id)
                    val lastMsg = if (localLastEntity != null) {
                        localLastEntity.toMessage()
                    } else {
                        val msgResponse = runCallLocal { b -> service.getThreadMessages(apiKey = apiKey, authorization = b, threadIdFilter = "eq.${thread.id}", order = "created_at.desc", limit = 1) }
                        if (msgResponse != null && msgResponse.isSuccessful && !msgResponse.body().isNullOrEmpty()) {
                            val msg = msgResponse.body()!![0].toMessage()
                            val msgsRepo = com.example.data.repository.MessagesRepository.getInstance()
                            val effectiveClearedAt = msgsRepo.getEffectiveClearedAt(thread.id, myMember?.lastClearedAt)
                            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                                messageId = msg.id,
                                messageClientUuid = msg.clientMessageUuid,
                                messageCreatedAt = msg.createdAt,
                                lastClearedAt = effectiveClearedAt,
                                deletedMessageIds = msgsRepo.getUserDeletedMessageIds()
                            )
                            if (shouldKeep) {
                                messageDao.insertMessage(com.example.data.database.MessageEntity.fromMessage(msg))
                                msg
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    val unreadCount = try {
                        messageDao.getUnreadCountForChat(thread.id, currentUid)
                    } catch (e: Exception) {
                        0
                    }

                    val localChatEntity = chatDao.getChatById(thread.id)
                    val isMuted = myMember?.isMuted == true || (localChatEntity?.isMuted == true)
                    val isPinned = myMember?.isPinned == true || (localChatEntity?.isPinned == true)
                    val pinnedAt = myMember?.pinnedAt ?: localChatEntity?.pinnedAt
                    val chat = thread.toChat(isMuted = isMuted, isPinned = isPinned, pinnedAt = pinnedAt)

                    // Sync to DB
                    chatDao.insertChat(ChatEntity.fromChat(chat, otherMemberId, lastMsg?.id, unreadCount))
                    otherProfile?.let { p: com.example.data.model.Profile ->
                        val pubEntity = com.example.data.database.PublicProfileEntity(
                            id = p.id,
                            displayName = p.displayName,
                            firstName = p.firstName,
                            lastName = p.lastName,
                            avatarUrl = p.avatarUrl,
                            updatedAt = p.lastProfileEdit
                        )
                        publicProfileDao.upsert(pubEntity)
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sync failed: Threads query unsuccessful"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncChatsWithSupabase exception", e)
            Result.failure(e)
        }
    }

    suspend fun getChatById(chatId: String): Result<Chat?> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            
            val response = service.getChat(apiKey, bearer, "eq.$chatId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalChat(chatId: String): Chat? = withContext(Dispatchers.IO) {
        try {
            chatDao.getChatById(chatId)?.toChat()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local chat", e)
            null
        }
    }

    suspend fun getParticipant(chatId: String, userId: String): Result<ChatMember?> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            
            val response = service.getChatParticipant(apiKey, bearer, "eq.$chatId", "eq.$userId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinChannel(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        val nowStr = SupabaseClient.getNowIsoString()
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            
            val member = ChatMember(chatId, currentUid, "member", nowStr)
            val response = service.createChatMember(apiKey, bearer, member)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to join"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscriberCount(chatId: String): Int = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext 0
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken ?: return@withContext 0
            val bearer = "Bearer $token"
            
            // PostgREST "Prefer: count=exact" for row counts
            val response = service.getChatParticipantsCount(apiKey, bearer, "eq.$chatId")
            if (response.isSuccessful) {
                // PostgREST content-range looks like "0-0/123"
                val range = response.headers()["Content-Range"]
                if (range != null && range.contains("/")) {
                    return@withContext range.substringAfter("/").toIntOrNull() ?: 0
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun createDirectChat(otherUserId: String): Result<Chat> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        val nowStr = SupabaseClient.getNowIsoString()

        if (!SupabaseClient.isConfigured) {
            delay(1000)
            // Check if DM chat already exists in demo
            val existingChatId = SupabaseClient.demoChatMembers
                .filter { it.userId == currentUid }
                .map { it.chatId }
                .firstOrNull { cid ->
                    SupabaseClient.demoChatMembers.any { it.chatId == cid && it.userId == otherUserId }
                }

            if (existingChatId != null) {
                val chat = SupabaseClient.demoChats[existingChatId]
                if (chat != null) return@withContext Result.success(chat)
            }

            // Create new demo chat
            val newId = "chat_${UUID.randomUUID().toString().take(6)}"
            val newChat = Chat(newId, nowStr, "dm")
            
            SupabaseClient.demoChats[newId] = newChat
            SupabaseClient.demoChatMembers.add(ChatMember(newId, currentUid, "member", nowStr))
            SupabaseClient.demoChatMembers.add(ChatMember(newId, otherUserId, "member", nowStr))

            return@withContext Result.success(newChat)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey

            val userA = if (currentUid < otherUserId) currentUid else otherUserId
            val userB = if (currentUid < otherUserId) otherUserId else currentUid

            // Step 1: Search for existing thread
            val threadsResponse = service.getOneToOneThreads(
                apiKey = apiKey,
                authorization = bearer,
                orFilter = "(user_a.eq.$currentUid,user_b.eq.$currentUid)"
            )
            if (threadsResponse.isSuccessful) {
                val threads = threadsResponse.body() ?: emptyList()
                val existingThread = threads.firstOrNull { 
                    (it.userA == userA && it.userB == userB) || (it.userA == userB && it.userB == userA)
                }
                if (existingThread != null) {
                    Log.d(TAG, "Found existing thread: ${existingThread.id}")
                    return@withContext Result.success(existingThread.toChat())
                }
            }

            // Step 2: Create a new thread since it doesn't exist
            val threadBody = mapOf("user_a" to userA, "user_b" to userB)
            val createResponse = service.createOneToOneThread(apiKey, bearer, "return=representation", threadBody)
            if (createResponse.isSuccessful && !createResponse.body().isNullOrEmpty()) {
                val newThread = createResponse.body()!![0]
                Log.d(TAG, "Created new thread: ${newThread.id}")
                return@withContext Result.success(newThread.toChat())
            } else {
                val errMsg = createResponse.errorBody()?.string() ?: "Failed to create thread"
                Log.e(TAG, "Failed to create thread: $errMsg")
                return@withContext Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createDirectChat exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateChannelReadOnlyStatus(chatId: String, isReadonly: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            
            val response = service.updateChat(apiKey, bearer, "eq.$chatId", mapOf("is_readonly" to isReadonly))
            if (response.isSuccessful) {
                try {
                    val existing = chatDao.getChatById(chatId)
                    if (existing != null) {
                        chatDao.insertChat(existing.copy(isReadonly = isReadonly))
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to update local db chat isReadonly", e)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update read-only status"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateChannelReadOnlyStatus exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteChatLocallyAndRemotely(chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }.format(java.util.Date())
        com.example.data.repository.MessagesRepository.getInstance().localClearedAtMap[chatId] = nowStr

        try {
            chatDao.deleteChatById(chatId)
            messageDao.clearChatMessages(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat locally: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            SupabaseClient.demoChats.remove(chatId)
            SupabaseClient.demoMessages.removeAll { it.chatId == chatId }
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val apiKey = SupabaseClient.supabaseAnonKey
            val currentUser = com.example.data.supabase.SupabaseClient.currentUser?.id 
                ?: return@withContext Result.success(true)

            val upsertRes = runCall { auth ->
                service.upsertChatMemberMap(
                    apiKey = apiKey,
                    authorization = auth,
                    prefer = "resolution=merge-duplicates",
                    memberData = mapOf("chat_id" to chatId, "user_id" to currentUser, "is_hidden" to true, "last_cleared_at" to nowStr)
                )
            }
            if (upsertRes?.isSuccessful == true) {
                return@withContext Result.success(true)
            }

            val result = runCall { auth -> 
                service.updateChatParticipant(
                    apiKey = apiKey,
                    authorization = auth,
                    chatIdFilter = "eq.$chatId",
                    userIdFilter = "eq.$currentUser",
                    updates = mapOf<String, Any>("is_hidden" to true, "last_cleared_at" to nowStr)
                ) 
            }
            
            if (result?.isSuccessful == true) {
                Result.success(true)
            } else {
                Log.e(TAG, "Error from updateChatParticipant (hideChat): ${result?.errorBody()?.string()}")
                val fallbackResult = runCall { auth -> 
                    service.hideChatRpc(
                        apiKey = apiKey,
                        authorization = auth,
                        params = mapOf("p_chat_id" to chatId)
                    ) 
                }
                if (fallbackResult?.isSuccessful != true) {
                    Log.e(TAG, "Error from hideChatRpc: ${fallbackResult?.errorBody()?.string()}")
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error personal deleting remote chat: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    suspend fun archiveChat(chatId: String, isArchived: Boolean = true): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val existing = chatDao.getChatById(chatId)
            if (existing != null) {
                chatDao.insertChat(existing.copy(isArchived = isArchived))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local chat isArchived: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val apiKey = SupabaseClient.supabaseAnonKey
            runCall { auth ->
                service.updateChat(
                    apiKey = apiKey,
                    authorization = auth,
                    idFilter = "eq.$chatId",
                    updates = mapOf("is_archived" to isArchived)
                )
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating remote chat isArchived: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    suspend fun muteChat(chatId: String, isMuted: Boolean = true): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val existing = chatDao.getChatById(chatId)
            if (existing != null) {
                chatDao.insertChat(existing.copy(isMuted = isMuted))
            } else {
                chatDao.updateChatMuteStatus(chatId, isMuted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local chat isMuted: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val apiKey = SupabaseClient.supabaseAnonKey
            runCall { auth ->
                service.updateChatMuteStatusRpc(
                    apiKey = apiKey,
                    authorization = auth,
                    params = mapOf("p_chat_id" to chatId, "p_is_muted" to isMuted)
                )
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating remote chat mute status RPC: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    suspend fun pinChat(chatId: String, isPinned: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentTimestamp = if (isPinned) java.time.Instant.now().toString() else null
        try {
            val existing = chatDao.getChatById(chatId)
            if (existing != null) {
                chatDao.insertChat(existing.copy(isPinned = isPinned, pinnedAt = currentTimestamp))
            } else {
                chatDao.updateChatPinStatus(chatId, isPinned, currentTimestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local chat isPinned: ${e.localizedMessage}", e)
        }

        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val apiKey = SupabaseClient.supabaseAnonKey
            runCall { auth ->
                service.updateChatPinStatusRpc(
                    apiKey = apiKey,
                    authorization = auth,
                    params = mapOf("p_chat_id" to chatId, "p_is_pinned" to isPinned)
                )
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating remote chat pin status RPC: ${e.localizedMessage}", e)
            Result.success(true)
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
