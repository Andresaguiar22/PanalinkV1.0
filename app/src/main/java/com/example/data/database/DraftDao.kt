package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE draftId = :draftId AND userId = :userId")
    suspend fun getDraft(draftId: String, userId: String): DraftEntity?

    @Query("SELECT * FROM drafts WHERE userId = :userId AND type = :type")
    fun getDraftsByType(userId: String, type: String): Flow<List<DraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE draftId = :draftId AND userId = :userId")
    suspend fun deleteDraft(draftId: String, userId: String)

    @Query("DELETE FROM drafts WHERE updatedAt < :timestamp")
    suspend fun clearOldDrafts(timestamp: Long)
}
