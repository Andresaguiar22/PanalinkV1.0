package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Message
import com.example.data.model.Profile
import com.example.data.repository.MessagesRepository
import com.example.data.repository.ProfilesRepository
import com.example.data.repository.StickerRepository
import com.example.util.AudioPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Stable
sealed class ChatUiState {
    @Immutable
    object Loading : ChatUiState()
    @Immutable
    data class Success(
        val messages: List<Message>,
        val otherUser: Profile? = null
    ) : ChatUiState()
    @Immutable
    data class Error(val message: String) : ChatUiState()
}

enum class RecordState {
    IDLE,
    RECORDING,
    LOCKED_RECORDING,
    PREVIEWING,
    CANCELING
}

class ChatViewModel : ViewModel() {
    private val messagesRepo = MessagesRepository.getInstance()
    private val publicProfileRepo = com.example.data.repository.PublicProfileRepository.getInstance()
    private val chatsRepo = com.example.data.repository.ChatsRepository()
    private val playlistRepo = com.example.media.playlist.PlaylistRepository(
        com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance).playlistDao(),
        com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance).collaboratorDao()
    )

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _myPlaylists = MutableStateFlow<List<com.example.media.playlist.PlaylistEntity>>(emptyList())
    val myPlaylists: StateFlow<List<com.example.media.playlist.PlaylistEntity>> = _myPlaylists.asStateFlow()

    fun loadMyPlaylists() {
        viewModelScope.launch {
            playlistRepo.getPlaylistsByUser(currentUserId).collect {
                _myPlaylists.value = it
            }
        }
    }

    val currentUserId: String
        get() = com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""

    val currentAvatarUrl: String?
        get() = com.example.data.supabase.SupabaseClient.currentProfile?.avatarUrl

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _isGhostMode = MutableStateFlow(false)
    val isGhostMode: StateFlow<Boolean> = _isGhostMode.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _userPresence = MutableStateFlow<Map<String, String>>(emptyMap())
    val userPresence: StateFlow<Map<String, String>> = _userPresence.asStateFlow()

    init {
        viewModelScope.launch {
            com.example.data.repository.PresenceRepository.presenceMap.collect { presenceMap ->
                val stringMap = presenceMap.mapValues { entry ->
                    val info = entry.value
                    if (info.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) {
                        info.secondaryStatus.label
                    } else {
                        com.example.util.PresenceTimeFormatter.formatLastSeen(
                            status = info.status,
                            lastSeenMs = info.lastSeen
                        )
                    }
                }
                _userPresence.value = stringMap
            }
        }
    }

    private val _messageReactions = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val messageReactions: StateFlow<Map<String, Map<String, String>>> = _messageReactions.asStateFlow()

    private val _editedMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val editedMessages: StateFlow<Map<String, String>> = _editedMessages.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<Message?>(null)
    val replyingToMessage: StateFlow<Message?> = _replyingToMessage.asStateFlow()

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    private val _recordState = MutableStateFlow(RecordState.IDLE)
    val recordState: StateFlow<RecordState> = _recordState.asStateFlow()

    private val _voiceAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val voiceAmplitudes: StateFlow<List<Float>> = _voiceAmplitudes.asStateFlow()

    private val _previewPlayerState = MutableStateFlow(AudioPlayerState())
    val previewPlayerState: StateFlow<AudioPlayerState> = _previewPlayerState.asStateFlow()

    private val _previewWaveform = MutableStateFlow<List<Float>>(emptyList())
    val previewWaveform: StateFlow<List<Float>> = _previewWaveform.asStateFlow()

    var previewFile: java.io.File? = null
    var previewDurationSeconds: Int = 0

    private val _playNotificationSound = MutableSharedFlow<Unit>()
    val playNotificationSound: SharedFlow<Unit> = _playNotificationSound

    private val _playOutgoingSound = MutableSharedFlow<Unit>()
    val playOutgoingSound: SharedFlow<Unit> = _playOutgoingSound

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentChatId: String? = null
    private var currentOtherUserId: String? = null
    private var audioRecorder: com.example.util.AudioRecorder? = null

