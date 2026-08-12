package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Durable offline-first social operation.
 *
 * targetType is the explicit domain discriminator used by the sync worker.
 * UNKNOWN is retained only for backwards compatibility with pre-v43 rows;
 * those rows are reconciled during migration and the worker has a legacy fallback.
 */
@Entity(tableName = "pending_social_actions")
data class PendingSocialActionEntity(
    @PrimaryKey val localActionId: String,
    val userId: String,
    val targetId: String,
    val actionType: String, // LIKE, UNLIKE, FAVORITE, UNFAVORITE, COMMENT, DELETE_COMMENT, SHARE
    val payload: String?,
    val isReel: Boolean, // legacy compatibility; targetType is authoritative for new rows
    val targetType: String = "UNKNOWN", // POST | REEL | STORY | UNKNOWN
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "pending" // pending, failed
)
