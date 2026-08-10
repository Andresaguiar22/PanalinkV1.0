package com.example.data.repository

import com.example.data.database.DraftEntity
import com.example.data.database.PanalinkDatabase
import com.example.data.supabase.SupabaseClient
import com.example.PanaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DraftsRepository {
    private val db = PanalinkDatabase.getDatabase(PanaApplication.instance)
    private val draftDao = db.draftDao()

    suspend fun saveChatDraft(chatId: String, text: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext
        if (text.isBlank()) {
            draftDao.deleteDraft(chatId, userId)
        } else {
            val draft = DraftEntity(
                draftId = chatId,
                userId = userId,
                type = "chat",
                content = text
            )
            draftDao.insertDraft(draft)
        }
    }

    suspend fun getChatDraft(chatId: String): String? = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext null
        return@withContext draftDao.getDraft(chatId, userId)?.content
    }

    suspend fun deleteChatDraft(chatId: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext
        draftDao.deleteDraft(chatId, userId)
    }
}
