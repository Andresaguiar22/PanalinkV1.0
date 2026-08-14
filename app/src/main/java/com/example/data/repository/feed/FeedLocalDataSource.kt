package com.example.data.repository.feed

import com.example.data.database.PostDao
import com.example.data.database.PostEntity
import com.example.data.model.PostDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Feature-owned local boundary for the public Feed / Muro.
 *
 * Room remains the physical SSOT, but Muro callers no longer need to know
 * about PostDao or the shared database implementation.
 */
class FeedLocalDataSource(
    private val postDao: PostDao
) {
    fun observePosts(limit: Int = 20): Flow<List<PostDto>> =
        postDao.getPostsFlow(limit).map { entities ->
            entities.map(PostEntity::toPostDto)
        }

    suspend fun getPostById(id: String): PostDto? =
        postDao.getPostById(id)?.toPostDto()

    suspend fun getPosts(limit: Int = 20): List<PostDto> =
        postDao.getPosts(limit).map(PostEntity::toPostDto)

    suspend fun getPostsPaged(limit: Int, lastCreatedAt: String): List<PostDto> =
        postDao.getPostsPaged(limit, lastCreatedAt).map(PostEntity::toPostDto)

    suspend fun save(post: PostDto) {
        postDao.upsert(PostEntity.fromPostDto(post))
    }

    suspend fun saveAll(posts: List<PostDto>) {
        if (posts.isNotEmpty()) {
            postDao.upsertAll(posts.map(PostEntity::fromPostDto))
        }
    }

    suspend fun deletePost(id: String) {
        postDao.deletePostById(id)
    }
}
