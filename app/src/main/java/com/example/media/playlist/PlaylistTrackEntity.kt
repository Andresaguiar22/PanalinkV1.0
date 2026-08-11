package com.example.media.playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_songs",
    indices = [
        Index("playlistId"),
        Index("trackId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = com.example.media.audio.AudioTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistTrackEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val trackId: String,
    @ColumnInfo(name = "orderIndex", defaultValue = "0") val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val isDirty: Boolean = false
)
