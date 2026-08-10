package com.example.notification.engine.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.engine.storage.entity.NotificationEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntityV2>)

    @Query("UPDATE notifications_v2 SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications_v2 SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications_v2 WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notifications_v2 WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM notifications_v2")
    suspend fun clearAll()

    @Query("SELECT * FROM notifications_v2 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntityV2>>

    @Query("SELECT COUNT(*) FROM notifications_v2 WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications_v2 WHERE isRead = 0 AND domain IN (:domains)")
    fun observeUnreadCountForDomains(domains: List<NotificationDomain>): Flow<Int>

    @Query("SELECT * FROM notifications_v2 WHERE domain = :domain ORDER BY timestamp DESC")
    fun observeByDomain(domain: NotificationDomain): Flow<List<NotificationEntityV2>>

    @Query("SELECT * FROM notifications_v2 WHERE type = :type ORDER BY timestamp DESC")
    fun observeByType(type: NotificationTypeV2): Flow<List<NotificationEntityV2>>

    @Query("SELECT * FROM notifications_v2 WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntityV2?
}
