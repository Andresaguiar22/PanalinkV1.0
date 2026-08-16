package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.data.repository.reels.ReelsLocalDataSource
import com.example.data.repository.reels.ReelsRepository
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections

sealed class ReelsUiState {
    data object Loading : ReelsUiState()
    data class Success(val reels: List<UserStateWithUser>) : ReelsUiState()
    data class Error(val message: String) : ReelsUiState()
}

/** Compatibility state used by the current Reel feed while the feature-owned
 * repository remains the source of truth. */
typealias StatesUiState = ReelsUiState

data class ReelUploadProgress(val percent: Int, val status: String)

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
    private val preferences = PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        .openHelper.writableDatabase

    val reelsState: StateFlow<ReelsUiState> = repository.observeReels()
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

    fun toggleLike(reelId: String, liked: Boolean, onError: ((String) -> Unit)? = null) =
        runAction("like:$reelId", onError) { repository.toggleLike(reelId, liked) }

    fun toggleFavorite(reelId: String, favorited: Boolean, onError: ((String) -> Unit)? = null) =
        runAction("favorite:$reelId", onError) { repository.toggleFavorite(reelId, favorited) }

    fun registerShare(reelId: String, onError: ((String) -> Unit)? = null) =
        runAction("share:$reelId", onError) { repository.registerShare(reelId) }

    fun registerView(reel: UserStateWithUser) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.registerView(reel.state.id).onSuccess {
                repository.saveReelLocally(reel.copy(state = reel.state.copy(viewedByMe = true)))
            }
        }
    }

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
        super.onCleared()
    }
}
