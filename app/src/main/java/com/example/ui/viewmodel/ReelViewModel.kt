package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.data.repository.ReelRepository
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections

sealed class ReelUiState {
    data object Loading : ReelUiState()
    data class Success(val reels: List<UserStateWithUser>) : ReelUiState()
    data class Error(val message: String) : ReelUiState()
}

/**
 * ViewModel dedicated to Reels. It deliberately does not expose or depend on
 * StatesViewModel, so Reel UI state/actions cannot accidentally bleed into
 * Stories or Wall state.
 */
class ReelViewModel(
    private val repository: ReelRepository = ReelRepository()
) : ViewModel() {

    private val errorHandler = com.example.util.Resilience.globalExceptionHandler("ReelViewModel")
    private val processingIds = Collections.synchronizedSet(mutableSetOf<String>())

    val reelsState: StateFlow<ReelUiState> = repository.observeReels()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReelUiState.Loading
        )

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments

    private val _commentsReelId = MutableStateFlow<String?>(null)
    val commentsReelId: StateFlow<String?> = _commentsReelId

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var commentsJob: Job? = null

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                repository.refresh()
                    .onFailure { error ->
                        Log.e("ReelViewModel", "Reel refresh failed", error)
                    }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleLike(reelId: String, currentlyLiked: Boolean, onError: ((String) -> Unit)? = null) {
        if (!processingIds.add("like:$reelId")) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                repository.toggleLike(reelId, currentlyLiked)
                    .onFailure { onError?.invoke(it.localizedMessage ?: "Error al dar me gusta") }
            } finally {
                processingIds.remove("like:$reelId")
            }
        }
    }

    fun toggleFavorite(reelId: String, currentlyFavorited: Boolean, onError: ((String) -> Unit)? = null) {
        if (!processingIds.add("favorite:$reelId")) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                repository.toggleFavorite(reelId, currentlyFavorited)
                    .onFailure { onError?.invoke(it.localizedMessage ?: "Error al guardar favorito") }
            } finally {
                processingIds.remove("favorite:$reelId")
            }
        }
    }

    fun registerShare(reelId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.registerShare(reelId)
                .onFailure { onError?.invoke(it.localizedMessage ?: "Error al registrar compartir") }
        }
    }

    fun registerView(reel: UserStateWithUser) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.registerView(reel.state.id)
                .onSuccess {
                    val authorId = reel.state.userId
                    if (authorId.isNotBlank() && authorId != SupabaseClient.currentUser?.id) {
                        com.example.data.repository.NotificationsRepository()
                            .createNotification(authorId, "view", reel.state.id)
                    }
                    repository.saveReelLocally(
                        reel.copy(state = reel.state.copy(viewedByMe = true))
                    )
                }
        }
    }

    fun addComment(
        reelId: String,
        commentText: String,
        parentId: String? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val text = commentText.trim()
        if (text.isBlank()) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            repository.addComment(reelId, text, parentId)
                .onSuccess {
                    val reel = currentReel(reelId)
                    val authorId = reel?.state?.userId.orEmpty()
                    if (authorId.isNotBlank() && authorId != SupabaseClient.currentUser?.id) {
                        com.example.data.repository.NotificationsRepository()
                            .createNotification(authorId, "comment", reelId)
                    }
                }
                .onFailure { error ->
                    Log.e("ReelViewModel", "Comment failed: $reelId", error)
                    onError?.invoke(error.localizedMessage ?: "Error al comentar")
                }
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
                    // Ignore late emissions from an older Reel after navigation.
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
            repository.deleteComment(commentId)
                .onSuccess { loadComments(reelId) }
        }
    }

    fun deleteReel(reel: UserStateWithUser, onSuccess: () -> Unit = {}, onError: ((String) -> Unit)? = null) {
        if (!processingIds.add("delete:${reel.state.id}")) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                repository.deleteReel(reel.state.id, reel.state.mediaUrl)
                    .onSuccess { onSuccess() }
                    .onFailure { onError?.invoke(it.localizedMessage ?: "No se pudo eliminar el Reel") }
            } finally {
                processingIds.remove("delete:${reel.state.id}")
            }
        }
    }

    private fun currentReel(reelId: String): UserStateWithUser? =
        (reelsState.value as? ReelUiState.Success)?.reels?.firstOrNull { it.state.id == reelId }

    override fun onCleared() {
        commentsJob?.cancel()
        super.onCleared()
    }
}
