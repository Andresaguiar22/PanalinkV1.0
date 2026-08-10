package com.example.data.database

import androidx.room.*

@Dao
interface PendingSocialActionDao {
    @Query("SELECT * FROM pending_social_actions WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<PendingSocialActionEntity>

    @Query("SELECT * FROM pending_social_actions WHERE localActionId = :id")
    suspend fun getActionById(id: String): PendingSocialActionEntity?

    @Query("SELECT * FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionType = :actionType")
    suspend fun getExistingAction(userId: String, targetId: String, actionType: String): PendingSocialActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingSocialActionEntity)

    @Query("UPDATE pending_social_actions SET retryCount = retryCount + 1, status = :status WHERE localActionId = :id")
    suspend fun updateActionStatus(id: String, status: String)

    @Query("DELETE FROM pending_social_actions WHERE localActionId = :id")
    suspend fun deleteActionById(id: String)

    @Query("DELETE FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionType IN ('LIKE', 'UNLIKE')")
    suspend fun deleteLikeActionsForTarget(userId: String, targetId: String)

    @Query("DELETE FROM pending_social_actions")
    suspend fun deleteAll()
}
