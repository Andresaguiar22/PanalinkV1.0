package com.example.media.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P6.7.2 - Playlist Entity
 * Metadata for user-created playlists.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true,
    val isCollaborative: Boolean = false,
    val remoteId: String? = null,
    val lastSyncAt: Long? = null,
    val isDirty: Boolean = false
)
