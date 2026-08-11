package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val content: String = "",
    val privacy: String = "PUBLIC", // "PUBLIC" o "PANAS"
    val selectedMediaUris: List<Uri> = emptyList(),
    val isAudioPost: Boolean = false,
    val isPublishing: Boolean = false,
    val preview: com.example.data.model.MediaPreview? = null
)

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {
    private val errorHandler = com.example.util.Resilience.globalExceptionHandler("CreatePostViewModel")
    private val youtubeRepo = com.example.data.repository.YouTubeLinkPreviewRepository()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun fetchLinkPreview(url: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val preview = youtubeRepo.fetchPreview(url)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _uiState.update { it.copy(preview = preview) }
            }
        }
    }
    
    // ... existing functions ...
    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onPrivacyChanged(newPrivacy: String) {
        _uiState.update { it.copy(privacy = newPrivacy) }
    }

    fun addMediaUris(uris: List<Uri>) {
        _uiState.update {
            it.copy(
                selectedMediaUris = (it.selectedMediaUris + uris).distinct(),
                isAudioPost = false
            )
        }
    }

    fun setAudioUri(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedMediaUris = (it.selectedMediaUris + uri).distinct(),
                isAudioPost = true
            )
        }
    }

    fun addAudioUris(uris: List<Uri>) {
        _uiState.update {
            it.copy(
                selectedMediaUris = (it.selectedMediaUris + uris).distinct(),
                isAudioPost = true
            )
        }
    }

    fun removeMediaUri(uri: Uri) {
        _uiState.update {
            val updated = it.selectedMediaUris - uri
            it.copy(
                selectedMediaUris = updated,
                isAudioPost = if (updated.isEmpty()) false else it.isAudioPost
            )
        }
    }

    fun publishPost(currentUserId: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        val userId = if (currentUserId.isNotBlank()) currentUserId else com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
        
        // Extract YouTube Video ID from preview or directly from content text
        val extractedVideoId = state.preview?.videoId 
            ?: com.example.util.YouTubeUrlParser.extractYouTubeVideoId(state.content)

        val finalPreview = if (state.preview != null) {
            state.preview
        } else if (!extractedVideoId.isNullOrBlank()) {
            com.example.data.model.MediaPreview(
                provider = "youtube",
                videoId = extractedVideoId,
                title = "Video de YouTube",
                thumbnailUrl = "https://img.youtube.com/vi/$extractedVideoId/hqdefault.jpg",
                embedUrl = "https://www.youtube.com/embed/$extractedVideoId"
            )
        } else null

        if (state.content.isBlank() && state.selectedMediaUris.isEmpty() && finalPreview == null) return

        // Determinar el tipo de post
        val postType = when {
            finalPreview != null -> "YOUTUBE"
            state.isAudioPost -> "AUDIO"
            state.selectedMediaUris.isNotEmpty() -> "ALBUM"
            else -> "TEXT"
        }

        val db = com.example.data.database.PanalinkDatabase.getDatabase(getApplication())
        val pendingPostDao = db.pendingPostDao()

        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val pendingPostId = java.util.UUID.randomUUID().toString()
            val serverPostId = java.util.UUID.randomUUID().toString()
            val mediaUrisJson = org.json.JSONArray(state.selectedMediaUris.map { it.toString() }).toString()
            
            // Serialize preview to JSON
            val previewDataJson = finalPreview?.let { 
                org.json.JSONObject().apply {
                    put("provider", it.provider)
                    put("video_id", it.videoId)
                    put("title", it.title)
                    put("thumbnail_url", it.thumbnailUrl)
                    put("embed_url", it.embedUrl)
                }.toString()
            }

            val pendingPost = com.example.data.database.PendingPostEntity(
                id = pendingPostId,
                userId = userId,
                content = state.content.ifBlank { null },
                type = postType,
                mediaUrisJson = mediaUrisJson,
                privacy = state.privacy,
                status = "pending",
                previewDataJson = previewDataJson
            )

            pendingPostDao.insertPost(pendingPost)

            val workManager = WorkManager.getInstance(getApplication())
            val inputData = workDataOf(
                "pendingPostId" to pendingPostId,
                "serverPostId" to serverPostId
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadWorkRequest = OneTimeWorkRequestBuilder<com.example.worker.PostUploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            workManager.enqueue(uploadWorkRequest)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _uiState.value = CreatePostUiState()
                onSuccess()
            }
        }
    }
}
