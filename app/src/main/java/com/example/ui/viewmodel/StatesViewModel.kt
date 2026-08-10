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
    private val optimisticOverrides = MutableStateFlow<Map<String, UserState>>(emptyMap())

    // Map to keep track of local counters to avoid double-counting in ultra-fast taps
    private val localActionTimestamps = mutableMapOf<String, Long>()

    private fun applyOverrides(list: List<UserStateWithUser>, overrides: Map<String, UserState>): List<UserStateWithUser> {
        return list.map { item ->
            val override = overrides[item.state.id]
            if (override != null) {
                // Ensure we don't overwrite if the Room data is already newer (e.g. sync finished)
                // Actually, per user request, we "retain" the local state until we are sure.
                item.copy(state = override)
            } else item
        }
    }

    // Primary Source of Truth: Room Flow combined with Optimistic Overrides
    val statesState: StateFlow<StatesUiState> = combine(
        statesRepository.getLocalStatesFlow(isReel = false),
        optimisticOverrides
    ) { list, overrides ->
        val merged = applyOverrides(list, overrides)
        if (merged.isEmpty()) StatesUiState.Loading else StatesUiState.Success(merged)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    val storiesState: StateFlow<StatesUiState> = combine(
        statesRepository.getLocalStatesFlow(isReel = false),
        optimisticOverrides
    ) { list, overrides ->
        StatesUiState.Success(applyOverrides(list, overrides))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    val reelsState: StateFlow<StatesUiState> = combine(
        statesRepository.getLocalStatesFlow(isReel = true),
        optimisticOverrides
    ) { list, overrides ->
        StatesUiState.Success(applyOverrides(list, overrides))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatesUiState.Loading)

    private val _createStateFlow = MutableStateFlow<CreateStateUiState>(CreateStateUiState.Idle)
    val createStateFlow: StateFlow<CreateStateUiState> = _createStateFlow

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
                // Background refresh when a post or reel is fully uploaded
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

    init {
        viewModelScope.launch(errorHandler) {
            SupabaseClient.realtimeStatuses.collect { newState ->
                val currentUid = SupabaseClient.currentUser?.id
                val tempProfile = if (newState.userId == currentUid && SupabaseClient.currentProfile != null) {
                    SupabaseClient.currentProfile!!
                } else {
                    com.example.data.model.Profile(newState.userId, "Pana de la Comunidad 🇻🇪", null)
                }
                val tempStateWithUser = UserStateWithUser(newState, tempProfile)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    statesRepository.saveStateLocally(tempStateWithUser)
                }
                
                launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val realProfile = statesRepository.getProfileForUser(newState.userId)
                        statesRepository.saveStateLocally(UserStateWithUser(newState, realProfile))
                    } catch (e: Exception) {
                        android.util.Log.e("StatesViewModel", "Error updating profile for live state", e)
                    }
                }
            }
        }
        
        viewModelScope.launch(errorHandler) {
            SupabaseClient.realtimeLikes.collect { statusId ->
                loadActiveStates(showLoading = false)
            }
        }
        
        viewModelScope.launch(errorHandler) {
            SupabaseClient.realtimeComments.collect { statusId ->
                loadActiveStates(showLoading = false)
            }
        }
    }

    fun loadActiveStates(showLoading: Boolean = false) {
        if (isActiveStatesLoading) return
        isActiveStatesLoading = true
        if (showLoading) {
            _uiLoadingState.value = "Cargando..."
        }
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
                    val newLiked = !wasLiked
                    val currentCount = currentState.state.likesCount ?: 0
                    val newCount = if (wasLiked) (currentCount - 1).coerceAtLeast(0) else currentCount + 1
                    
                    Log.d("StatesViewModel", "Optimistic Like: id=$stateId, wasLiked=$wasLiked, newLiked=$newLiked, old=$currentCount, new=$newCount")

                    // Optimistic local update
                    val optimisticState = currentState.state.copy(
                        likedByMe = newLiked,
                        likesCount = newCount
                    )
                    val optimisticItem = currentState.copy(state = optimisticState)
                    
                    // Add to overrides to retain in UI
                    optimisticOverrides.value = optimisticOverrides.value + (stateId to optimisticState)
                    statesRepository.saveStateLocally(optimisticItem)

                    val isReel = isReelState(currentState.state)
                    val authorId = currentState.state.userId
                    
                    statesRepository.toggleLike(stateId, wasLiked, isReel)
                        .onSuccess { result ->
                            Log.d("StatesViewModel", "Like Success: id=$stateId, serverLiked=${result.liked}, serverCount=${result.likesCount}")
                            
                            if (result.liked && authorId.isNotEmpty() && authorId != SupabaseClient.currentUser?.id) {
                                com.example.data.repository.NotificationsRepository().createNotification(authorId, "like", stateId)
                            }
                            
                            // Prevent count jumping from 0 to 2:
                            // If we just liked it and wasLiked was false, count should be at least 1,
                            // but if currentCount was 0, count is strictly 1 (unless server genuinely has multiple likes).
                            val serverCount = result.likesCount
                            val finalCount = if (result.liked) {
                                if (currentCount == 0 && serverCount > 1) 1 else maxOf(newCount, serverCount)
                            } else {
                                (currentCount - 1).coerceAtLeast(0)
                            }

                            val finalState = currentState.state.copy(
                                likedByMe = result.liked,
                                likesCount = finalCount
                            )
                            optimisticOverrides.value = optimisticOverrides.value + (stateId to finalState)
                            statesRepository.saveStateLocally(currentState.copy(state = finalState))
                        }
                        .onFailure { error ->
                            Log.e("StatesViewModel", "Like Failure: id=$stateId", error)
                            // Revert local update
                            optimisticOverrides.value = optimisticOverrides.value - stateId
                            statesRepository.saveStateLocally(currentState)
                            onError?.invoke(error.localizedMessage ?: "Error al dar me gusta")
                        }
                }
            } finally {
                processingIds.remove(stateId)
            }
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
                    val newFav = !currentFavState
                    val currentCount = currentState.state.favoritesCount ?: 0
                    val newCount = if (currentFavState) (currentCount - 1).coerceAtLeast(0) else currentCount + 1
                    
                    // Optimistic local update
                    val optimisticState = currentState.state.copy(
                        favoritedByMe = newFav,
                        favoritesCount = newCount
                    )
                    optimisticOverrides.value = optimisticOverrides.value + (stateId to optimisticState)
                    statesRepository.saveStateLocally(currentState.copy(state = optimisticState))

                    val isReel = isReelState(currentState.state)
                    statesRepository.toggleFavorite(stateId, currentFavState, isReel)
                        .onSuccess { result ->
                            val finalState = currentState.state.copy(
                                favoritedByMe = result.favorited,
                                favoritesCount = if (result.favoritesCount >= 0) result.favoritesCount else newCount
                            )
                            optimisticOverrides.value = optimisticOverrides.value + (stateId to finalState)
                            statesRepository.saveStateLocally(currentState.copy(state = finalState))
                            delay(1500)
                            optimisticOverrides.value = optimisticOverrides.value - stateId
                        }
                        .onFailure { error ->
                            optimisticOverrides.value = optimisticOverrides.value - stateId
                            onError?.invoke(error.localizedMessage ?: "Error al guardar favorito")
                            statesRepository.saveStateLocally(currentState) // Revert
                        }
                }
            } finally {
                processingIds.remove(stateId)
            }
        }
    }

    fun incrementShare(stateId: String, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }

            if (currentState != null) {
                val newCount = (currentState.state.sharesCount ?: 0) + 1
                statesRepository.saveStateLocally(currentState.copy(state = currentState.state.copy(
                    sharesCount = newCount
                )))

                val isReel = isReelState(currentState.state)
                statesRepository.incrementShare(stateId, isReel)
                    .onFailure { error ->
                        onError?.invoke(error.localizedMessage ?: "Error al registrar compartir")
                        statesRepository.saveStateLocally(currentState) // Revert
                    }
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
                
                // Optimistic update for comment count
                val currentCount = currentState.state.commentsCount ?: 0
                val optimisticState = currentState.state.copy(
                    commentsCount = currentCount + 1
                )
                
                Log.d("StatesViewModel", "Optimistic Comment: id=$stateId, old=$currentCount, new=${currentCount + 1}")

                optimisticOverrides.value = optimisticOverrides.value + (stateId to optimisticState)
                statesRepository.saveStateLocally(currentState.copy(state = optimisticState))

                // Optimistic update for _currentComments list
                val currentUser = SupabaseClient.currentUser
                val profileName = SupabaseClient.currentProfile?.displayName ?: "Yo"
                val avatarUrl = SupabaseClient.currentProfile?.avatarUrl
                val tempId = "temp_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"
                val tempComment = Comment(
                    id = tempId,
                    stateId = stateId,
                    userId = currentUser?.id ?: "",
                    text = trimmedText,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date()),
                    authorName = profileName,
                    avatarUrl = avatarUrl,
                    parentCommentId = parentId
                )
                _currentComments.value = _currentComments.value + tempComment

                statesRepository.addComment(stateId, trimmedText, isReel, parentId)
                    .onSuccess {
                        Log.d("StatesViewModel", "Comment Success: id=$stateId")
                        
                        if (authorId.isNotEmpty() && authorId != SupabaseClient.currentUser?.id) {
                            com.example.data.repository.NotificationsRepository().createNotification(authorId, "comment", stateId)
                        }

                        // Success: Load the real list and keep commentsCount in local DB
                        statesRepository.getStateComments(stateId, isReel)
                            .onSuccess { realList ->
                                _currentComments.value = realList
                                val finalCommentsCount = maxOf(currentCount + 1, realList.size)
                                val finalState = currentState.state.copy(commentsCount = finalCommentsCount)
                                optimisticOverrides.value = optimisticOverrides.value + (stateId to finalState)
                                statesRepository.saveStateLocally(currentState.copy(state = finalState))
                            }
                    }
                    .onFailure { error ->
                        Log.e("StatesViewModel", "Comment Failure: id=$stateId", error)
                        _currentComments.value = _currentComments.value.filter { it.id != tempId }
                        optimisticOverrides.value = optimisticOverrides.value - stateId
                        statesRepository.saveStateLocally(currentState)
                        onError?.invoke(error.localizedMessage ?: "Error al comentar")
                    }
            }
        }
    }
    fun sendQuickReplyToAuthor(
        authorId: String,
        messageText: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            try {
                val chatsRepo = com.example.data.repository.ChatsRepository()
                val messagesRepo = com.example.data.repository.MessagesRepository.getInstance()
                val chatResult = chatsRepo.createDirectChat(authorId)
                if (chatResult.isSuccess) {
                    val chat = chatResult.getOrThrow()
                    val sendResult = messagesRepo.sendMessage(
                        chatId = chat.id,
                        content = messageText,
                        receiverUid = authorId
                    )
                    if (sendResult.isSuccess) {
                        onSuccess()
                    } else {
                        onError?.invoke(sendResult.exceptionOrNull()?.localizedMessage ?: "Error al enviar mensaje por DM")
                    }
                } else {
                    onError?.invoke(chatResult.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar el chat con el autor")
                }
            } catch (e: Exception) {
                onError?.invoke(e.localizedMessage ?: "Error inesperado al enviar DM")
            }
        }
    }

    fun loadComments(stateId: String) {
        val existingTemps = _currentComments.value.filter { it.stateId == stateId && it.id.startsWith("temp_") }
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            val isReel = currentState?.let { isReelState(it.state) } ?: false
            
            statesRepository.getStateComments(stateId, isReel)
                .onSuccess { list ->
                    val serverIds = list.map { it.id }.toSet()
                    val unsavedTemps = existingTemps.filter { it.id !in serverIds }
                    // Sort comments by newest first (descending createdAt)
                    val sortedList = (list + unsavedTemps).sortedByDescending { it.createdAt }
                    _currentComments.value = sortedList
                }
                .onFailure {
                    _currentComments.value = existingTemps.sortedByDescending { it.createdAt }
                }
        }
    }

    fun loadSpectators(stateId: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            val isReel = currentState?.let { isReelState(it.state) } ?: false
            
            statesRepository.getStatusViews(stateId, isReel)
                .onSuccess { list ->
                    // Sort spectators by oldest first (ascending viewedAt) as requested
                    val sortedList = list.sortedBy { it.viewedAt }
                    _currentSpectators.value = sortedList
                }
                .onFailure {
                    _currentSpectators.value = emptyList()
                }
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
        viewModelScope.launch {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }

            if (currentState != null) {
                val isReel = isReelState(currentState.state)
                val mediaUrl = currentState.state.mediaUrl
                
                // Remove instantly from local DB
                val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                db.statesDao().deleteById(stateId)
                
                statesRepository.deleteUserStatus(stateId, isReel, mediaUrl)
                    .onSuccess {
                        mediaUrl?.let { com.example.data.video.VideoCacheManager.removeVideoCache(it) }
                        onSuccess()
                    }
            }
        }
    }

    fun deleteComment(stateId: String, commentId: String) {
        viewModelScope.launch {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            val isReel = currentState?.let { isReelState(it.state) } ?: false
            
            statesRepository.deleteComment(commentId, isReel)
                .onSuccess {
                    loadComments(stateId)
                    loadActiveStates(showLoading = false)
                }
        }
    }

    fun registerView(stateId: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }

            if (currentState != null) {
                val isReel = isReelState(currentState.state)
                val authorId = currentState.state.userId
                
                statesRepository.registerView(stateId, isReel)
                    .onSuccess {
                        if (authorId.isNotEmpty()) {
                            com.example.data.repository.NotificationsRepository().createNotification(authorId, "view", stateId)
                        }
                        // Update viewed status locally
                        statesRepository.saveStateLocally(currentState.copy(state = currentState.state.copy(viewedByMe = true)))
                    }
            }
        }
    }

    fun publishTextState(caption: String, isReel: Boolean = false) {
        if (caption.isBlank()) return
        _createStateFlow.value = CreateStateUiState.Loading("Preparando estado...")
        viewModelScope.launch {
            statesRepository.createState(
                mediaType = "text",
                caption = caption,
                mediaBytes = null,
                mediaMimeType = null,
                isReel = isReel
            ).onSuccess {
                _createStateFlow.value = CreateStateUiState.Success
                clearStoryDraft()
                loadActiveStates()
            }.onFailure { error ->
                _createStateFlow.value = CreateStateUiState.Error(error.localizedMessage ?: "Error publicando estado")
            }
        }
    }

    fun publishImageState(
        context: android.content.Context,
        caption: String?,
        imageBytes: ByteArray,
        mimeType: String,
        uri: android.net.Uri? = null,
        isReel: Boolean = false,
        mediaFile: java.io.File? = null
    ) {
        _createStateFlow.value = CreateStateUiState.Loading("Preparando archivo...")
        viewModelScope.launch(errorHandler) {
            var finalFile = mediaFile
            if (finalFile == null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
                        if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                        val extension = mimeType.split("/").lastOrNull() ?: "bin"
                        val tempFile = java.io.File.createTempFile("state_temp_", ".$extension", pendingMediaDir)
                        
                        if (uri != null && mimeType.startsWith("video/")) {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                tempFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        } else if (imageBytes.isNotEmpty()) {
                            tempFile.writeBytes(imageBytes)
                        } else if (uri != null) {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                tempFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0) {
                            finalFile = tempFile
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("StatesViewModel", "Failed to prepare local temp file", e)
                    }
                }
            }

            if (finalFile != null && mimeType.startsWith("video/")) {
                _createStateFlow.value = CreateStateUiState.Loading("Comprimiendo video...")
                try {
                    val compressedFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
                        if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                        java.io.File.createTempFile("state_compressed_", ".mp4", pendingMediaDir)
                    }
                    
                    val success = com.example.util.VideoCompressorHelper.compressVideo(
                        context,
                        android.net.Uri.fromFile(finalFile!!),
                        compressedFile
                    ) { progress ->
                        val percent = (progress * 100).toInt()
                        _createStateFlow.value = CreateStateUiState.Loading("Comprimiendo video ($percent%)...")
                    }
                    if (compressedFile.exists() && compressedFile.length() > 0) {
                        if (finalFile != mediaFile) {
                            finalFile!!.delete()
                        }
                        finalFile = compressedFile
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StatesViewModel", "Fallo al comprimir video", e)
                    _createStateFlow.value = CreateStateUiState.Error("Fallo en compresión de video: ${e.localizedMessage}")
                    return@launch
                }
            }
            
            if (finalFile == null) {
                _createStateFlow.value = CreateStateUiState.Error("No se pudo preparar el archivo local para la publicación.")
                return@launch
            }
            _createStateFlow.value = CreateStateUiState.Loading("Preparando cola persistente...")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val uploadId = java.util.UUID.randomUUID().toString()
                    val currentUser = com.example.data.supabase.SupabaseClient.currentUser
                    val userId = currentUser?.id ?: ""
                    val uploadType = if (isReel) "REEL" else "STATE"
                    
                    val pendingUpload = com.example.data.database.PendingUploadEntity(
                        id = uploadId,
                        userId = userId,
                        uploadType = uploadType,
                        localFilePath = finalFile!!.absolutePath,
                        mimeType = mimeType,
                        caption = caption,
                        status = "pending"
                    )
                    
                    val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                    db.pendingUploadDao().insertUpload(pendingUpload)
                    
                    val constraints = androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                        
                    val inputData = androidx.work.workDataOf("uploadId" to uploadId)
                    
                    val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.SocialMediaUploadWorker>()
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .addTag("social_upload")
                        .addTag("upload_$uploadId")
                        .addTag("social_upload_$uploadId")
                        .setBackoffCriteria(
                            androidx.work.BackoffPolicy.EXPONENTIAL,
                            androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                            java.util.concurrent.TimeUnit.MILLISECONDS
                        )
                        .build()
                        
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "social_upload_$uploadId",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        uploadWorkRequest
                    )
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _createStateFlow.value = CreateStateUiState.Success
                        clearStoryDraft()
                        loadActiveStates()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StatesViewModel", "Fallo al registrar en la cola persistente", e)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _createStateFlow.value = CreateStateUiState.Error("Fallo al registrar en cola persistente: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun publishReelBackground(
        context: android.content.Context,
        caption: String?,
        imageBytes: ByteArray,
        mimeType: String,
        uri: android.net.Uri? = null,
        isReel: Boolean = false,
        mediaFile: java.io.File? = null
    ) {
        viewModelScope.launch {
            try {
                val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
                if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                
                val tempFile = if (mediaFile != null && mediaFile.exists()) {
                    mediaFile
                } else {
                    val extension = mimeType.split("/").lastOrNull() ?: "bin"
                    val file = java.io.File.createTempFile("upload_temp_", ".$extension", pendingMediaDir)
                    
                    if (uri != null && mimeType.startsWith("video/")) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            file.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } else if (imageBytes.isNotEmpty()) {
                        file.writeBytes(imageBytes)
                    } else {
                        uri?.let {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                file.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                    }
                    file
                }

                if (tempFile.length() <= 0) {
                    throw Exception("Could not write media data to temp file")
                }

                val uploadId = java.util.UUID.randomUUID().toString()
                val currentUser = com.example.data.supabase.SupabaseClient.currentUser
                val userId = currentUser?.id ?: ""
                
                val pendingUpload = com.example.data.database.PendingUploadEntity(
                    id = uploadId,
                    userId = userId,
                    uploadType = "REEL",
                    localFilePath = tempFile.absolutePath,
                    mimeType = mimeType,
                    caption = caption,
                    status = "pending"
                )
                
                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                db.pendingUploadDao().insertUpload(pendingUpload)

                // --- Optimistic UI via Room SSOT (Only for stories, NOT for reels per user request) ---
                if (currentUser != null && !isReel) {
                    val fakeId = "optimistic_$uploadId"
                    val tempLocalPath = tempFile.absolutePath
                    val fakeState = com.example.data.model.UserState(
                        id = fakeId,
                        authorId = currentUser.id,
                        mediaUrl = "file://$tempLocalPath",
                        mediaType = if (mimeType.startsWith("video/")) "video" else "image",
                        caption = caption,
                        createdAt = com.example.data.supabase.SupabaseClient.getNowIsoString(),
                        type = "story",
                        localVideoPath = tempLocalPath
                    )
                    val myProfile = com.example.data.supabase.SupabaseClient.currentProfile 
                        ?: com.example.data.model.Profile(currentUser.id, "Yo", null)
                    
                    statesRepository.saveStateLocally(com.example.data.model.UserStateWithUser(fakeState, myProfile))
                    Log.d("StatesViewModel", "Optimistic state saved to Room: $fakeId")
                }

                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                    
                val inputData = androidx.work.workDataOf("uploadId" to uploadId)
                
                val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.SocialMediaUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag("social_upload")
                    .addTag("upload_$uploadId")
                    .addTag("social_upload_$uploadId")
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .build()
                    
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "social_upload_$uploadId",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    uploadWorkRequest
                )
                android.util.Log.d("StatesViewModel", "Enqueued social upload work request for reel: $uploadId")
            } catch (e: Exception) {
                android.util.Log.e("StatesViewModel", "Error scheduling upload worker", e)
            }
        }
    }

    fun publishCarouselState(
        context: android.content.Context,
        caption: String?,
        imagesList: List<ByteArray>,
        mimeTypesList: List<String>
    ) {
        _createStateFlow.value = CreateStateUiState.Loading("Preparando carrusel...")
        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            for (index in imagesList.indices) {
                val bytes = imagesList[index]
                val mimeType = mimeTypesList.getOrNull(index) ?: "image/jpeg"
                _createStateFlow.value = CreateStateUiState.Loading("Subiendo imagen ${index + 1} de ${imagesList.size}...")
                
                val uploadResult = com.example.data.repository.UploadRepository().uploadVideo(bytes, mimeType, "Carousel $index", SupabaseClient.currentUser?.id ?: "")
                if (uploadResult.isSuccess) {
                    val url = uploadResult.getOrThrow().url
                    uploadedUrls.add(url)
                } else {
                    val err = uploadResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    _createStateFlow.value = CreateStateUiState.Error("Fallo subiendo imagen ${index + 1}: $err")
                    return@launch
                }
            }
            
            if (uploadedUrls.isEmpty()) {
                _createStateFlow.value = CreateStateUiState.Error("No se pudo subir ninguna imagen para el carrusel.")
                return@launch
            }
            
            val joinedUrls = uploadedUrls.joinToString(",")
            val finalCaptionWithCount = "$caption [CarouselCount: ${uploadedUrls.size}]".trim()
            
            _createStateFlow.value = CreateStateUiState.Loading("Insertando estado de carrusel...")
            statesRepository.createState(
                mediaType = "image",
                caption = finalCaptionWithCount,
                mediaBytes = null,
                mediaMimeType = null,
                isReel = false,
                presetMediaUrl = joinedUrls
            ).onSuccess {
                _createStateFlow.value = CreateStateUiState.Success
                loadActiveStates()
            }.onFailure { error ->
                _createStateFlow.value = CreateStateUiState.Error(error.localizedMessage ?: "Error guardando carrusel")
            }
        }
    }

    fun resetCreateState() {
        _createStateFlow.value = CreateStateUiState.Idle
    }

    private fun isReelState(state: UserState): Boolean {
        return state.type == "reel" || state.mediaType == "video"
    }
}
