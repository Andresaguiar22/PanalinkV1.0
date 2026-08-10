package com.example.media.lifecycle

import android.content.Context
import android.util.Log
import com.example.media.analytics.MediaAnalytics
import com.example.media.model.MediaAssetEntity
import com.example.media.repository.MediaRepository
import com.example.media.security.MediaSecurityValidator
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaLifecycleManager(
    private val context: Context,
    private val repository: MediaRepository,
    private val storageManager: MediaStorageManager
) {
    private val TAG = "MediaLifecycleManager"

    suspend fun getOrFetchMedia(
        id: String,
        remoteUrl: String,
        type: String,
        ownerId: String? = null
    ): File? = withContext(Dispatchers.IO) {
        if (!MediaSecurityValidator.isUrlSafe(remoteUrl)) {
            Log.e(TAG, "Unsafe URL rejected: $remoteUrl")
            return@withContext null
        }

        // 1. Check local repository
        val existing = repository.getLocalMedia(id)
        if (existing != null && !existing.localPath.isNullOrBlank()) {
            val safePath = MediaSecurityValidator.sanitizePath(existing.localPath)
            val file = File(safePath)
            if (MediaSecurityValidator.validateFile(file)) {
                // Update access timestamp
                repository.saveMediaAsset(
                    existing.copy(lastSyncedAt = System.currentTimeMillis())
                )
                MediaAnalytics.logCacheHit(id)
                return@withContext file
            }
        }

        // 2. Fetch from network
        MediaAnalytics.logCacheMiss(id)
        val downloadedFile = repository.syncManager.syncMedia(id, remoteUrl, type, ownerId)
        val localPath = downloadedFile?.localPath ?: return@withContext null

        val file = File(MediaSecurityValidator.sanitizePath(localPath))
        return@withContext if (MediaSecurityValidator.validateFile(file)) file else null
    }

    suspend fun deleteMediaAsset(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val asset = repository.getLocalMedia(id)
            if (asset != null && !asset.localPath.isNullOrBlank()) {
                val file = File(asset.localPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting media asset $id", e)
            false
        }
    }

    suspend fun purgeExpiredCache(maxAgeMs: Long): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val baseDir = File(context.filesDir, "media")
            if (baseDir.exists() && baseDir.isDirectory) {
                val now = System.currentTimeMillis()
                baseDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    if (now - file.lastModified() > maxAgeMs) {
                        if (file.delete()) {
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purging expired cache", e)
        }
        count
    }
}
