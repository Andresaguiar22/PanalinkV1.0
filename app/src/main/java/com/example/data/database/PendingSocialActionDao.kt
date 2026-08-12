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

    /**
     * Atomically replaces the desired state for one logical interaction family.
     * The row identity is preserved when the family already exists, while the
     * revision changes so an in-flight Worker snapshot becomes stale.
     */
    @Transaction
    suspend fun replaceDesiredState(action: PendingSocialActionEntity) {
        val family = requireNotNull(action.actionFamily) {
            "replaceDesiredState requires a non-null actionFamily"
        }
        val desiredState = requireNotNull(action.desiredState) {
            "replaceDesiredState requires a non-null desiredState"
        }

        val updatedRows = updateActionFamilyState(
            userId = action.userId,
            targetId = action.targetId,
            isReel = action.isReel,
            actionFamily = family,
            desiredState = desiredState,
            createdAt = action.createdAt
        )

        if (updatedRows == 0) {
            insertAction(
                action.copy(
                    status = "pending",
                    retryCount = 0,
                    revision = 1L
                )
            )
        }
    }

    @Query("""
        UPDATE pending_social_actions
        SET desiredState = :desiredState,
            createdAt = :createdAt,
            retryCount = 0,
            status = 'pending',
            revision = revision + 1
        WHERE userId = :userId
          AND targetId = :targetId
          AND isReel = :isReel
          AND actionFamily = :actionFamily
    """)
    suspend fun updateActionFamilyState(
        userId: String,
        targetId: String,
        isReel: Boolean,
        actionFamily: String,
        desiredState: Boolean,
        createdAt: Long
    ): Int

    /**
     * Deletes a declarative action only if the exact Worker snapshot is still
     * current. This protects even A -> B -> A transitions from stale deletes.
     */
    @Query("""
        DELETE FROM pending_social_actions
        WHERE localActionId = :id
          AND actionFamily = :family
          AND desiredState = :desiredState
          AND revision = :revision
    """)
    suspend fun deleteIfStillCurrent(
        id: String,
        family: String,
        desiredState: Boolean,
        revision: Long
    ): Int

    /**
     * Marks a declarative action pending only when the Worker is still operating
     * on the same snapshot. A stale RPC cannot mutate a newer user intent.
     */
    @Query("""
        UPDATE pending_social_actions
        SET retryCount = retryCount + 1,
            status = :status
        WHERE localActionId = :id
          AND actionFamily = :family
          AND desiredState = :desiredState
          AND revision = :revision
    """)
    suspend fun updateStatusIfStillCurrent(
        id: String,
        family: String,
        desiredState: Boolean,
        revision: Long,
        status: String
    ): Int

    @Query("UPDATE pending_social_actions SET retryCount = retryCount + 1, status = :status WHERE localActionId = :id")
    suspend fun updateActionStatus(id: String, status: String)

    @Query("DELETE FROM pending_social_actions WHERE localActionId = :id")
    suspend fun deleteActionById(id: String)

    @Query("DELETE FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionType IN ('LIKE', 'UNLIKE')")
    suspend fun deleteLikeActionsForTarget(userId: String, targetId: String)

    @Query("DELETE FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionType IN ('FAVORITE', 'UNFAVORITE')")
    suspend fun deleteFavoriteActionsForTarget(userId: String, targetId: String)

    @Query("DELETE FROM pending_social_actions")
    suspend fun deleteAll()
}
