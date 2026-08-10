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

    @Upsert
    suspend fun upsertAll(entities: List<PostEntity>)

    @Query("DELETE FROM local_posts WHERE id = :id")
    suspend fun deletePostById(id: String)

    @Query("DELETE FROM local_posts")
    suspend fun deleteAll()
}
