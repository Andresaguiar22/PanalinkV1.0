package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUploadDao {
    @Query("SELECT * FROM pending_uploads WHERE id = :id")
    suspend fun getUploadById(id: String): PendingUploadEntity?

    @Query("SELECT * FROM pending_uploads WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUploadsByUser(userId: String): Flow<List<PendingUploadEntity>>

    @Query("SELECT * FROM pending_uploads WHERE userId = :userId AND status IN ('pending', 'uploading', 'failed') ORDER BY createdAt ASC")
    fun getActiveUploadsByUserFlow(userId: String): Flow<List<PendingUploadEntity>>

    @Query("SELECT * FROM pending_uploads WHERE status IN ('pending', 'uploading', 'failed') ORDER BY createdAt ASC")
    fun getAllActiveUploadsFlow(): Flow<List<PendingUploadEntity>>

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt DESC")
    fun getAllUploadsFlow(): Flow<List<PendingUploadEntity>>

    @Query("SELECT * FROM pending_uploads WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getUploadsByStatus(status: String): List<PendingUploadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpload(upload: PendingUploadEntity)

    @Update
    suspend fun updateUpload(upload: PendingUploadEntity)

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun deleteUploadById(id: String)

    @Query("DELETE FROM pending_uploads WHERE status = 'completed'")
    suspend fun clearCompletedUploads()
}
