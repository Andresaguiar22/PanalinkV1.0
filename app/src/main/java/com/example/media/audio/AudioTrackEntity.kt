package com.example.media.audio

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P6.7 - Audio Track Entity
 * Local offline-first model for audio tracks (music, voice notes, chat audios, imports).
 */
@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val artist: String = "Desconocido",
    val album: String = "Sencillo",
    val coverPath: String? = null,
    val durationMs: Long = 0L,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val playlistId: String? = null,
    val isFavorite: Boolean = false,
    val genre: String = "Desconocido",
    val trackType: String = "MUSIC", // "MUSIC", "VOICE_NOTE", "CHAT_AUDIO", "IMPORTED"
    val fileHash: String? = null,
    val remoteId: String? = null,
    val lastSyncAt: Long? = null,
    val isDirty: Boolean = false
) {
    val coverUri: String? get() = coverPath
}
