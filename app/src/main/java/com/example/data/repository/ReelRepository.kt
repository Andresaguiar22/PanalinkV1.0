package com.example.data.repository

import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import kotlinx.coroutines.flow.Flow

/**
 * Reel-only data facade.
 *
 * Reels are backed by the existing states persistence layer for now, but the UI
 * no longer needs to know about StatesRepository/StatesViewModel or the story
 * code path. Every operation in this facade is explicitly reel-scoped.
 */
class ReelRepository(
    private val statesRepository: StatesRepository = StatesRepository()
) {
    fun observeReels(): Flow<List<UserStateWithUser>> =
        statesRepository.getLocalStatesFlow(isReel = true)

    suspend fun refresh(): Result<Unit> =
        statesRepository.getActiveStates()

    suspend fun toggleLike(reelId: String, currentlyLiked: Boolean): Result<Unit> =
        statesRepository.toggleLike(reelId, currentlyLiked, true)

    suspend fun toggleFavorite(reelId: String, currentlyFavorited: Boolean): Result<Unit> =
        statesRepository.toggleFavorite(reelId, currentlyFavorited, true)

    suspend fun registerShare(reelId: String): Result<Unit> =
        statesRepository.incrementShare(reelId, true)

    suspend fun registerView(reelId: String): Result<Unit> =
        statesRepository.registerView(reelId, true)

    fun observeComments(reelId: String): Flow<List<Comment>> =
        statesRepository.getCommentsFlow(reelId, true)

    suspend fun refreshComments(reelId: String): Result<List<Comment>> =
        statesRepository.getStateComments(reelId, true)

    suspend fun addComment(
        reelId: String,
        text: String,
        parentId: String? = null
    ): Result<Unit> =
        statesRepository.addComment(reelId, text, true, parentId)

    suspend fun deleteComment(commentId: String): Result<Unit> =
        statesRepository.deleteComment(commentId, true)

    suspend fun deleteReel(reelId: String, mediaUrl: String?): Result<Unit> =
        statesRepository.deleteUserStatus(reelId, true, mediaUrl)

    suspend fun saveReelLocally(reel: UserStateWithUser) =
        statesRepository.saveStateLocally(reel)
}
