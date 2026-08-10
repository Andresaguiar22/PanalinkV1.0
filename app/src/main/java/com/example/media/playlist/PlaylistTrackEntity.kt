package com.example.media.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_songs")
data class PlaylistTrackEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val trackId: String,
    @androidx.room.ColumnInfo(name = "orderIndex", defaultValue = "0") val position: Int,
    val addedAt: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(defaultValue = "0") val isDirty: Boolean? = false
)
