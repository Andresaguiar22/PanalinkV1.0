package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.model.PostDto
import com.example.data.repository.FeedRepository
import com.example.data.repository.FeedRepositoryImpl
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class FeedUiState(
    val isLoading: Boolean = false,
    val posts: List<PostDto> = emptyList(),
    val pendingPosts: List<com.example.data.database.PendingPostEntity> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val uploadProgress: Float? = null
)

class FeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val errorHandler = com.example.util.Resilience.globalExceptionHandler("FeedViewModel")

    private val feedRepository: FeedRepository by lazy { FeedRepositoryImpl() }
    private val chatsRepository: com.example.data.repository.ChatsRepository by lazy { com.example.data.repository.ChatsRepository() }

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val db by lazy { com.example.data.database.PanalinkDatabase.getDatabase(application) }
    private val pendingPostDao by lazy { db.pendingPostDao() }

    private val localLimit = MutableStateFlow(20)

    init {
        observeLocalFeed()
        loadFeed()
        observePendingPosts()
        observeUploadProgress()
        observeUploadSuccess()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLocalFeed() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            localLimit.flatMapLatest { limit ->
                feedRepository.getLocalPostsFlow(limit)
            }.collect { localPosts ->
                _uiState.value = _uiState.value.copy(
                    posts = localPosts,
                    hasMore = localPosts.size >= localLimit.value
                )
                
                // If a post is selected, update details reactively
                val selected = _selectedPostDetail.value
                if (selected != null) {
                    val updatedSelected = localPosts.find { it.id == selected.id }
                    if (updatedSelected != null) {
                        _selectedPostDetail.value = updatedSelected
                    }
                }
            }
        }
    }

    private fun observeUploadSuccess() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.repository.UploadRepository.uploadSuccessEvent.collect {
                // Background refresh when a post is fully uploaded
                refreshFeed()
            }
        }
    }

    private fun observePendingPosts() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            pendingPostDao.getActivePostsFlow().collect { pending ->
                _uiState.value = _uiState.value.copy(pendingPosts = pending)
            }
        }
    }

    private fun observeUploadProgress() {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.repository.UploadRepository.globalUploadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }
        }
    }

    fun loadFeed() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            feedRepository.getFeed(limit = localLimit.value)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshFeed() {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            localLimit.value = 20
            feedRepository.getFeed(limit = 20)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasMore || currentState.posts.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        val lastCreatedAt = currentState.posts.last().createdAt

        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            localLimit.value = localLimit.value + 20
            feedRepository.getFeed(limit = 20, lastCreatedAt = lastCreatedAt)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private val _postComments = MutableStateFlow<Map<String, List<com.example.data.model.PostCommentDto>>>(emptyMap())
    val postComments: StateFlow<Map<String, List<com.example.data.model.PostCommentDto>>> = _postComments.asStateFlow()

    private val commentJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    fun loadComments(postId: String) {
        if (commentJobs.containsKey(postId)) return
        val job = viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            feedRepository.getCommentsForPost(postId)
            feedRepository.getCommentsFlow(postId).collect { comments ->
                val sortedComments = comments.sortedByDescending { it.createdAt }
                val current = _postComments.value.toMutableMap()
                current[postId] = sortedComments
                _postComments.value = current
            }
        }
        commentJobs[postId] = job
    }

    fun addComment(postId: String, content: String, onSuccess: () -> Unit) {
        val currentUserId = SupabaseClient.currentUser?.id ?: return
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.addComment(postId, currentUserId, content)
            if (result.isSuccess) {
                loadComments(postId)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    fun toggleLike(post: PostDto) {
        val currentUserId = SupabaseClient.currentUser?.id ?: return
        val isCurrentlyLiked = post.isLikedByMe
        
        viewModelScope.launch(errorHandler) {
            feedRepository.toggleLike(post.id!!, currentUserId, isCurrentlyLiked)
        }
    }

    fun createPost(content: String, type: String = "TEXT", privacy: String = "PUBLIC", mediaUris: List<String> = emptyList()) {
        val currentUserId = SupabaseClient.currentUser?.id ?: return
        
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val pendingPostId = java.util.UUID.randomUUID().toString()
            val serverPostId = java.util.UUID.randomUUID().toString() // Pre-generate server ID
            val mediaUrisJson = org.json.JSONArray(mediaUris).toString()
            
            val pendingPost = com.example.data.database.PendingPostEntity(
                id = pendingPostId,
                userId = currentUserId,
                content = content,
                type = type,
                mediaUrisJson = mediaUrisJson,
                privacy = privacy,
                status = "pending"
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
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            feedRepository.deletePost(postId)
        }
    }

    fun updatePost(postId: String, content: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            feedRepository.updatePost(postId, content)
        }
    }

    private val _selectedPostDetail = MutableStateFlow<PostDto?>(null)
    val selectedPostDetail: StateFlow<PostDto?> = _selectedPostDetail.asStateFlow()

    private val _selectedPostLoading = MutableStateFlow(false)
    val selectedPostLoading: StateFlow<Boolean> = _selectedPostLoading.asStateFlow()

    fun getPostDetail(postId: String) {
        _selectedPostLoading.value = true
        _selectedPostDetail.value = null
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.getPostById(postId)
            if (result.isSuccess) {
                _selectedPostDetail.value = result.getOrNull()
                loadComments(postId)
            } else {
                android.util.Log.e("FeedViewModel", "Error fetching post detail: ${result.exceptionOrNull()?.message}")
            }
            _selectedPostLoading.value = false
        }
    }
}
