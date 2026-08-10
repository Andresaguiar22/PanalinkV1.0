package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM local_comments WHERE targetId = :targetId AND isReel = :isReel ORDER BY createdAt ASC")
    fun getCommentsFlow(targetId: String, isReel: Boolean): Flow<List<CommentEntity>>

    @Query("SELECT * FROM local_comments WHERE targetId = :targetId AND isReel = :isReel ORDER BY createdAt ASC")
    suspend fun getComments(targetId: String, isReel: Boolean): List<CommentEntity>

    @Query("SELECT * FROM local_comments WHERE id = :id")
    suspend fun getCommentById(id: String): CommentEntity?

    @Upsert
    suspend fun upsert(entity: CommentEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CommentEntity>)

    @Query("DELETE FROM local_comments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM local_comments WHERE targetId = :targetId AND isReel = :isReel")
    suspend fun deleteByTarget(targetId: String, isReel: Boolean)

    @Query("DELETE FROM local_comments")
    suspend fun deleteAll()
}
