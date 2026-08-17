package com.example.data.repository.reels

import com.example.data.repository.StatesRepository
import com.example.data.model.Comment

/**
 * Remote data source for Reels, abstracting Supabase interactions.
 */
class ReelsRemoteDataSource(
    private val remote: StatesRepository = StatesRepository()
) {
    suspend fun getActiveStates() = remote.getActiveStates()

    suspend fun toggleLike(reelId: String, currentlyLiked: Boolean) = 
        remote.toggleLike(reelId, currentlyLiked, true).map { }

    suspend fun toggleFavorite(reelId: String, currentlyFavorited: Boolean) =
        remote.toggleFavorite(reelId, currentlyFavorited, true).map { }

    suspend fun registerShare(reelId: String) = remote.incrementShare(reelId, true)

    suspend fun registerView(reelId: String) = remote.registerView(reelId, true)

    fun observeComments(reelId: String) = remote.getCommentsFlow(reelId, true)

    suspend fun refreshComments(reelId: String) = remote.getStateComments(reelId, true)

    suspend fun addComment(reelId: String, text: String, parentId: String?) =
        remote.addComment(reelId, text, true, parentId)

    suspend fun deleteComment(commentId: String) = remote.deleteComment(commentId, true)

    suspend fun deleteReel(reelId: String, mediaUrl: String?) = remote.deleteUserStatus(reelId, true, mediaUrl)
}
