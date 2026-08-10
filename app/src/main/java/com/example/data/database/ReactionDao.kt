package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {
    @Query("SELECT * FROM message_reactions WHERE thread_message_id = :messageId")
    fun getReactionsForMessage(messageId: String): Flow<List<ReactionEntity>>

    @Query("SELECT * FROM message_reactions WHERE thread_message_id = :messageId AND user_id = :userId LIMIT 1")
    suspend fun getReaction(messageId: String, userId: String): ReactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: ReactionEntity)

    @Query("DELETE FROM message_reactions WHERE thread_message_id = :messageId AND user_id = :userId")
    suspend fun deleteReaction(messageId: String, userId: String)

    @Query("DELETE FROM message_reactions WHERE thread_message_id = :messageId")
    suspend fun deleteAllReactionsForMessage(messageId: String)
}
