package com.example.media.sync

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.example.data.supabase.SupabaseApiService
import com.example.media.audio.AudioRepository
import com.example.media.playlist.PlaylistEntity
import com.example.media.playlist.PlaylistRepository
import com.example.media.playlist.PlaylistInvitationRepository
import com.example.media.playlist.PlaylistTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * P6.7.6B - Music Social Sync Manager
 * Synchronizes music metadata (playlists, tracks, collaborators, invitations) between Room and Supabase.
 */
class MusicSocialSyncManager(
    private val context: Context,
    private val supabaseApi: SupabaseApiService,
    private val playlistRepo: PlaylistRepository,
    private val invitationRepo: PlaylistInvitationRepository,
    private val audioRepo: AudioRepository,
    private val apiKey: String
) {
    private val TAG = "MusicSocialSyncManager"

    /**
     * Handles a Realtime update for a playlist.
     */
    suspend fun handleRemotePlaylistUpdate(record: org.json.JSONObject) = withContext(Dispatchers.IO) {
        try {
            val remoteId = record.optString("id") ?: return@withContext
            val local = playlistRepo.getPlaylistById(remoteId) ?: return@withContext
            
            if (local.isDirty) {
                // Conflict resolution: LWW
                val remoteUpdatedAt = parseIsoTimestamp(record.optString("updated_at"))
                if (remoteUpdatedAt <= local.updatedAt) return@withContext
            }

            val updatedLocal = local.copy(
                name = record.optString("title", local.name),
                description = record.optString("description", local.description),
                coverPath = record.optString("cover_cdn_url", local.coverPath),
                isCollaborative = record.optBoolean("is_collaborative", local.isCollaborative),
                updatedAt = parseIsoTimestamp(record.optString("updated_at")),
                lastSyncAt = System.currentTimeMillis(),
                isDirty = false
            )
            // P6.7.8 Audit: Disable automatic timestamp update to keep remote precision
            playlistRepo.updatePlaylist(updatedLocal, updateTimestamp = false)
            Log.d(TAG, "Realtime update applied to playlist: ${updatedLocal.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote playlist update", e)
        }
    }

    /**
     * Handles a Realtime deletion for a playlist.
     */
    suspend fun handleRemotePlaylistDelete(record: org.json.JSONObject) = withContext(Dispatchers.IO) {
        try {
            val remoteId = record.optString("id") ?: return@withContext
            val local = playlistRepo.getPlaylistById(remoteId) ?: return@withContext
            playlistRepo.deletePlaylist(local)
            Log.d(TAG, "Realtime deletion applied to playlist: ${local.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote playlist deletion", e)
        }
    }

    /**
     * Handles a Realtime update for a playlist track.
     */
    suspend fun handleRemoteTrackUpdate(record: org.json.JSONObject, eventType: String) = withContext(Dispatchers.IO) {
        try {
            val playlistId = record.optString("playlist_id") ?: return@withContext
            val mediaHash = record.optString("media_hash") ?: return@withContext

            if (eventType == "DELETE") {
                val existingAudio = audioRepo.getTrackByHash(mediaHash)
                if (existingAudio != null) {
                    playlistRepo.removeTrackFromPlaylist(playlistId, existingAudio.id)
                    Log.d(TAG, "Realtime track removed from playlist: $playlistId")
                }
                return@withContext
            }

            // UPSERT Track
            val existingAudio = audioRepo.getTrackByHash(mediaHash)
            val trackId = existingAudio?.id ?: UUID.randomUUID().toString()
            
            if (existingAudio == null) {
                val newAudio = com.example.media.audio.AudioTrackEntity(
                    id = trackId,
                    userId = record.optString("owner_id", ""), // Or current user?
                    title = record.optString("title", "Unknown"),
                    artist = record.optString("artist", "Unknown"),
                    album = record.optString("album", "Sencillo"),
                    coverPath = record.optString("cover_cdn_url", ""),
                    durationMs = record.optLong("duration_ms", 0L),
                    filePath = record.optString("cdn_url", ""),
                    fileHash = mediaHash,
                    remoteId = record.optString("id"),
                    lastSyncAt = System.currentTimeMillis()
                )
                audioRepo.saveTrack(newAudio)
            }
            
            val orderIndex = record.optInt("order_index", 0)
            playlistRepo.upsertTrackWithPosition(playlistId, trackId, orderIndex)
            Log.d(TAG, "Realtime track updated/added to playlist: $playlistId at pos $orderIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote track update", e)
        }
    }

    suspend fun syncFull(userId: String, authToken: String) = withContext(Dispatchers.IO) {
        try {
            syncPlaylistsFromRemote(userId, authToken)
            syncUnsyncedPlaylists(userId, authToken)
            
            // P6.7.9 Phase 2: Sync Invitations
            invitationRepo.syncRemoteToLocal(userId, "Bearer $authToken")
            invitationRepo.syncLocalToRemote("Bearer $authToken")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
        }
    }

    /**
     * Handles a Realtime update for a playlist invitation.
     */
    suspend fun handleRemoteInvitationUpdate(record: org.json.JSONObject) = withContext(Dispatchers.IO) {
        try {
            val remoteId = record.optString("id") ?: return@withContext
            val remoteStatus = record.optString("status") ?: return@withContext
            
            val local = invitationRepo.getInvitationById(remoteId)
            if (local == null) {
                // New invitation from remote (usually for receiver)
                val response = supabaseApi.getMusicPlaylistInvitations(
                    apiKey, 
                    "Bearer ${getUserAuthToken()}", // Assuming we have a way to get it
                    select = "*"
                )
                // It's better to fetch properly via API to get all fields
                invitationRepo.syncRemoteToLocal(record.optString("receiver_id"), "Bearer ${getUserAuthToken()}")
            } else {
                // Update local status
                invitationRepo.upsertInvitation(local.copy(
                    status = remoteStatus,
                    updatedAt = parseIsoTimestamp(record.optString("updated_at")),
                    isDirty = false
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote invitation update", e)
        }
    }

    private fun getUserAuthToken(): String {
        // This is a placeholder. In a real app, we'd get it from a SessionManager.
        // For this app, it might be stored in SharedPreferences or passed down.
        return "" 
    }

    /**
     * Pulls playlists from Supabase and updates local Room database.
     */
    private suspend fun syncPlaylistsFromRemote(userId: String, authToken: String) {
        val response = supabaseApi.getMusicPlaylists(
            apiKey = apiKey,
            authorization = "Bearer $authToken"
        )

        if (response.isSuccessful) {
            val remotePlaylists = response.body() ?: emptyList()
            remotePlaylists.forEach { remote ->
                val local = playlistRepo.getPlaylistById(remote.id ?: return@forEach)
                
                if (local == null) {
                    // New playlist from remote
                    val newLocal = MusicSocialMapper.toLocalEntity(remote)
                    playlistRepo.createPlaylist(newLocal)
                    syncTracksForPlaylist(remote.id ?: "", userId, authToken)
                } else if (local.isDirty) {
                    // Conflict Resolution: Last-Write-Wins
                    val remoteUpdatedAt = parseIsoTimestamp(remote.updated_at)
                    if (remoteUpdatedAt > local.updatedAt) {
                        // Remote is newer, overwrite local
                        val updatedLocal = local.copy(
                            name = remote.title,
                            description = remote.description,
                            coverPath = remote.cover_cdn_url,
                            lastSyncAt = System.currentTimeMillis(),
                            updatedAt = remoteUpdatedAt,
                            isDirty = false
                        )
                        // P6.7.8 Audit: Disable automatic timestamp update
                        playlistRepo.updatePlaylist(updatedLocal, updateTimestamp = false)
                        syncTracksForPlaylist(remote.id, userId, authToken)
                    }
                    // Else: local is newer or same, keep local (it will be pushed later)
                } else {
                    // Update local if not dirty
                    val updatedLocal = local.copy(
                        name = remote.title,
                        description = remote.description,
                        coverPath = remote.cover_cdn_url,
                        lastSyncAt = System.currentTimeMillis(),
                        updatedAt = parseIsoTimestamp(remote.updated_at)
                    )
                    playlistRepo.updatePlaylist(updatedLocal, updateTimestamp = false)
                    syncTracksForPlaylist(remote.id, userId, authToken)
                }
            }
        }
    }

    private fun parseIsoTimestamp(iso: String?): Long {
        if (iso == null) return 0L
        return try {
            // P6.7.8 Audit: Support Supabase ISO with optional milliseconds and Z
            val cleanedIso = iso.replace("Z", "+0000")
            val pattern = if (cleanedIso.contains(".")) {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ" // Handle high precision
            } else {
                "yyyy-MM-dd'T'HH:mm:ssZ"
            }
            val format = java.text.SimpleDateFormat(pattern, Locale.US)
            format.parse(cleanedIso)?.time ?: 0L
        } catch (e: Exception) {
            // Fallback for different precision
            try {
                val fallback = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                fallback.parse(iso?.replace("Z", "+0000") ?: "")?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    /**
     * Syncs tracks for a specific playlist from remote to local.
     */
    private suspend fun syncTracksForPlaylist(playlistId: String, userId: String, authToken: String) {
        val response = supabaseApi.getMusicPlaylistTracks(
            apiKey = apiKey,
            authorization = "Bearer $authToken",
            playlistId = playlistId
        )

        if (response.isSuccessful) {
            val remoteTracks = response.body() ?: emptyList()
            remoteTracks.forEach { remote ->
                // 1. Check if track exists in audio_tracks by media_hash (Deduplication)
                val existingAudio = audioRepo.getTrackByHash(remote.media_hash)
                
                val trackId = existingAudio?.id ?: UUID.randomUUID().toString()
                
                if (existingAudio == null) {
                    // Create minimal local audio entry (will be downloaded by MediaSyncManager if played)
                    val newAudio = com.example.media.audio.AudioTrackEntity(
                        id = trackId,
                        userId = userId, // Using current userId
                        title = remote.title,
                        artist = remote.artist,
                        album = remote.album ?: "Sencillo",
                        coverPath = remote.cover_cdn_url,
                        durationMs = remote.duration_ms,
                        filePath = remote.cdn_url, // URL as temporary path until downloaded
                        fileHash = remote.media_hash,
                        remoteId = remote.id,
                        lastSyncAt = System.currentTimeMillis()
                    )
                    audioRepo.saveTrack(newAudio)
                }

                // 2. Ensure relationship in playlist_songs (P6.7.8 Audit: Use upsert to prevent duplicates)
                playlistRepo.upsertTrackWithPosition(playlistId, trackId, remote.order_index)
            }
        }
    }

    /**
     * Pushes local changes to Supabase.
     */
    private suspend fun syncUnsyncedPlaylists(userId: String, authToken: String) {
        val unsynced = playlistRepo.getUnsyncedPlaylistsSync()
        unsynced.forEach { local ->
            val remoteDto = MusicSocialMapper.toRemoteDto(local)

            val response = supabaseApi.upsertMusicPlaylist(
                apiKey = apiKey,
                authorization = "Bearer $authToken",
                playlist = remoteDto
            )

            if (response.isSuccessful) {
                val syncedRemote = response.body()?.firstOrNull()
                if (syncedRemote != null) {
                    playlistRepo.updatePlaylist(local.copy(
                        remoteId = syncedRemote.id,
                        lastSyncAt = System.currentTimeMillis(),
                        isDirty = false
                    ))
                    
                    // Sync tracks for this playlist too
                    pushPlaylistTracks(local.id, syncedRemote.id ?: "", authToken)
                }
            }
        }
    }

    private suspend fun pushPlaylistTracks(localPlaylistId: String, remotePlaylistId: String, authToken: String) {
        val tracks = playlistRepo.getTracksForPlaylistSync(localPlaylistId)
        val remoteTracks = tracks.mapIndexed { index, audio ->
            RemoteMusicPlaylistTrack(
                playlist_id = remotePlaylistId,
                media_hash = audio.fileHash ?: "",
                cdn_url = audio.filePath,
                title = audio.title,
                artist = audio.artist,
                album = audio.album,
                cover_cdn_url = audio.coverPath,
                duration_ms = audio.durationMs,
                mime_type = "audio/mpeg", // Default or detect
                order_index = index
            )
        }

        if (remoteTracks.isNotEmpty()) {
            supabaseApi.upsertMusicPlaylistTracks(
                apiKey = apiKey,
                authorization = "Bearer $authToken",
                tracks = remoteTracks
            )
        }
    }
}