private var chatJob: kotlinx.coroutines.Job? = null
    private var typingJob: kotlinx.coroutines.Job? = null

    fun loadChatHistory(chatId: String, otherUserId: String) {
        currentChatId = chatId
        currentOtherUserId = otherUserId
        
        // Mark active chat globally in SupabaseClient for realtime read updates
        com.example.data.supabase.SupabaseClient.activeChatId = chatId
        com.example.data.supabase.SupabaseClient.isChatScreenActive = true

        // Mark thread read & delivered
        viewModelScope.launch {
            try {
                messagesRepo.markThreadDelivered(chatId)
                messagesRepo.markThreadRead(chatId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking thread read/delivered on load", e)
            }
        }

        // Collect typing status updates for this chat
        typingJob?.cancel()
        _typingUsers.value = emptyList()
        typingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.data.supabase.SupabaseClient.realtimeTyping.collect { status ->
                    if (status.chatId == chatId) {
                        val currentList = _typingUsers.value.toMutableList()
                        if (status.isTyping) {
                            if (!currentList.contains(status.userId)) {
                                currentList.add(status.userId)
                            }
                            // Auto-timeout typing indicator after 5 seconds
                            launch {
                                kotlinx.coroutines.delay(5000)
                                val listAfterDelay = _typingUsers.value.toMutableList()
                                if (listAfterDelay.contains(status.userId)) {
                                    listAfterDelay.remove(status.userId)
                                    _typingUsers.value = listAfterDelay
                                }
                            }
                        } else {
                            currentList.remove(status.userId)
                        }
                        _typingUsers.value = currentList
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore normal cancellation
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error collecting typing status", e)
            }
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            // 1. Load local profile first from PublicProfileRepository / Room (Offline-First)
            var currentOtherProfile: Profile? = null
            if (otherUserId.isNotBlank()) {
                val cachedPublic = publicProfileRepo.getPublicProfile(otherUserId, forceRefresh = false)
                if (cachedPublic is com.example.data.repository.PublicProfileFetchResult.Success) {
                    val pub = cachedPublic.data
                    currentOtherProfile = com.example.data.repository.PublicProfileResolver.toProfile(pub)
                }
            }

            // 2. Load cached messages list
            val cachedMessages = messagesRepo.getCachedMessages(chatId)

            if (cachedMessages.isNotEmpty()) {
                // Instantly emit success with the local cache! No spinner!
                _uiState.value = ChatUiState.Success(
                    messages = cachedMessages,
                    otherUser = currentOtherProfile
                )
            } else {
                // No local messages, default to loading
                _uiState.value = ChatUiState.Loading
            }

            // 3. Launch background job to fetch remote public profile and update cache & State
            if (otherUserId.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val result = publicProfileRepo.getPublicProfile(otherUserId, forceRefresh = true)
                        if (result is com.example.data.repository.PublicProfileFetchResult.Success) {
                            val pub = result.data
                            val remoteProfile = com.example.data.repository.PublicProfileResolver.toProfile(pub)
                            currentOtherProfile = remoteProfile
                            
                            // Update UI state if it's currently Success
                            val currentState = _uiState.value
                            if (currentState is ChatUiState.Success) {
                                _uiState.value = currentState.copy(otherUser = remoteProfile)
                            }
                        } else if (result is com.example.data.repository.PublicProfileFetchResult.NotFound) {
                            Log.w("ChatViewModel", "Public profile not found for $otherUserId")
                        } else {
                            Log.w("ChatViewModel", "Public profile fetch error for $otherUserId: $result")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error updating remote profile", e)
                    }
                }
            }

            // 4. Collect the Flow from Room local database in real-time
            messagesRepo.getMessagesFlow(chatId).collect { messages ->
                _uiState.value = ChatUiState.Success(
                    messages = messages,
                    otherUser = currentOtherProfile
                )
            }
        }
        
        // Fetch remote messages in background to update local DB on opening chat
        viewModelScope.launch(Dispatchers.IO) {
            try {
                messagesRepo.getMessagesForChatPaged(chatId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching remote paged messages", e)
            }
        }
    }

    fun loadMoreMessages(chatId: String) {
        val currentState = _uiState.value
        if (currentState !is ChatUiState.Success) return
        if (_isLoadingMore.value) return

        val currentCount = currentState.messages.size
        if (currentCount < 10) return

        _isLoadingMore.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldestTimestamp = currentState.messages.firstOrNull()?.createdAt
                messagesRepo.getMessagesForChatPaged(chatId, limit = 50, oldestTimestamp = oldestTimestamp)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading more paged messages", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    fun sharePlaylist(chatId: String, playlist: com.example.media.playlist.PlaylistEntity) {
        viewModelScope.launch {
            messagesRepo.sendPlaylistShareMessage(
                chatId = chatId,
                playlistId = playlist.id,
                playlistName = playlist.name,
                coverUrl = playlist.coverPath
            )
        }
    }

    fun markMessagesAsRead(visibleIds: List<String>) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            try {
                messagesRepo.markThreadRead(chatId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking visible messages as read", e)
            }
        }
    }
    fun sendTypingStatus(isTyping: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.supabase.SupabaseClient.sendTypingStatus(chatId, isTyping)
        }
    }
    
    fun setReplyingToMessage(msg: Message?) {
        _replyingToMessage.value = msg
    }

    fun setEditingMessage(msg: Message?) {
        _editingMessage.value = msg
    }

    fun clearReplyAndEdit() {
        _replyingToMessage.value = null
        _editingMessage.value = null
    }

    fun onInputMessageChange(text: String) {
        _inputMessage.value = text
    }

    fun clearActiveChat() {
        com.example.data.supabase.SupabaseClient.activeChatId = null
        com.example.data.supabase.SupabaseClient.isChatScreenActive = false
        currentChatId = null
        currentOtherUserId = null
        chatJob?.cancel()
        chatJob = null
        typingJob?.cancel()
        typingJob = null
        _typingUsers.value = emptyList()
    }

    fun sendMessage(text: String, replyToId: String? = null) {
        val chatId = currentChatId ?: return
        val otherId = currentOtherUserId
        viewModelScope.launch {
            messagesRepo.sendMessage(chatId, text, replyToId = replyToId, receiverUid = otherId, isGhost = _isGhostMode.value)
            _inputMessage.value = ""
        }
    }

    fun sendPlaylistMessage(payload: com.example.media.playlist.PlaylistSharePayload) {
        val chatId = currentChatId ?: return
        val otherId = currentOtherUserId
        val json = Json.encodeToString(payload)
        viewModelScope.launch {
            messagesRepo.sendMessage(
                chatId = chatId,
                content = json,
                receiverUid = otherId,
                messageType = "playlist",
                mediaUrl = payload.coverPath,
                duration = payload.durationMs
            )
        }
    }

    fun deleteMessageDefinitively(id: String) {
        viewModelScope.launch {
            messagesRepo.deleteMessageDefinitively(id)
        }
    }

    fun deleteMessageForMe(id: String) {
        viewModelScope.launch {
            messagesRepo.deleteMessageForMe(id)
        }
    }

    fun deleteMessageForEveryone(id: String) {
        viewModelScope.launch {
            messagesRepo.deleteMessageForEveryone(id)
        }
    }

    fun addReaction(msgId: String, emoji: String) {
        val chatId = currentChatId ?: return
        val myUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: return
        viewModelScope.launch {
            messagesRepo.saveReaction(msgId, chatId, myUserId, emoji)
        }
    }

    fun clearChat() {
        val chatId = currentChatId ?: return
        val current = _uiState.value
        if (current is ChatUiState.Success) {
            _uiState.value = current.copy(messages = emptyList())
        }
        viewModelScope.launch {
            messagesRepo.clearChat(chatId)
        }
    }

    fun deleteChat(onBack: () -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatsRepo.deleteChatLocallyAndRemotely(chatId)
            onBack()
        }
    }
    
    fun toggleFavorite(msg: Message) {
        viewModelScope.launch {
            messagesRepo.toggleMessageFavorite(msg)
        }
    }

    fun toggleGhostMode() {
        _isGhostMode.value = !_isGhostMode.value
    }
    
    fun consumeGhostMessage(id: String) {
        viewModelScope.launch {
            messagesRepo.consumeGhostMessage(id)
        }
    }

    fun onVoiceGestureEvent(
        event: com.example.ui.components.chat.voice.VoiceGestureEvent,
        context: Context,
        replyToId: String? = null,
        fallbackDurationSeconds: Int = 0,
        onProgress: (Boolean) -> Unit = {}
    ) {
        if (audioRecorder == null) {
            audioRecorder = com.example.util.AudioRecorder(context)
        }
        
        when (event) {
            com.example.ui.components.chat.voice.VoiceGestureEvent.StartRecording -> {
                audioRecorder?.startRecording()
                _recordState.value = RecordState.RECORDING
            }
            com.example.ui.components.chat.voice.VoiceGestureEvent.LockRecording -> {
                _recordState.value = RecordState.LOCKED_RECORDING
            }
            com.example.ui.components.chat.voice.VoiceGestureEvent.CancelRecording -> {
                audioRecorder?.cancelRecording()
                _recordState.value = RecordState.IDLE
            }
            com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording -> {
                val file = audioRecorder?.stopRecording()
                _recordState.value = RecordState.IDLE
                if (file != null) {
                    uploadAndSendMedia(
                        file = file,
                        mimeType = "audio/mp4",
                        typeLabel = "Voice",
                        replyToId = replyToId,
                        context = context,
                        onProgress = onProgress
                    )
                }
            }
            else -> {}
        }
    }

    fun cancelPreviewRecording() {}
    fun getRecordingElapsedSeconds(): Int = 0
    fun cleanOldCache(context: Context) {}
    
    fun pausePreviewAudio() {}
    fun playPreviewAudio() {}
    fun togglePreviewSpeed() {}
    fun seekPreviewAudio(posMs: Long) {}

    fun sendPreviewRecording(
        context: Context,
        replyToId: String? = null,
        onProgress: (Boolean) -> Unit = {}
    ) {}
    
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

