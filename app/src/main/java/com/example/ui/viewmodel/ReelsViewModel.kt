package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.PanalinkDatabase
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.data.repository.reels.ReelsLocalDataSource
import com.example.data.repository.reels.ReelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections

sealed class ReelsUiState {
    data object Loading : ReelsUiState()
    data class Success(val reels: List<UserStateWithUser>) : ReelsUiState()
    data class Error(val message: String) : ReelsUiState()
}

/** Feature-owned ViewModel for the Reels feed. */
class ReelsViewModel(
    private val repository: ReelsRepository = ReelsRepository(
        ReelsLocalDataSource(
            PanalinkDatabase.getDatabase(com.example.PanaApplication.instance).statesDao()
        )
    )
) : ViewModel() {

    private val errorHandler = com.example.util.Resilience.globalExceptionHandler("ReelsViewModel")
    private val processing = Collections.synchronizedSet(mutableSetOf<String>())

    val reelsState: StateFlow<ReelsUiState> = repository.observeReels()
        .map { reels -> ReelsUiState.Success(reels) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReelsUiState.Loading)

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments

    private val _commentsReelId = MutableStateFlow<String?>(null)
    val commentsReelId: StateFlow<String?> = _commentsReelId

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var commentsJob: Job? = null

    private val prefs by lazy {
        com.example.PanaApplication.instance.getSharedPreferences("reels_feature", android.content.Context.MODE_PRIVATE)
    }

    private val preloadManager = com.example.media.feed.ReelsPreloadManager(com.example.PanaApplication.instance)

    fun preloadNextReel(url: String) {
        preloadManager.preloadNext(url)
    }

    fun consumePreloadedPlayer(url: String?): androidx.media3.exoplayer.ExoPlayer? {
        return preloadManager.consumePreloaded(url)
    }

    fun releasePreload() {
        preloadManager.releasePreload()
    }

    fun getLastViewedReelId(): String? = prefs.getString("last_viewed_reel_id", null)

    fun rememberLastViewedReel(reelId: String) {
        if (reelId.isNotBlank()) prefs.edit().putString("last_viewed_reel_id", reelId).apply()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                repository.refresh().onFailure {
                    Log.e("ReelsViewModel", "Remote reel refresh failed", it)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadActiveStates(showLoading: Boolean = false) = refresh()

    fun toggleLike(reelId: String, liked: Boolean, onError: ((String) -> Unit)? = null) =
        runAction("like:$reelId", onError) { repository.toggleLike(reelId, liked) }

    fun toggleFavorite(reelId: String, favorited: Boolean, onError: ((String) -> Unit)? = null) =
        runAction("favorite:$reelId", onError) { repository.toggleFavorite(reelId, favorited) }

    fun incrementShare(reelId: String, onError: ((String) -> Unit)? = null) =
        registerShare(reelId, onError)

    fun registerShare(reelId: String, onError: ((String) -> Unit)? = null) =
        runAction("share:$reelId", onError) { repository.registerShare(reelId) }

    fun registerView(reelId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.registerView(reelId).onFailure {
                Log.e("ReelsViewModel", "Failed to register reel view: $reelId", it)
            }
        }
    }

    fun registerView(reel: UserStateWithUser) = registerView(reel.state.id)

    fun addComment(reelId: String, text: String, parentId: String? = null, onError: ((String) -> Unit)? = null) {
        val clean = text.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.addComment(reelId, clean, parentId)
                .onFailure { onError?.invoke(it.localizedMessage ?: "Error al comentar") }
        }
    }

    fun loadComments(reelId: String) {
        if (_commentsReelId.value == reelId && commentsJob?.isActive == true) return
        commentsJob?.cancel()
        _commentsReelId.value = reelId
        _currentComments.value = emptyList()
        commentsJob = viewModelScope.launch(errorHandler + Dispatchers.IO) {
            launch {
                repository.observeComments(reelId).collect { comments ->
                    if (_commentsReelId.value == reelId) {
                        _currentComments.value = comments.sortedByDescending { it.createdAt }
                    }
                }
            }
            repository.refreshComments(reelId)
        }
    }

    fun clearComments(reelId: String? = null) {
        if (reelId == null || _commentsReelId.value == reelId) {
            commentsJob?.cancel()
            commentsJob = null
            _commentsReelId.value = null
            _currentComments.value = emptyList()
        }
    }

    fun deleteComment(reelId: String, commentId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.deleteComment(commentId).onSuccess { loadComments(reelId) }
        }
    }

    fun removeFromFeed(reelId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.clearLocalReel(reelId)
        }
    }

    fun deleteReel(reel: UserStateWithUser, onSuccess: () -> Unit = {}, onError: ((String) -> Unit)? = null) {
        runAction("delete:${reel.state.id}", onError) {
            repository.deleteReel(reel.state.id, reel.state.mediaUrl).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    fun deleteState(reelId: String, onSuccess: () -> Unit = {}, onError: ((String) -> Unit)? = null) {
        val reel = (reelsState.value as? ReelsUiState.Success)?.reels?.firstOrNull { it.state.id == reelId }
        if (reel == null) {
            onError?.invoke("Reel no encontrado")
            return
        }
        deleteReel(reel, onSuccess, onError)
    }

    private fun runAction(
        key: String,
        onError: ((String) -> Unit)?,
        block: suspend () -> Result<Unit>
    ) {
        if (!processing.add(key)) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                block().onFailure { onError?.invoke(it.localizedMessage ?: "No se pudo completar la acción") }
            } finally {
                processing.remove(key)
            }
        }
    }

    override fun onCleared() {
        commentsJob?.cancel()
        preloadManager.releasePreload()
        super.onCleared()
    }
}
