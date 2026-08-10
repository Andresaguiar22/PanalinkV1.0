package com.example.media.repository

import android.content.Context
import com.example.data.database.PanalinkDatabase
import com.example.media.model.MediaAssetEntity
import com.example.media.storage.MediaStorageManager
import com.example.media.cache.MediaMemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.example.media.sync.MediaSyncManager

class MediaRepository(
    private val context: Context,
    private val storageManager: MediaStorageManager
) {
    private val mediaAssetDao = PanalinkDatabase.getDatabase(context).mediaAssetDao()
    private val mediaFlowCache = ConcurrentHashMap<String, Flow<MediaAssetEntity?>>()
    val syncManager by lazy { MediaSyncManager(context, this, storageManager) }

    fun observeMedia(id: String): Flow<MediaAssetEntity?> {
        return mediaFlowCache.getOrPut(id) {
            mediaAssetDao.observeMediaAsset(id).map { entity ->
                entity?.let {
                    MediaMemoryCache.put(id, it)
                }
                entity
            }.distinctUntilChanged()
        }
    }

    suspend fun getLocalMedia(id: String): MediaAssetEntity? = withContext(Dispatchers.IO) {
        val memoryHit = MediaMemoryCache.get(id)
        if (memoryHit != null) return@withContext memoryHit
        
        val entity = mediaAssetDao.getMediaAsset(id)
        if (entity != null) {
            MediaMemoryCache.put(id, entity)
        }
        return@withContext entity
    }
    
    suspend fun saveMediaAsset(asset: MediaAssetEntity) = withContext(Dispatchers.IO) {
        mediaAssetDao.insertOrUpdate(asset)
        MediaMemoryCache.put(asset.id, asset)
    }

    suspend fun downloadIfNeeded(id: String, remoteUrl: String, type: String, ownerId: String? = null) {
        syncManager.syncMedia(id, remoteUrl, type, ownerId)
    }

    suspend fun deleteUnusedMedia() = withContext(Dispatchers.IO) {
        // Delegate to MediaStorageCleaner or handle it here
    }
}
