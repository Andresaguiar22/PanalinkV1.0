package com.example.panatv

import android.app.Application
import android.util.Log
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PanaTVViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PanaTVRepository(application)
    private val prefs = application.getSharedPreferences("panatv_prefs", Context.MODE_PRIVATE)

    private val _channels = MutableStateFlow<List<PanaTVChannelEntity>>(emptyList())
    val channels: StateFlow<List<PanaTVChannelEntity>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentChannel = MutableStateFlow<PanaTVChannelEntity?>(null)
    val currentChannel: StateFlow<PanaTVChannelEntity?> = _currentChannel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCountry = MutableStateFlow("")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()
    
    val availableCountries = MutableStateFlow<List<String>>(emptyList())

    private val _debugMessage = MutableStateFlow("")
    val debugMessage: StateFlow<String> = _debugMessage.asStateFlow()

    private val _crashTrace = MutableStateFlow(prefs.getString("last_crash_trace", "") ?: "")
    val crashTrace: StateFlow<String> = _crashTrace.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()
    
    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val trace = throwable.stackTraceToString()
        Log.e("PanaTVViewModel", "CRASH DETECTED:\n$trace")
        prefs.edit().putString("last_crash_trace", trace).apply()
        _crashTrace.value = trace
    }

    fun clearCrashTrace() {
        prefs.edit().remove("last_crash_trace").apply()
        _crashTrace.value = ""
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            repository.fetchChannelsIfNeeded { message ->
                _debugMessage.value = message
            }
        }
        
        viewModelScope.launch {
            repository.getFavorites().collectLatest { favList ->
                _favorites.value = favList.map { it.id }.toSet()
            }
        }
        
        viewModelScope.launch {
            repository.getChannels("", "").collectLatest { allChannels ->
                availableCountries.value = allChannels.map { it.country }.filter { it.isNotBlank() }.distinct().sorted()
            }
        }
        
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_searchQuery, _selectedCountry, _showOnlyFavorites) { q, c, f ->
                Triple(q, c, f)
            }.collectLatest { (query, country, showFavs) ->
                updateChannelList(query, country, showFavs)
            }
        }
    }

    private suspend fun updateChannelList(query: String, country: String, showFavs: Boolean) {
        repository.getChannels(query, country).collectLatest { dbChannels ->
            val filtered = if (showFavs) {
                val currentFavs = _favorites.value
                dbChannels.filter { currentFavs.contains(it.id) }
            } else {
                dbChannels
            }
            _channels.value = filtered
            if (filtered.isNotEmpty() && _currentChannel.value == null) {
                _currentChannel.value = filtered.first()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCountry(country: String) {
        _selectedCountry.value = country
    }
    
    fun toggleShowFavorites() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }
    
    fun toggleFavorite(channelId: String) {
        viewModelScope.launch(exceptionHandler + kotlinx.coroutines.Dispatchers.IO) {
            if (_favorites.value.contains(channelId)) {
                repository.removeFavorite(channelId)
            } else {
                repository.addFavorite(channelId)
            }
        }
    }

    fun selectChannel(channel: PanaTVChannelEntity) {
        _currentChannel.value = channel
    }
}
