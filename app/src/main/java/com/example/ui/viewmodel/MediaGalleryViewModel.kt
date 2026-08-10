package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.PanalinkDatabase
import com.example.ui.components.chat.gallery.MediaGalleryItem
import com.example.ui.components.chat.gallery.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class MediaGalleryUiState {
    object Loading : MediaGalleryUiState()
    data class Success(
        val photos: List<MediaGalleryItem>,
        val videos: List<MediaGalleryItem>,
        val documents: List<MediaGalleryItem>,
        val audios: List<MediaGalleryItem>
    ) : MediaGalleryUiState()
    object Empty : MediaGalleryUiState()
    data class Error(val message: String) : MediaGalleryUiState()
}

class MediaGalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PanalinkDatabase.getDatabase(application)
    private val messageDao = database.messageDao()

    private val _uiState = MutableStateFlow<MediaGalleryUiState>(MediaGalleryUiState.Loading)
    val uiState: StateFlow<MediaGalleryUiState> = _uiState.asStateFlow()

    private val dateParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun loadMedia(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                messageDao.getMessagesForChatFlow(chatId)
                    .map { entities ->
                        entities.filter { !it.mediaUrl.isNullOrEmpty() }
                            .map { entity ->
                                val type = when (entity.messageType?.lowercase()) {
                                    "image" -> MediaType.IMAGE
                                    "video" -> MediaType.VIDEO
                                    "document" -> MediaType.DOCUMENT
                                    "audio" -> MediaType.AUDIO
                                    else -> MediaType.IMAGE // fallback
                                }
                                
                                val timestamp = try {
                                    dateParser.parse(entity.createdAt)?.time ?: 0L
                                } catch (e: Exception) {
                                    0L
                                }

                                MediaGalleryItem(
                                    id = entity.id,
                                    messageId = entity.id,
                                    type = type,
                                    url = entity.mediaUrl ?: "",
                                    createdAt = timestamp,
                                    senderId = entity.senderId,
                                    thumbnailUrl = entity.thumbnailUrl
                                )
                            }
                    }
                    .collect { items ->
                        if (items.isEmpty()) {
                            _uiState.value = MediaGalleryUiState.Empty
                        } else {
                            _uiState.value = MediaGalleryUiState.Success(
                                photos = items.filter { it.type == MediaType.IMAGE },
                                videos = items.filter { it.type == MediaType.VIDEO },
                                documents = items.filter { it.type == MediaType.DOCUMENT },
                                audios = items.filter { it.type == MediaType.AUDIO }
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = MediaGalleryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
