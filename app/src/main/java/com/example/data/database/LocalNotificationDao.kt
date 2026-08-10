package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalNotificationDao {
    @Query("SELECT * FROM local_notifications ORDER BY timestamp DESC")
    fun getNotificationsFlow(): Flow<List<LocalNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: LocalNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<LocalNotificationEntity>)

    @Query("UPDATE local_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM local_notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM local_notifications")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM local_notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>
}
