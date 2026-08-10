package com.example.media.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_assets")
data class MediaAssetEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val type: String,
    val remoteUrl: String?,
    val localPath: String?,
    val thumbnailPath: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val createdAt: Long,
    val lastSyncedAt: Long,
    val syncState: String
)
