package com.example.media.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.media.audio.AudioTrackEntity
import com.example.media.playlist.PlaylistManager
import com.example.media.playlist.PlaylistRepository
import com.example.media.playlist.PlaylistState
import com.example.media.playlist.PlaylistMemberRole
import com.example.media.sync.MusicPlaylistRealtimeManager
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * P6.7.5 - Playlist ViewModel
 * Manages state and logic for the Playlist Detail Screen.
 */
class PlaylistViewModel(
    private val playlistId: String,
    private val playlistManager: PlaylistManager,
    private val playlistRepository: PlaylistRepository,
    private val invitationRepository: com.example.media.playlist.PlaylistInvitationRepository,
    private val realtimeManager: MusicPlaylistRealtimeManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistState(isLoading = true))
    val uiState: StateFlow<PlaylistState> = _uiState.asStateFlow()

    init {
        loadPlaylistData()
        realtimeManager?.startObserving(playlistId)
    }

    override fun onCleared() {
        super.onCleared()
        realtimeManager?.stopObserving()
    }

    private fun loadPlaylistData() {
        viewModelScope.launch {
            val p = playlistRepository.getPlaylistById(playlistId)
            if (p == null) {
                _uiState.update { it.copy(isLoading = false, error = "Playlist no encontrada") }
                return@launch
            }

            val currentUserId = SessionManager.getCurrentUserId() ?: ""
            
            // Observe tracks
            val tracksJob = launch {
                playlistRepository.getTracksForPlaylist(playlistId)
                    .collect { trackList ->
                        _uiState.update { 
                            it.copy(
                                playlist = p,
                                tracks = trackList,
                                totalDurationMs = trackList.sumOf { t -> t.durationMs }
                            )
                        }
                    }
            }

            // Observe collaborators
            val collaboratorsJob = launch {
                playlistRepository.getCollaboratorsForPlaylist(playlistId)
                    .collect { collabs ->
                        val roleStr = collabs.find { it.userId == currentUserId }?.role ?: "VIEWER"
                        val role = if (p.ownerId == currentUserId) PlaylistMemberRole.OWNER else {
                            when(roleStr) {
                                "EDITOR" -> PlaylistMemberRole.EDITOR
                                "VIEWER" -> PlaylistMemberRole.VIEWER
                                else -> PlaylistMemberRole.VIEWER
                            }
                        }
                        _uiState.update { it.copy(collaborators = collabs, userRole = role) }
                    }
            }

            // Observe invitations
            val invitationsJob = launch {
                invitationRepository.observeInvitationsForPlaylist(playlistId)
                    .collect { invites ->
                        _uiState.update { it.copy(invitations = invites) }
                    }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // P6.7.9B - Invitation Actions
    fun inviteCollaborator(receiverId: String, role: String) {
        viewModelScope.launch {
            val currentUserId = SessionManager.getCurrentUserId() ?: return@launch
            invitationRepository.createInvitationLocally(
                playlistId = playlistId,
                senderId = currentUserId,
                receiverId = receiverId,
                role = role
            )
        }
    }

    fun revokeInvitation(invitationId: String) {
        viewModelScope.launch {
            val authHeader = "Bearer ${SessionManager.getUserAuthToken()}"
            invitationRepository.revokeInvitation(invitationId, authHeader)
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            val authHeader = "Bearer ${SessionManager.getUserAuthToken()}"
            invitationRepository.acceptInvitation(invitationId, authHeader)
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            val authHeader = "Bearer ${SessionManager.getUserAuthToken()}"
            invitationRepository.rejectInvitation(invitationId, authHeader)
        }
    }

    fun removeCollaborator(collaboratorId: String) {
        viewModelScope.launch {
            // Implementation depends on Repository, adding it here for Phase 2 completeness
            playlistRepository.removeCollaborator(collaboratorId)
        }
    }

    fun updateCollaboratorRole(collaboratorId: String, newRole: String) {
        viewModelScope.launch {
            playlistRepository.updateCollaboratorRole(collaboratorId, newRole)
        }
    }

    fun playAll(playerViewModel: PlayerViewModel) {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playerViewModel.playTracks(tracks, 0)
        }
    }

    fun shuffleAndPlay(playerViewModel: PlayerViewModel) {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
            playerViewModel.playTracks(shuffled, 0)
            playerViewModel.toggleShuffle()
        }
    }

    fun playTrack(track: AudioTrackEntity, playerViewModel: PlayerViewModel) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOf(track)
        if (index != -1) {
            playerViewModel.playTracks(tracks, index)
        } else {
            playerViewModel.playTrack(track)
        }
    }

    fun removeTrack(track: AudioTrackEntity) {
        viewModelScope.launch {
            playlistManager.removeTrackFromPlaylist(playlistId, track.id)
            // La recarga ocurre automáticamente si el flow de tracks es reactivo al DB
        }
    }
}
