import re

content = """package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
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
    private val profilesRepo = ProfilesRepository()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _isGhostMode = MutableStateFlow(false)
    val isGhostMode: StateFlow<Boolean> = _isGhostMode.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _userPresence = MutableStateFlow<Map<String, String>>(emptyMap())
    val userPresence: StateFlow<Map<String, String>> = _userPresence.asStateFlow()

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

    private var currentChatId: String? = null
    private var currentOtherUserId: String? = null
    private var audioRecorder: com.example.util.AudioRecorder? = null

    fun loadChatHistory(chatId: String, otherUserId: String) {
        currentChatId = chatId
        currentOtherUserId = otherUserId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            val messagesResult = messagesRepo.getMessagesForChat(chatId)
            val profileResult = profilesRepo.getProfile(otherUserId)
            
            if (messagesResult.isSuccess) {
                _uiState.value = ChatUiState.Success(
                    messages = messagesResult.getOrNull() ?: emptyList(),
                    otherUser = profileResult.getOrNull()
                )
            } else {
                _uiState.value = ChatUiState.Error(messagesResult.exceptionOrNull()?.message ?: "Error loading chat")
            }
        }
    }

    fun loadMoreMessages(chatId: String) {}
    fun markMessagesAsRead(visibleIds: List<String>) {}
    fun sendTypingStatus(isTyping: Boolean) {}
    
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

    fun clearActiveChat() {}

    fun sendMessage(text: String, replyToId: String? = null) {
        val chatId = currentChatId ?: return
        val otherId = currentOtherUserId
        viewModelScope.launch {
            messagesRepo.sendMessage(chatId, text, replyToId = replyToId, receiverUid = otherId, isGhost = _isGhostMode.value)
            _inputMessage.value = ""
        }
    }

    fun deleteMessageDefinitively(id: String) {
        viewModelScope.launch {
            messagesRepo.deleteMessageDefinitively(id)
        }
    }

    fun addReaction(msgId: String, emoji: String) {
        val chatId = currentChatId ?: return
        val myUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: return
        viewModelScope.launch {
            messagesRepo.saveReaction(msgId, chatId, myUserId, emoji)
        }
    }

    fun clearChat() {}
    fun deleteChat(onBack: () -> Unit) {}
    
    fun toggleFavorite(msg: Message) {
        viewModelScope.launch {
            messagesRepo.toggleMessageFavorite(msg)
        }
    }

    fun toggleGhostMode() {
        _isGhostMode.value = !_isGhostMode.value
    }
    
    fun consumeGhostMessage(id: String) {}

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
                        typeLabel = "Audio",
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
        uri: Uri? = null,
        file: java.io.File? = null,
        mimeType: String,
        typeLabel: String,
        replyToId: String?,
        context: Context,
        fileName: String? = null,
        onProgress: (Boolean) -> Unit
    ) {
        val chatId = currentChatId ?: return
        val otherUserId = currentOtherUserId
        val userId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: return
        
        val actualFile = file ?: uri?.let { getFileFromUri(context, it) } ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            onProgress(true)
            val result = com.example.util.PanalinkMediaManager.uploadMediaAndThumbnail(
                context = context,
                mediaFile = actualFile,
                mimeType = mimeType,
                typeLabel = typeLabel,
                userId = userId,
                caption = ""
            )
            if (result.isSuccess) {
                val uploadResult = result.getOrNull()!!
                messagesRepo.sendMessage(
                    chatId = chatId,
                    content = "[$typeLabel]",
                    replyToId = replyToId,
                    receiverUid = otherUserId,
                    messageType = mimeType,
                    mediaUrl = uploadResult.url,
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    mediaMime = mimeType,
                    isGhost = _isGhostMode.value
                )
            }
            withContext(Dispatchers.Main) {
                onProgress(false)
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
        viewModelScope.launch {
            messagesRepo.sendMessage(
                chatId = chatId,
                content = "[Sticker]",
                replyToId = replyToId,
                receiverUid = otherUserId,
                messageType = "image/webp",
                mediaUrl = url,
                thumbnailUrl = preview ?: url,
                isGhost = _isGhostMode.value
            )
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
"""

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
