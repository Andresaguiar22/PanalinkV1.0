package com.example.media.sync

import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * P6.7.7A - Music Playlist Realtime Manager
 * Manages Supabase Realtime subscriptions for collaborative playlists.
 */
class MusicPlaylistRealtimeManager(
    private val syncManager: MusicSocialSyncManager
) {
    private val TAG = "MusicPlaylistRealtimeManager"
    private val managerScope = CoroutineScope(Dispatchers.IO + Job())
    private var observationJob: Job? = null

    /**
     * Starts observing realtime updates for a specific playlist.
     */
    fun startObserving(playlistId: String) {
        stopObserving() // Ensure no duplicate jobs
        
        observationJob = managerScope.launch {
            SupabaseClient.realtimeMusicUpdates.collectLatest { update ->
                // Filter by playlistId if possible in the record
                val recordPlaylistId = update.record.optString("playlist_id") ?: update.record.optString("id")
                
                if (recordPlaylistId == playlistId) {
                    when (update.table) {
                        "music_playlists" -> {
                            if (update.eventType == "DELETE") {
                                syncManager.handleRemotePlaylistDelete(update.record)
                            } else {
                                syncManager.handleRemotePlaylistUpdate(update.record)
                            }
                        }
                        "music_playlist_tracks" -> {
                            syncManager.handleRemoteTrackUpdate(update.record, update.eventType)
                        }
                        "music_playlist_collaborators" -> {
                            // Already handled via implicit sync or can be explicit
                        }
                        "music_playlist_invitations" -> {
                            syncManager.handleRemoteInvitationUpdate(update.record)
                        }
                    }
                }
            }
        }
        Log.i(TAG, "Started observing realtime updates for playlist: $playlistId")
    }

    /**
     * Stops observing updates.
     */
    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
        Log.i(TAG, "Stopped observing realtime updates")
    }
}
