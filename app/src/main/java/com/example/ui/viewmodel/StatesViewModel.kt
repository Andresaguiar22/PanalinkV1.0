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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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

    init { observeUploadSuccess() }

    private fun observeUploadSuccess() {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            com.example.data.repository.UploadRepository.uploadSuccessEvent.collect { loadActiveStates() }
        }
    }

    fun onStoryDraftChange(text: String) {
        _storyDraft.value = text
        viewModelScope.launch(errorHandler + Dispatchers.IO) { draftsRepository.saveChatDraft("story_draft_id", text) }
    }

    fun loadStoryDraft() {
        viewModelScope.launch(errorHandler + Dispatchers.IO) { _storyDraft.value = draftsRepository.getChatDraft("story_draft_id") ?: "" }
    }

    fun clearStoryDraft() {
        _storyDraft.value = ""
        viewModelScope.launch(errorHandler + Dispatchers.IO) { draftsRepository.deleteChatDraft("story_draft_id") }
    }

    private val _uiLoadingState = MutableStateFlow<String?>(null)
    val uiLoadingState: StateFlow<String?> = _uiLoadingState

    fun loadActiveStates(showLoading: Boolean = false) {
        if (isActiveStatesLoading) return
        isActiveStatesLoading = true
        if (showLoading) _uiLoadingState.value = "Cargando..."
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            statesRepository.getActiveStates()
                .onSuccess { _uiLoadingState.value = null; isActiveStatesLoading = false }
                .onFailure { error ->
                    _uiLoadingState.value = null
                    isActiveStatesLoading = false
                    Log.e("StatesViewModel", "Refresh states failed", error)
                }
        }
    }

    fun toggleLike(stateId: String, currentLikeState: Boolean, onError: ((String) -> Unit)? = null) {
        val now = System.currentTimeMillis()
        if (now - (localActionTimestamps[stateId] ?: 0L) < 500) return
        localActionTimestamps[stateId] = now
        if (!processingIds.add(stateId)) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                val currentState = findState(stateId) ?: return@launch
                statesRepository.toggleLike(stateId, currentState.state.likedByMe ?: currentLikeState, isReelState(currentState.state))
                    .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al dar me gusta") }
            } finally { processingIds.remove(stateId) }
        }
    }

    fun toggleFavorite(stateId: String, currentFavState: Boolean, onError: ((String) -> Unit)? = null) {
        if (!processingIds.add(stateId)) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                val currentState = findState(stateId) ?: return@launch
                statesRepository.toggleFavorite(stateId, currentFavState, isReelState(currentState.state))
                    .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al guardar favorito") }
            } finally { processingIds.remove(stateId) }
        }
    }

    fun incrementShare(stateId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId) ?: return@launch
            statesRepository.incrementShare(stateId, isReelState(currentState.state))
                .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al registrar compartir") }
        }
    }

    fun addComment(stateId: String, commentText: String, parentId: String? = null, onError: ((String) -> Unit)? = null) {
        if (commentText.isBlank()) return
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId) ?: return@launch
            val isReel = isReelState(currentState.state)
            val authorId = currentState.state.userId
            statesRepository.addComment(stateId, commentText.trim(), isReel, parentId)
                .onSuccess {
                    if (authorId.isNotEmpty() && authorId != SupabaseClient.currentUser?.id) {
                        com.example.data.repository.NotificationsRepository().createNotification(authorId, "comment", stateId)
                    }
                }
                .onFailure { error -> onError?.invoke(error.localizedMessage ?: "Error al comentar") }
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
                    if (sendResult.isSuccess) onSuccess() else onError?.invoke(sendResult.exceptionOrNull()?.localizedMessage ?: "Error al enviar mensaje por DM")
                } else onError?.invoke(chatResult.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar el chat con el autor")
            } catch (e: Exception) { onError?.invoke(e.localizedMessage ?: "Error inesperado al enviar DM") }
        }
    }

    fun loadComments(stateId: String) {
        val existingTemps = _currentComments.value.filter { it.stateId == stateId && it.id.startsWith("temp_") }
        commentsJobs[stateId]?.cancel()
        commentsJobs[stateId] = viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId)
            val isReel = currentState?.let { isReelState(it.state) } ?: false
            launch {
                statesRepository.getCommentsFlow(stateId, isReel).collect { comments ->
                    val ids = comments.map { it.id }.toSet()
                    _currentComments.value = (comments + existingTemps.filter { it.id !in ids }).sortedByDescending { it.createdAt }
                }
            }
            statesRepository.getStateComments(stateId, isReel)
        }
    }

    fun loadSpectators(stateId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId)
            val isReel = currentState?.let { isReelState(it.state) } ?: false
            statesRepository.getStatusViews(stateId, isReel)
                .onSuccess { _currentSpectators.value = it.sortedBy { viewer -> viewer.viewedAt } }
                .onFailure { _currentSpectators.value = emptyList() }
        }
    }

    fun deleteStateForMe(stateId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            db.statesDao().deleteById(stateId)
            onSuccess()
        }
    }

    fun deleteState(stateId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId) ?: return@launch
            val isReel = isReelState(currentState.state)
            val mediaUrl = currentState.state.mediaUrl
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            db.statesDao().deleteById(stateId)
            statesRepository.deleteUserStatus(stateId, isReel, mediaUrl)
                .onSuccess {
                    mediaUrl?.let { com.example.data.video.VideoCacheManager.removeVideoCache(it) }
                    onSuccess()
                }
        }
    }

    fun deleteComment(stateId: String, commentId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId) ?: return@launch
            statesRepository.deleteComment(commentId, isReelState(currentState.state))
                .onSuccess { loadComments(stateId); loadActiveStates(false) }
        }
    }

    fun registerView(stateId: String) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val currentState = findState(stateId) ?: return@launch
            val isReel = isReelState(currentState.state)
            val authorId = currentState.state.userId
            statesRepository.registerView(stateId, isReel).onSuccess {
                if (authorId.isNotEmpty()) {
                    com.example.data.repository.NotificationsRepository().createNotification(authorId, "view", stateId)
                }
                statesRepository.saveStateLocally(currentState.copy(state = currentState.state.copy(viewedByMe = true)))
            }
        }
    }

    fun publishTextState(caption: String, isReel: Boolean = false) {
        if (caption.isBlank()) return
        _createStateFlow.value = CreateStateUiState.Loading("Preparando estado...")
        viewModelScope.launch(errorHandler) {
            statesRepository.createState("text", caption, null, null, isReel = isReel)
                .onSuccess { _createStateFlow.value = CreateStateUiState.Success; clearStoryDraft(); loadActiveStates() }
                .onFailure { error -> _createStateFlow.value = CreateStateUiState.Error(error.localizedMessage ?: "Error publicando estado") }
        }
    }

    fun publishReelBackground(
        context: android.content.Context,
        caption: String?,
        imageBytes: ByteArray,
        mimeType: String,
        uri: android.net.Uri? = null,
        isReel: Boolean = true,
        mediaFile: java.io.File? = null
    ) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
                if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                val tempFile = if (mediaFile?.exists() == true) mediaFile else {
                    val extension = mimeType.substringAfter('/', "bin")
                    java.io.File.createTempFile("upload_temp_", ".$extension", pendingMediaDir).also { file ->
                        when {
                            uri != null -> context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                            imageBytes.isNotEmpty() -> file.writeBytes(imageBytes)
                        }
                    }
                }
                require(tempFile.length() > 0) { "Could not write media data to temp file" }
                val uploadId = java.util.UUID.randomUUID().toString()
                val userId = SupabaseClient.currentUser?.id ?: return@launch
                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                db.pendingUploadDao().insertUpload(
                    com.example.data.database.PendingUploadEntity(
                        id = uploadId,
                        userId = userId,
                        uploadType = "REEL",
                        localFilePath = tempFile.absolutePath,
                        mimeType = mimeType,
                        caption = caption,
                        status = "pending"
                    )
                )
                val request = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.SocialMediaUploadWorker>()
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .setInputData(androidx.work.workDataOf("uploadId" to uploadId))
                    .addTag("social_upload")
                    .addTag("upload_$uploadId")
                    .addTag("social_upload_$uploadId")
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, androidx.work.WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("social_upload_$uploadId", androidx.work.ExistingWorkPolicy.KEEP, request)
            } catch (e: Exception) {
                Log.e("StatesViewModel", "Error scheduling upload worker", e)
                _createStateFlow.value = CreateStateUiState.Error(e.localizedMessage ?: "Error programando publicación")
            }
        }
    }

    fun resetCreateState() { _createStateFlow.value = CreateStateUiState.Idle }

    private fun findState(stateId: String): UserStateWithUser? =
        (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }

    private fun isReelState(state: UserState): Boolean = state.type == "reel" || state.mediaType == "video"
}
