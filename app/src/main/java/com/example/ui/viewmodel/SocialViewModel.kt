package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.repository.SocialRepository
import com.example.data.repository.SocialRepositoryImpl
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SocialUiState(
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val replyingTo: Comment? = null,
    val inputText: String = ""
)

sealed class CommentsEvent {
    data class OnReplyTo(val comment: Comment) : CommentsEvent()
    object OnCancelReply : CommentsEvent()
    data class OnSendComment(val stateId: String, val text: String) : CommentsEvent()
}

class SocialViewModel(
    private val repository: SocialRepository = SocialRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null
    private var currentStateId: String? = null

    fun setTrackedState(stateId: String) {
        if (currentStateId == stateId) return
        currentStateId = stateId
        observationJob?.cancel()
        // Clear the previous Reel immediately so its comments cannot flash while the new target loads.
        _uiState.value = SocialUiState()
        observationJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val statesDao = database.statesDao()
            launch {
                statesDao.getStateFlowById(stateId).collect { stateEntity ->
                    if (currentStateId != stateId) return@collect
                    if (stateEntity != null) _uiState.value = _uiState.value.copy(
                        likeCount = stateEntity.likesCount,
                        isLiked = stateEntity.likedByMe
                    )
                }
            }
            launch {
                repository.getComments(stateId, true).collect { commentsList ->
                    if (currentStateId != stateId) return@collect
                    _uiState.value = _uiState.value.copy(comments = commentsList)
                }
            }
        }
    }

    fun toggleLike(stateId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { repository.toggleLike(stateId, true) } catch (_: Exception) { }
        }
    }

    fun onEvent(event: CommentsEvent) {
        when (event) {
            is CommentsEvent.OnReplyTo -> _uiState.value = _uiState.value.copy(replyingTo = event.comment)
            is CommentsEvent.OnCancelReply -> _uiState.value = _uiState.value.copy(replyingTo = null)
            is CommentsEvent.OnSendComment -> {
                val parentId = _uiState.value.replyingTo?.id
                _uiState.value = _uiState.value.copy(replyingTo = null)
                viewModelScope.launch { repository.addComment(event.stateId, event.text, parentId, true) }
            }
        }
    }

    fun addComment(stateId: String, text: String, parentId: String? = null) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.addComment(stateId, text, parentId, true) }
    }
}
