package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_posts")
data class PendingPostEntity(
    @PrimaryKey val id: String, // UUID
    val userId: String,
    val content: String?,
    val type: String, // "TEXT", "ALBUM", "AUDIO"
    val mediaUrisJson: String, // JSON array of local URIs
    val privacy: String,
    val status: String, // "pending", "uploading", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val progress: Float = 0f, // 0.0 to 1.0
    val previewDataJson: String? = null
)
