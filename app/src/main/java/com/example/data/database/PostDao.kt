package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM local_posts ORDER BY createdAt DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM local_posts ORDER BY createdAt DESC LIMIT :limit")
    fun getPostsFlow(limit: Int): Flow<List<PostEntity>>

    @Query("SELECT * FROM local_posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM local_posts ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getPosts(limit: Int): List<PostEntity>

    @Query("SELECT * FROM local_posts WHERE createdAt < :lastCreatedAt ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getPostsPaged(limit: Int, lastCreatedAt: String): List<PostEntity>

    @Upsert
    suspend fun upsert(entity: PostEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM pending_social_actions WHERE targetId = :postId AND status = 'pending' AND actionType IN ('LIKE', 'UNLIKE'))")
    suspend fun hasPendingLikeAction(postId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM pending_social_actions WHERE targetId = :postId AND status = 'pending' AND actionType = 'COMMENT')")
    suspend fun hasPendingCommentAction(postId: String): Boolean

    /**
     * Refreshes posts from the server without overwriting local optimistic
     * engagement while the corresponding durable action is still pending.
     */
    @Transaction
    suspend fun upsertAll(entities: List<PostEntity>) {
        entities.forEach { remote ->
            val local = getPostById(remote.id)
            val preserveLike = hasPendingLikeAction(remote.id)
            val preserveComment = hasPendingCommentAction(remote.id)

            val merged = if (local != null) {
                remote.copy(
                    currentUserLiked = if (preserveLike) local.currentUserLiked else remote.currentUserLiked,
                    likesCount = if (preserveLike) local.likesCount else remote.likesCount,
                    commentsCount = if (preserveComment) local.commentsCount else remote.commentsCount
                )
            } else {
                remote
            }

            upsert(merged)
        }
    }

    @Query("DELETE FROM local_posts WHERE id = :id")
    suspend fun deletePostById(id: String)

    @Query("DELETE FROM local_posts")
    suspend fun deleteAll()
}
