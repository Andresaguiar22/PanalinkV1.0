package com.example.data.repository.reels

import com.example.data.database.PanalinkDatabase
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.data.repository.StatesRepository
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Feature-owned Reel repository.
 *
 * Local persistence is isolated behind [ReelsLocalDataSource]. The existing
 * StatesRepository is used only as a temporary remote-sync adapter; the Reel
 * UI does not depend on it and this adapter can be replaced by a Reel-specific
 * remote data source in the next migration step.
 */
class ReelsRepository(
    private val local: ReelsLocalDataSource,
    private val remote: StatesRepository = StatesRepository()
) {
    private val db by lazy {
        PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
    }

    fun observeReels(): Flow<List<UserStateWithUser>> =
        local.observe().map { entities -> entities.map { it.toUserStateWithUser() } }

    suspend fun refresh(): Result<Unit> = remote.getActiveStates()

    suspend fun toggleLike(reelId: String, currentlyLiked: Boolean): Result<Unit> =
        remote.toggleLike(reelId, currentlyLiked, true)

    suspend fun toggleFavorite(reelId: String, currentlyFavorited: Boolean): Result<Unit> =
        remote.toggleFavorite(reelId, currentlyFavorited, true)

    suspend fun registerShare(reelId: String): Result<Unit> =
        remote.incrementShare(reelId, true)

    suspend fun registerView(reelId: String): Result<Unit> =
        remote.registerView(reelId, true)

    fun observeComments(reelId: String): Flow<List<Comment>> =
        remote.getCommentsFlow(reelId, true)

    suspend fun refreshComments(reelId: String): Result<List<Comment>> =
        remote.getStateComments(reelId, true)

    suspend fun addComment(reelId: String, text: String, parentId: String?): Result<Unit> =
        remote.addComment(reelId, text, true, parentId)

    suspend fun deleteComment(commentId: String): Result<Unit> =
        remote.deleteComment(commentId, true)

    suspend fun deleteReel(reelId: String, mediaUrl: String?): Result<Unit> = withContext(Dispatchers.IO) {
        local.deleteById(reelId)
        val result = remote.deleteUserStatus(reelId, true, mediaUrl)
        if (result.isSuccess && !mediaUrl.isNullOrBlank()) {
            try {
                com.example.data.video.VideoCacheManager.removeVideoCache(mediaUrl)
            } catch (_: Exception) {
                // Cache cleanup is best-effort; the remote deletion already succeeded.
            }
        }
        result
    }

    suspend fun saveReelLocally(reel: UserStateWithUser) {
        local.save(com.example.data.database.StateEntity.fromUserStateWithUser(reel))
    }

    suspend fun getLocalReel(reelId: String): UserStateWithUser? =
        local.getById(reelId)?.toUserStateWithUser()

    suspend fun clearLocalReel(reelId: String) {
        local.deleteById(reelId)
    }

    fun currentUserId(): String? = SupabaseClient.currentUser?.id
}
