package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ReelsPlayerManager {
    private val _isFloatingEnabled = MutableStateFlow(true)
    val isFloatingEnabled: StateFlow<Boolean> = _isFloatingEnabled.asStateFlow()

    private val _activeVideoUrl = MutableStateFlow<String?>(null)
    val activeVideoUrl: StateFlow<String?> = _activeVideoUrl.asStateFlow()

    private val _currentPlaybackPosition = MutableStateFlow(0L)
    val currentPlaybackPosition: StateFlow<Long> = _currentPlaybackPosition.asStateFlow()

    private val _isFloatingActive = MutableStateFlow(false)
    val isFloatingActive: StateFlow<Boolean> = _isFloatingActive.asStateFlow()

    fun setFloatingEnabled(enabled: Boolean) {
        _isFloatingEnabled.value = enabled
        if (!enabled) {
            closeAndClear()
        }
    }

    fun minimizeToFloating(url: String, position: Long = 0L) {
        if (!_isFloatingEnabled.value) return
        _activeVideoUrl.value = url
        _currentPlaybackPosition.value = position
        _isFloatingActive.value = true
    }

    fun hideFloatingPlayer() {
        _isFloatingActive.value = false
    }

    fun closeAndClear() {
        _isFloatingActive.value = false
        _activeVideoUrl.value = null
        _currentPlaybackPosition.value = 0L
    }

    // Call when voice notes, audio stories, or external videos start playing
    fun pauseForOtherAudio() {
        if (_isFloatingActive.value) {
            _isFloatingActive.value = false
        }
    }
}
