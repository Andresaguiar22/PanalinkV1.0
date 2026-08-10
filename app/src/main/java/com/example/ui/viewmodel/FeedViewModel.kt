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
import kotlinx.coroutines.launch

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

    init {
        loadFeed()
        observePendingPosts()
        observeUploadProgress()
        observeUploadSuccess()
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
            val result = feedRepository.getFeed(limit = 20)
            if (result.isSuccess) {
                val newPosts = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posts = newPosts,
                    hasMore = newPosts.size == 20
                )
            } else {
                android.util.Log.e("FeedViewModel", "Error loading feed: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                    // Silently fail, do not show error on UI to keep experience stable
                )
            }
        }
    }

    fun refreshFeed() {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.getFeed(limit = 20)
            if (result.isSuccess) {
                val newPosts = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    posts = newPosts,
                    hasMore = newPosts.size == 20
                )
            } else {
                android.util.Log.e("FeedViewModel", "Error refreshing feed: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false
                    // Silently fail on refresh
                )
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasMore || currentState.posts.isEmpty()) return

        // _uiState.value = currentState.copy(isLoading = true, error = null)
        val lastCreatedAt = currentState.posts.last().createdAt

        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.getFeed(limit = 20, lastCreatedAt = lastCreatedAt)
            if (result.isSuccess) {
                val newPosts = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posts = currentState.posts + newPosts,
                    hasMore = newPosts.size == 20
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Error loading more feed"
                )
            }
        }
    }

    private val _postComments = MutableStateFlow<Map<String, List<com.example.data.model.PostCommentDto>>>(emptyMap())
    val postComments: StateFlow<Map<String, List<com.example.data.model.PostCommentDto>>> = _postComments.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.getCommentsForPost(postId)
            if (result.isSuccess) {
                val comments = result.getOrNull() ?: emptyList()
                val sortedComments = comments.sortedByDescending { it.createdAt }
                val current = _postComments.value.toMutableMap()
                current[postId] = sortedComments
                _postComments.value = current
            }
        }
    }

    fun addComment(postId: String, content: String, onSuccess: () -> Unit) {
        val currentUserId = SupabaseClient.currentUser?.id ?: return
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.addComment(postId, currentUserId, content)
            if (result.isSuccess) {
                // Refresh comments
                loadComments(postId)
                // Optimistically update comment count in UI state
                val currentState = _uiState.value
                val updatedPosts = currentState.posts.map { post ->
                    if (post.id == postId) post.copy(commentsCount = post.commentsCount + 1) else post
                }
                _uiState.value = currentState.copy(posts = updatedPosts)
                
                if (_selectedPostDetail.value?.id == postId) {
                    _selectedPostDetail.value = _selectedPostDetail.value?.copy(
                        commentsCount = (_selectedPostDetail.value?.commentsCount ?: 0) + 1
                    )
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    fun toggleLike(post: PostDto) {
        val currentUserId = SupabaseClient.currentUser?.id ?: return
        val currentState = _uiState.value
        val previousSelectedPost = _selectedPostDetail.value
        
        // Optimistic update
        val isCurrentlyLiked = post.isLikedByMe
        val newLikesCount = if (isCurrentlyLiked) (post.likesCount - 1).coerceAtLeast(0) else post.likesCount + 1
        
        val updatedPost = post.copy(
            isLikedByMe = !isCurrentlyLiked,
            likesCount = newLikesCount
        )
        
        val updatedPosts = currentState.posts.map {
            if (it.id == post.id) updatedPost else it
        }
        
        _uiState.value = currentState.copy(posts = updatedPosts)
        
        if (_selectedPostDetail.value?.id == post.id) {
            _selectedPostDetail.value = updatedPost
        }
        
        // Background sync
        viewModelScope.launch(errorHandler) {
            val result = feedRepository.toggleLike(post.id!!, currentUserId, isCurrentlyLiked)
            if (result.isFailure) {
                android.util.Log.e("FeedViewModel", "Failed to toggle like on server: ${result.exceptionOrNull()?.message}")
                // Revert on failure
                _uiState.value = currentState
                if (_selectedPostDetail.value?.id == post.id) {
                    _selectedPostDetail.value = previousSelectedPost
                }
            }
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
            val result = feedRepository.deletePost(postId)
            if (result.isSuccess) {
                // Remove from UI state
                val currentState = _uiState.value
                val updatedPosts = currentState.posts.filter { it.id != postId }
                // _uiState.value = currentState.copy(posts = updatedPosts)
            } else {
                android.util.Log.e("FeedViewModel", "Error deleting post: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updatePost(postId: String, content: String) {
        viewModelScope.launch(errorHandler + kotlinx.coroutines.Dispatchers.IO) {
            val result = feedRepository.updatePost(postId, content)
            if (result.isSuccess) {
                val updatedPost = result.getOrNull()
                if (updatedPost != null) {
                    // Update in UI state
                    val currentState = _uiState.value
                    val updatedPosts = currentState.posts.map {
                        if (it.id == postId) {
                            val existing = it
                            updatedPost.copy(
                                profile = existing.profile,
                                isLikedByMe = existing.isLikedByMe,
                                likesCount = existing.likesCount,
                                commentsCount = existing.commentsCount
                            )
                        } else it
                    }
                    // _uiState.value = currentState.copy(posts = updatedPosts)
                }
            } else {
                android.util.Log.e("FeedViewModel", "Error updating post: ${result.exceptionOrNull()?.message}")
            }
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
                // Also load comments for this post
                loadComments(postId)
            } else {
                android.util.Log.e("FeedViewModel", "Error fetching post detail: ${result.exceptionOrNull()?.message}")
            }
            _selectedPostLoading.value = false
        }
    }
}
