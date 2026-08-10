package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.media.model.MediaAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE id = :id")
    suspend fun getMediaAsset(id: String): MediaAssetEntity?

    @Query("SELECT * FROM media_assets WHERE id = :id")
    fun observeMediaAsset(id: String): Flow<MediaAssetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(asset: MediaAssetEntity)

    @Query("DELETE FROM media_assets WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("SELECT * FROM media_assets WHERE syncState = :syncState")
    suspend fun getAssetsBySyncState(syncState: String): List<MediaAssetEntity>
}
