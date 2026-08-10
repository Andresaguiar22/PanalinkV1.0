package com.example.media.playlist

import androidx.room.*
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * P6.7.2 - Playlist DAO
 * Operations for playlists and their track associations.
 */
@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE ownerId = :userId ORDER BY updatedAt DESC")
    fun getPlaylistsByUser(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE name LIKE '%' || :query || '%' AND (isPublic = 1 OR ownerId = :userId)")
    fun searchPlaylists(query: String, userId: String): Flow<List<PlaylistEntity>>

    // Track associations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(playlistTracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Transaction
    @Query("""
        SELECT audio_tracks.* FROM audio_tracks 
        INNER JOIN playlist_songs ON audio_tracks.id = playlist_songs.trackId 
        WHERE playlist_songs.playlistId = :playlistId 
        ORDER BY playlist_songs.orderIndex ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<AudioTrackEntity>>

    @Transaction
    @Query("""
        SELECT audio_tracks.* FROM audio_tracks 
        INNER JOIN playlist_songs ON audio_tracks.id = playlist_songs.trackId 
        WHERE playlist_songs.playlistId = :playlistId 
        ORDER BY playlist_songs.orderIndex ASC
    """)
    suspend fun getTracksForPlaylistSync(playlistId: String): List<AudioTrackEntity>

    @Query("SELECT MAX(orderIndex) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Query("UPDATE playlist_songs SET orderIndex = :newPosition WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackPosition(playlistId: String, trackId: String, newPosition: Int)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId AND trackId = :trackId LIMIT 1")
    suspend fun getPlaylistTrackRelation(playlistId: String, trackId: String): PlaylistTrackEntity?

    @Query("SELECT * FROM playlists WHERE isDirty = 1")
    suspend fun getUnsyncedPlaylists(): List<PlaylistEntity>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getTrackCount(playlistId: String): Int

    @Query("SELECT * FROM playlist_songs WHERE isDirty = 1")
    suspend fun getUnsyncedTracks(): List<PlaylistTrackEntity>
}
