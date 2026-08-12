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

    /** Raw persistence primitive used for imperative events and by replaceDesiredState. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawAction(action: PendingSocialActionEntity)

    /**
     * Compatibility entry point for existing Repository call sites.
     * LIKE/UNLIKE and FAVORITE/UNFAVORITE are transparently promoted to
     * declarative state actions. SHARE/COMMENT/etc. remain ordinary events.
     */
    @Transaction
    suspend fun insertAction(action: PendingSocialActionEntity) {
        val declarative = action.actionFamily != null || action.actionType in setOf(
            "LIKE", "UNLIKE", "FAVORITE", "UNFAVORITE"
        )

        if (!declarative) {
            insertRawAction(action)
            return
        }

        val family = action.actionFamily ?: when (action.actionType) {
            "LIKE", "UNLIKE" -> "LIKE"
            "FAVORITE", "UNFAVORITE" -> "FAVORITE"
            else -> error("Unsupported declarative action type: ${action.actionType}")
        }
        val desiredState = action.desiredState ?: when (action.actionType) {
            "LIKE", "FAVORITE" -> true
            "UNLIKE", "UNFAVORITE" -> false
            else -> error("Unsupported declarative action type: ${action.actionType}")
        }

        replaceDesiredState(
            action.copy(
                actionFamily = family,
                desiredState = desiredState,
                actionType = if (family == "LIKE") {
                    if (desiredState) "LIKE" else "UNLIKE"
                } else {
                    if (desiredState) "FAVORITE" else "UNFAVORITE"
                }
            )
        )
    }

    /**
     * Atomically replaces the desired state for one logical interaction family.
     * Existing localActionId is preserved and revision is advanced.
     */
    @Transaction
    suspend fun replaceDesiredState(action: PendingSocialActionEntity) {
        val family = requireNotNull(action.actionFamily) {
            "replaceDesiredState requires a non-null actionFamily"
        }
        val desiredState = requireNotNull(action.desiredState) {
            "replaceDesiredState requires a non-null desiredState"
        }

        val normalizedActionType = if (family == "LIKE") {
            if (desiredState) "LIKE" else "UNLIKE"
        } else if (family == "FAVORITE") {
            if (desiredState) "FAVORITE" else "UNFAVORITE"
        } else {
            action.actionType
        }

        val updatedRows = updateActionFamilyState(
            userId = action.userId,
            targetId = action.targetId,
            isReel = action.isReel,
            actionFamily = family,
            desiredState = desiredState,
            actionType = normalizedActionType,
            createdAt = action.createdAt
        )

        if (updatedRows == 0) {
            insertRawAction(
                action.copy(
                    actionType = normalizedActionType,
                    status = "pending",
                    retryCount = 0,
                    revision = 1L
                )
            )
        }
    }

    @Query("""
        UPDATE pending_social_actions
        SET actionType = :actionType,
            desiredState = :desiredState,
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
        actionType: String,
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
     * on the same snapshot.
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

    // Legacy Repository calls are retained, but they must never delete the
    // new declarative rows. Declarative rows are collapsed by insertAction().
    @Query("DELETE FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionFamily IS NULL AND actionType IN ('LIKE', 'UNLIKE')")
    suspend fun deleteLikeActionsForTarget(userId: String, targetId: String)

    @Query("DELETE FROM pending_social_actions WHERE userId = :userId AND targetId = :targetId AND actionFamily IS NULL AND actionType IN ('FAVORITE', 'UNFAVORITE')")
    suspend fun deleteFavoriteActionsForTarget(userId: String, targetId: String)

    @Query("DELETE FROM pending_social_actions")
    suspend fun deleteAll()
}
