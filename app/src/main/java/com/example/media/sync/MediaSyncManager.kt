package com.example.media.sync

import android.content.Context
import android.util.Log
import com.example.media.analytics.MediaAnalytics
import com.example.media.model.MediaAssetEntity
import com.example.media.model.MediaDownloadState
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaSyncManager(
    private val context: Context,
    private val repository: MediaRepository,
    private val storageManager: MediaStorageManager
) {
    private val TAG = "MediaSyncManager"

    suspend fun syncMedia(id: String, remoteUrl: String, type: String, ownerId: String? = null): MediaAssetEntity? = withContext(Dispatchers.IO) {
        try {
            val existing = repository.getLocalMedia(id)
            if (existing != null && !existing.localPath.isNullOrBlank()) {
                val file = java.io.File(existing.localPath)
                if (file.exists() && file.length() > 0) {
                    MediaAnalytics.logCacheHit(id)
                    return@withContext existing
                }
            }
            
            MediaAnalytics.logCacheMiss(id)
            MediaAnalytics.logDownloadStarted(id)
            val startTime = System.currentTimeMillis()
            Log.i(TAG, "Downloading media $id from $remoteUrl")
            val downloadedFile = storageManager.downloadMediaSafely(remoteUrl, type, id)
            
            if (downloadedFile != null) {
                val duration = System.currentTimeMillis() - startTime
                MediaAnalytics.logDownloadCompleted(id, duration, downloadedFile.length())
                val newEntity = MediaAssetEntity(
                    id = id,
                    ownerId = ownerId,
                    type = type,
                    remoteUrl = remoteUrl,
                    localPath = downloadedFile.absolutePath,
                    thumbnailPath = null, // Can be generated later
                    mimeType = null, 
                    sizeBytes = downloadedFile.length(),
                    width = null,
                    height = null,
                    durationMs = null,
                    createdAt = System.currentTimeMillis(),
                    lastSyncedAt = System.currentTimeMillis(),
                    syncState = MediaDownloadState.AVAILABLE.name
                )
                repository.saveMediaAsset(newEntity)
                return@withContext newEntity
            } else {
                MediaAnalytics.logDownloadFailed(id, "Download returned null file")
                
                // If it failed but we have an old entry, we update state to FAILED
                if (existing != null) {
                    repository.saveMediaAsset(existing.copy(syncState = MediaDownloadState.FAILED.name))
                } else {
                    // Create an entry with FAILED state
                    val failedEntity = MediaAssetEntity(
                        id = id,
                        ownerId = ownerId,
                        type = type,
                        remoteUrl = remoteUrl,
                        localPath = null,
                        thumbnailPath = null,
                        mimeType = null, 
                        sizeBytes = 0L,
                        width = null,
                        height = null,
                        durationMs = null,
                        createdAt = System.currentTimeMillis(),
                        lastSyncedAt = System.currentTimeMillis(),
                        syncState = MediaDownloadState.FAILED.name
                    )
                    repository.saveMediaAsset(failedEntity)
                    return@withContext failedEntity
                }
            }
            
            return@withContext existing
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing media $id", e)
            MediaAnalytics.logDownloadFailed(id, e.message ?: "Unknown error")
            return@withContext null
        }
    }
}

