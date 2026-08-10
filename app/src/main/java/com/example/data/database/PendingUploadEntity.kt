package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUploadEntity(
    @PrimaryKey val id: String, // UUID generated when task is queued
    val userId: String,
    val uploadType: String, // "STATE", "REEL", "PROFILE"
    val localFilePath: String,
    val thumbnailPath: String? = null,
    val mimeType: String,
    val caption: String? = null,
    val metadataJson: String? = null,
    val status: String, // "pending", "uploading", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val remoteUrl: String? = null
)
