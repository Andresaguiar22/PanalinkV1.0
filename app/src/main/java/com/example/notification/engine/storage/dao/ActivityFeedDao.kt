package com.example.notification.engine.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notification.engine.storage.entity.ActivityFeedEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityFeedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityFeedEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityFeedEntityV2>)

    @Query("SELECT * FROM activity_feed_v2 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ActivityFeedEntityV2>>

    @Query("DELETE FROM activity_feed_v2 WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM activity_feed_v2")
    suspend fun clearAll()
}
