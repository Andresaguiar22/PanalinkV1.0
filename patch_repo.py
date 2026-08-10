import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

replacement = """    suspend fun toggleMessageFavorite(message: Message): Result<Boolean> = withContext(Dispatchers.IO) {
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
    }"""

# Find the toggleMessageFavorite function
content = re.sub(r'    suspend fun toggleMessageFavorite\(message: Message\): Result<Boolean> = withContext\(Dispatchers.IO\) \{[\s\S]*?    fun getFavoritedMessagesFlow\(\): Flow<List<Message>> \{', replacement + '\n\n    fun getFavoritedMessagesFlow(): Flow<List<Message>> {', content)

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
