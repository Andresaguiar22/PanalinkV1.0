package com.example.data.repository.reels

import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingSocialActionDao
import com.example.data.database.PendingSocialActionEntity
import com.example.data.database.ReelCommentReactionDao
import com.example.data.database.ReelCommentReactionEntity
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ReelCommentReactionRepository(
    private val reactionDao: ReelCommentReactionDao,
    private val pendingActionDao: PendingSocialActionDao
) {
    suspend fun setReaction(commentId: String, reaction: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext

        // 1. Persist locally immediately for UI
        reactionDao.setReaction(commentId, userId, reaction)

        // 2. Queue for remote sync
        pendingActionDao.insertAction(
            PendingSocialActionEntity(
                localActionId = UUID.randomUUID().toString(),
                actionType = "COMMENT_REACTION_UPSERT",
                targetId = commentId,
                userId = userId,
                payload = "$reaction:sync",
                isReel = true
            )
        )

        // 3. Trigger background sync
        com.example.data.repository.MessagesRepository.getInstance().scheduleSync()
    }

    suspend fun deleteReaction(commentId: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext

        // 1. Remove locally immediately
        reactionDao.deleteReaction(commentId, userId)

        // 2. Queue for remote sync
        pendingActionDao.insertAction(
            PendingSocialActionEntity(
                localActionId = UUID.randomUUID().toString(),
                actionType = "COMMENT_REACTION_DELETE",
                targetId = commentId,
                userId = userId,
                payload = "delete:sync",
                isReel = true
            )
        )

        // 3. Trigger background sync
        com.example.data.repository.MessagesRepository.getInstance().scheduleSync()
    }
}
