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

    // Map to keep track of local counters to avoid double-counting in ultra-fast taps
    private val localActionTimestamps = mutableMapOf<String, Long>()

    // Primary Source of Truth: Room Flow
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
        observeUploadSuccess()
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
                    val isReel = isReelState(currentState.state)
                    
                    statesRepository.toggleLike(stateId, wasLiked, isReel)
                        .onFailure { error ->
                            Log.e("StatesViewModel", "Like Failure: id=$stateId", error)
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
                    val isReel = isReelState(currentState.state)
                    statesRepository.toggleFavorite(stateId, currentFavState, isReel)
                        .onFailure { error ->
                            onError?.invoke(error.localizedMessage ?: "Error al guardar favorito")
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
                val isReel = isReelState(currentState.state)
                statesRepository.incrementShare(stateId, isReel)
                    .onFailure { error ->
                        onError?.invoke(error.localizedMessage ?: "Error al registrar compartir")
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
        
        // Cancel previous comments job for this state if any
        commentsJobs[stateId]?.cancel()

        val job = viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val currentState = (reelsState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
                ?: (storiesState.value as? StatesUiState.Success)?.states?.find { it.state.id == stateId }
            val isReel = currentState?.let { isReelState(it.state) } ?: false

            // 1. Observe Flow from Room immediately
            launch {
                statesRepository.getCommentsFlow(stateId, isReel).collect { comments ->
                    val serverIds = comments.map { it.id }.toSet()
                    val unsavedTemps = existingTemps.filter { it.id !in serverIds }
                    val sortedList = (comments + unsavedTemps).sortedByDescending { it.createdAt }
                    _currentComments.value = sortedList
                }
            }

            // 2. Refresh from remote
            statesRepository.getStateComments(stateId, isReel)
        }
        commentsJobs[stateId] = job
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
                        ?: com.example.data.model.Profile(currentUser.id, "", null)
                    
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
