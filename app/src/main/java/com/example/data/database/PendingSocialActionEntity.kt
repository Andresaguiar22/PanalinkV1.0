package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_social_actions",
    indices = [
        Index(
            value = ["userId", "targetId", "isReel", "actionFamily"],
            unique = true
        )
    ]
)
data class PendingSocialActionEntity(
    @PrimaryKey val localActionId: String,
    val userId: String,
    val targetId: String,
    val actionType: String, // "LIKE", "UNLIKE", "COMMENT", "DELETE_COMMENT"
    val payload: String?,   // e.g. JSON or comment text
    val isReel: Boolean,    // true if Reel/Story, false if Post
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "pending", // "pending", "failed"
    // Declarative state family. Null for imperative/event actions.
    val actionFamily: String? = null,
    // Desired state for declarative actions. Null for imperative/event actions.
    val desiredState: Boolean? = null,
    // Local optimistic-concurrency version. Event rows remain at 0.
    val revision: Long = 0L
)