fun uploadAndSendMedia(
        uri: android.net.Uri? = null,
        file: java.io.File? = null,
        mimeType: String,
        typeLabel: String,
        replyToId: String?,
        context: android.content.Context,
        fileName: String? = null,
        onProgress: (Boolean) -> Unit
    ) {
        val chatId = currentChatId ?: return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            onProgress(true)
            
            val result = messagesRepo.sendMultimediaMessage(
                chatId = chatId,
                context = context,
                sourceUri = uri,
                sourceFile = file,
                mimeType = mimeType,
                typeLabel = typeLabel,
                content = "[$typeLabel]",
                replyToId = replyToId,
                isGhost = _isGhostMode.value,
                receiverId = currentOtherUserId
            )
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onProgress(false)
                if (result.isFailure) {
                    try {
                        android.widget.Toast.makeText(
                            context,
                            "Error procesando archivo",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (t: Throwable) {}
                }
            }
        }
    }
    
    fun editMessage(id: String, newContent: String) {
        viewModelScope.launch {
            messagesRepo.editMessage(id, newContent)
        }
    }

fun sendSticker(url: String, preview: String?, replyToId: String?) {
        val chatId = currentChatId ?: return
        val otherUserId = currentOtherUserId
        val isGif = url.lowercase().contains(".gif")
        viewModelScope.launch {
            val msgId = "temp_${java.util.UUID.randomUUID()}"
            val nowStr = com.example.data.supabase.SupabaseClient.getNowIsoString()
            val mType = if (isGif) "gif" else "sticker"
            val mMime = if (isGif) "image/gif" else "image/webp"
            
            // Optimistic UI for Sticker/GIF
            val optimisticMsg = com.example.data.model.Message(
                id = msgId,
                chatId = chatId,
                senderId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: "",
                content = if (isGif) "[GIF]" else "[Sticker]",
                createdAt = nowStr,
                status = "sending",
                replyToMessageId = replyToId,
                mediaUrl = url,
                thumbnailUrl = preview ?: url,
                mediaMime = mMime,
                messageType = mType,
                isGhost = _isGhostMode.value
            )
            messagesRepo.insertLocalMessage(optimisticMsg)
            
            messagesRepo.sendMessage(
                chatId = chatId,
                content = optimisticMsg.content ?: "",
                replyToId = replyToId,
                receiverUid = otherUserId,
                messageType = mType,
                mediaUrl = url,
                thumbnailUrl = preview ?: url,
                mediaMime = mMime,
                isGhost = _isGhostMode.value,
                messageId = msgId
            )
        }
    }

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    fun loadChatMuteStatus(chatId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chat = chatsRepo.getLocalChat(chatId)
                _isMuted.value = chat?.isMuted == true
                _isPinned.value = chat?.isPinned == true
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading mute status: ${e.localizedMessage}")
            }
        }
    }

    fun loadChatPinStatus(chatId: String, context: Context) {
        loadChatMuteStatus(chatId, context)
    }

    fun muteChat(chatId: String, muted: Boolean) {
        _isMuted.value = muted
        viewModelScope.launch(Dispatchers.IO) {
            chatsRepo.muteChat(chatId, muted)
        }
    }

    fun pinChat(chatId: String, pinned: Boolean) {
        _isPinned.value = pinned
        viewModelScope.launch(Dispatchers.IO) {
            chatsRepo.pinChat(chatId, pinned)
        }
    }

    fun saveSticker(stickerUrl: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.model.StickerResult(url = stickerUrl, preview = stickerUrl)
            StickerRepository.saveSticker(context, result)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Sticker guardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleStickerFavorite(stickerUrl: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.model.StickerResult(url = stickerUrl, preview = stickerUrl)
            val isFav = StickerRepository.toggleFavoriteSticker(context, result)
            withContext(Dispatchers.Main) {
                val msg = if (isFav) "Añadido a favoritos" else "Quitado de favoritos"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
