package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserState
import com.example.data.model.UserStateWithUser
import com.example.data.model.Comment
import com.example.data.model.StatusViewer
import com.example.data.repository.StatesRepository
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.Collections

sealed class StatesUiState {
    object Loading : StatesUiState()
    data class Success(val states: List<UserStateWithUser>) : StatesUiState()
    data class Error(val message: String) : StatesUiState()
}

sealed class CreateStateUiState {
    object Idle : CreateStateUiState()
    data class Loading(val message: String = "Preparando...") : CreateStateUiState()
    object Success : CreateStateUiState()
    data class Error(val message: String) : CreateStateUiState()
}

class StatesViewModel(private val statesRepository: StatesRepository = StatesRepository()) : ViewModel() {
    private val errorHandler = com.example.util.Resilience.globalExceptionHandler("StatesViewModel")

    private var isActiveStatesLoading = false
    private val processingIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val localActionTimestamps = mutableMapOf<String, Long>()

    val statesState: StateFlow<StatesUiState> = statesRepository.getLocalStatesFlow(isReel = false)
        .map { list -> if (list.isEmpty()) StatesUiState.Loading else StatesUiState.Success(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    val storiesState: StateFlow<StatesUiState> = statesRepository.getLocalStatesFlow(isReel = false)
        .map { StatesUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    val reelsState: StateFlow<StatesUiState> = statesRepository.getLocalStatesFlow(isReel = true)
        .map { StatesUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    private val _createStateFlow = MutableStateFlow<CreateStateUiState>(CreateStateUiState.Idle)
    val createStateFlow: StateFlow<CreateStateUiState> = _createStateFlow

    private val commentsJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments
    private val _currentSpectators = MutableStateFlow<List<StatusViewer>>(emptyList())
    val currentSpectators: StateFlow<List<StatusViewer>> = _currentSpectators
    private val draftsRepository = com.example.data.repository.DraftsRepository()
    private val _storyDraft = MutableStateFlow("")
    val storyDraft: StateFlow<String> = _storyDraft

    init {
        observeUploadSuccess()
    }

    private fun observeUploadSuccess() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.repository.UploadRepository.uploadSuccessEvent.collect {
                loadActiveStates()
            }
        }
    }

    fun onStoryDraftChange(text: String) {
        _storyDraft.value = text
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            draftsRepository.saveChatDraft("story_draft_id", text)
        }
    }

    fun loadStoryDraft() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            _storyDraft.value = draftsRepository.getChatDraft("story_draft_id") ?: ""
        }
    }

    fun clearStoryDraft() {
        _storyDraft.value = ""
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            draftsRepository.deleteChatDraft("story_draft_id")
        }
    }

    private val _uiLoadingState = MutableStateFlow<String?>(null)
    val uiLoadingState: StateFlow<String?> = _uiLoadingState

    fun loadActiveStates(showLoading: Boolean = false) {
        if (isActiveStatesLoading) return
        isActiveStatesLoading = true
        if (showLoading) _uiLoadingState.value = "Cargando..."
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            statesRepository.getActiveStates()
                .onSuccess {
                    _uiLoadingState.value = null
                    isActiveStatesLoading = false
                }
                .onFailure { error ->
                    _uiLoadingState.value = null
                    isActiveStatesLoading = false
                    android.util.Log.e("StatesViewModel", "Refresh states failed", error)
                }
        }
    }

    fun toggleLike(stateId: String, currentLikeState: Boolean, onError: ((String) -> Unit)? = null) {
        val now = System.currentTimeMillis()
        val lastAction = localActionTimestamps[stateId] ?: 0L
        if (now - lastAction < 500) {
            Log.d("StatesViewModel", "Ignoring rapid tap for like on $stateId")
            return
        }
        localActionTimestamps[stateId] = now
        if (!processingIds.add(stateId)) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                    ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                if (currentState != null) {
                    val wasLiked = currentState.state.likedByMe ?: false
                    val isReel = isReelState(currentState.state)
                    statesRepository.toggleLike(stateId, wasLiked, isReel)
                        .onFailure { error ->
                            Log.e("StatesViewModel", "Like Failure: id=$stateId", error)
                            onError?.invoke(error.localizedMessage ?: "Error al dar me gusta")
                        }
                }
            } finally { processingIds.remove(stateId) }
        }
    }

    fun toggleFavorite(stateId: String, currentFavState: Boolean, onError: ((String) -> Unit)? = null) {
        if (processingIds.contains(stateId)) return
        processingIds.add(stateId)
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                    ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                if (currentState != null) {
                    val isReel = isReelState(currentState.state)
                    statesRepository.toggleFavorite(stateId, currentFavState, isReel)
                        .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al guardar favorito") }
                }
            } finally { processingIds.remove(stateId) }
        }
    }

    fun incrementShare(stateId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            if (currentState != null) {
                val isReel = isReelState(currentState.state)
                statesRepository.incrementShare(stateId, isReel)
                    .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al registrar compartir") }
            }
        }
    }

    fun addComment(stateId: String, commentText: String, parentId: String? = null, onError: ((String) -> Unit)? = null) {
        if (commentText.isBlank()) return
        val trimmedText = commentText.trim()
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            if (currentState != null) {
                val isReel = isReelState(currentState.state)
                val authorId = currentState.state.userId
                statesRepository.addComment(stateId, trimmedText, isReel, parentId)
                    .onSuccess {
                        Log.d("StatesViewModel", "Comment Success: id=$stateId")
                        if (authorId.isNotEmpty() && authorId != SupabaseClient.currentUser?.id) {
                            com.example.data.repository.NotificationsRepository().createNotification(authorId, "comment", stateId)
                        }
                    }
                    .onFailure { error ->
                        Log.e("StatesViewModel", "Comment Failure: id=$stateId", error)
                        onError?.invoke(error.localizedMessage ?: "Error al comentar")
                    }
            }
        }
    }

    fun sendQuickReplyToAuthor(authorId: String, messageText: String, onSuccess: () -> Unit = {}, onError: ((String) -> Unit)? = null) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            try {
                val chatsRepo = com.example.data.repository.ChatsRepository()
                val messagesRepo = com.example.data.repository.MessagesRepository.getInstance()
                val chatResult = chatsRepo.createDirectChat(authorId)
                if (chatResult.isSuccess) {
                    val chat = chatResult.getOrThrow()
                    val sendResult = messagesRepo.sendMessage(chatId = chat.id, content = messageText, receiverUid = authorId)
                    if (sendResult.isSuccess) onSuccess()
                    else onError?.invoke(sendResult.exceptionOrNull()?.localizedMessage ?: "Error al enviar mensaje por DM")
                } else onError?.invoke(chatResult.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar el chat con el autor")
            } catch (e: Exception) {
                onError?.invoke(e.localizedMessage ?: "Error inesperado al enviar DM")
            }
        }
    }
