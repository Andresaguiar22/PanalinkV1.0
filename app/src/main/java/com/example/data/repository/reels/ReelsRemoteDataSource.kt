package com.example.data.repository.reels

import com.example.data.database.CommentEntity
import com.example.data.database.PanalinkDatabase
import com.example.data.model.Comment
import com.example.data.repository.PublicProfileFetchResult
import com.example.data.repository.PublicProfileRepository
import com.example.data.repository.PublicProfileResolver
import com.example.data.repository.StatesRepository
import com.example.data.supabase.SupabaseClient

/**
 * Remote data source for Reels, abstracting Supabase interactions.
 *
 * Comment authors are explicitly enriched from the canonical public profile
 * store before being exposed to the UI and persisted to Room. This prevents
 * the comments sheet from falling back to generic names such as "Pana" or
 * losing the user's avatar after a refresh/restart.
 */
class ReelsRemoteDataSource(
    private val remote: StatesRepository = StatesRepository()
) {
    private val db by lazy {
        PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
    }

    suspend fun getActiveStates() = remote.getActiveStates()

    suspend fun toggleLike(reelId: String, currentlyLiked: Boolean) =
        remote.toggleLike(reelId, currentlyLiked, true).map { }

    suspend fun toggleFavorite(reelId: String, currentlyFavorited: Boolean) =
        remote.toggleFavorite(reelId, currentlyFavorited, true).map { }

    suspend fun registerShare(reelId: String) = remote.incrementShare(reelId, true)

    suspend fun registerView(reelId: String) = remote.registerView(reelId, true)

    fun observeComments(reelId: String) = remote.getCommentsFlow(reelId, true)

    suspend fun refreshComments(reelId: String): Result<List<Comment>> {
        val result = remote.getStateComments(reelId, true)
        if (result.isFailure) return result

        val comments = result.getOrDefault(emptyList())
        if (comments.isEmpty()) return Result.success(comments)

        // Resolve every author through public_profiles. The remote comment
        // payload may not always include an embedded profiles object, so this
        // is intentionally a second authoritative enrichment step.
        val authorIds = comments.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (authorIds.isEmpty()) return Result.success(comments)

        return try {
            val profileResult = PublicProfileRepository.getInstance().getPublicProfiles(authorIds)
            val profiles = if (profileResult is PublicProfileFetchResult.Success) {
                profileResult.data.mapNotNull { (id, value) ->
                    if (value is PublicProfileFetchResult.Success) id to value.data else null
                }.toMap()
            } else emptyMap()

            if (profiles.isEmpty()) return Result.success(comments)

            val enriched = comments.map { comment ->
                val publicProfile = profiles[comment.userId]
                if (publicProfile == null) {
                    comment
                } else {
                    comment.copy(
                        authorName = PublicProfileResolver.resolveDisplayName(
                            publicProfile,
                            comment.authorName.takeIf { it.isNotBlank() },
                            comment.userId
                        ),
                        avatarUrl = publicProfile.avatarUrl ?: comment.avatarUrl
                    )
                }
            }

            // Persist the enriched identity so the comments sheet remains
            // correct when the application is reopened without a network hit.
            val entities = enriched.map { CommentEntity.fromStateComment(it, isReel = true) }
            db.commentDao().upsertAll(entities)

            Result.success(enriched)
        } catch (e: Exception) {
            // The already cached comments remain usable; do not fail the
            // entire comments load only because profile enrichment failed.
            Result.success(comments)
        }
    }

    suspend fun addComment(reelId: String, text: String, parentId: String?) =
        remote.addComment(reelId, text, true, parentId)

    suspend fun deleteComment(commentId: String) = remote.deleteComment(commentId, true)

    suspend fun deleteReel(reelId: String, mediaUrl: String?) = remote.deleteUserStatus(reelId, true, mediaUrl)
}
