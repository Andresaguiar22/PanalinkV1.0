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
    // Declarative state actions use a non-null family/state; event actions keep both null.
    val actionFamily: String? = null,
    val desiredState: Boolean? = null
)
