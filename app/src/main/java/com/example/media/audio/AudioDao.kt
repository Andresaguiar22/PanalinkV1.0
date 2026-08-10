package com.example.media.audio

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * P6.7 - Audio DAO
 * Room database access object for audio tracks.
 */
@Dao
interface AudioDao {

    @Query("SELECT * FROM audio_tracks ORDER BY createdAt DESC")
    fun getAllAudioTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): AudioTrackEntity?

    @Query("SELECT * FROM audio_tracks WHERE fileHash = :hash LIMIT 1")
    suspend fun getTrackByHash(hash: String): AudioTrackEntity?

    @Query("SELECT * FROM audio_tracks WHERE playlistId = :playlistId ORDER BY createdAt ASC")
    fun getTracksByPlaylist(playlistId: String): Flow<List<AudioTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AudioTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)

    @Update
    suspend fun updateTrack(track: AudioTrackEntity)

    @Delete
    suspend fun deleteTrack(track: AudioTrackEntity)

    @Query("UPDATE audio_tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteState(trackId: String, isFavorite: Boolean)
}

typealias AudioTrackDao = AudioDao
