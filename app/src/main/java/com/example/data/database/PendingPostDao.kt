package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPostDao {
    @Query("SELECT * FROM pending_posts WHERE id = :id")
    suspend fun getPostById(id: String): PendingPostEntity?

    @Query("SELECT * FROM pending_posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPostsByUserFlow(userId: String): Flow<List<PendingPostEntity>>

    @Query("SELECT * FROM pending_posts WHERE status IN ('pending', 'uploading', 'failed') ORDER BY createdAt DESC")
    fun getActivePostsFlow(): Flow<List<PendingPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PendingPostEntity)

    @Update
    suspend fun updatePost(post: PendingPostEntity)

    @Query("DELETE FROM pending_posts WHERE id = :id")
    suspend fun deletePostById(id: String)
    
    @Query("UPDATE pending_posts SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateStatusAndProgress(id: String, status: String, progress: Float)
}
