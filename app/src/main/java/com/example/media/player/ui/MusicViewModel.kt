package com.example.media.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.media.audio.AudioLibraryManager
import com.example.media.audio.AudioTrackEntity
import com.example.media.playlist.PlaylistEntity
import com.example.media.playlist.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * P6.7.3 - Music View Model
 * Manages the data for the Music Home Screen (playlists, recent tracks, favorites).
 */
class MusicViewModel(
    private val audioLibraryManager: AudioLibraryManager,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<AudioTrackEntity>>(emptyList())
    val recentTracks: StateFlow<List<AudioTrackEntity>> = _recentTracks.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<List<AudioTrackEntity>>(emptyList())
    val favoriteTracks: StateFlow<List<AudioTrackEntity>> = _favoriteTracks.asStateFlow()

    private val _albums = MutableStateFlow<List<String>>(emptyList())
    val albums: StateFlow<List<String>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<String>>(emptyList())
    val artists: StateFlow<List<String>> = _artists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredTracks = MutableStateFlow<List<AudioTrackEntity>>(emptyList())
    val filteredTracks: StateFlow<List<AudioTrackEntity>> = _filteredTracks.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { _playlists.value = it }
        }
        viewModelScope.launch {
            audioLibraryManager.allTracks.collect { tracks ->
                _recentTracks.value = tracks.take(10)
                _favoriteTracks.value = tracks.filter { it.isFavorite }
                _albums.value = tracks.map { it.album }.distinct()
                _artists.value = tracks.map { it.artist }.distinct()
                updateFilteredTracks(tracks, _searchQuery.value)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            audioLibraryManager.allTracks.first().let { tracks ->
                updateFilteredTracks(tracks, query)
            }
        }
    }

    private fun updateFilteredTracks(tracks: List<AudioTrackEntity>, query: String) {
        _filteredTracks.value = if (query.isBlank()) {
            emptyList()
        } else {
            tracks.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) || 
                it.album.contains(query, ignoreCase = true)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, "Nueva playlist de PanaLink")
            loadData()
        }
    }
}
