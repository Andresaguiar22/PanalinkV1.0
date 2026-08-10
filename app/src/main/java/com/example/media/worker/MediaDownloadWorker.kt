package com.example.media.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import com.example.media.sync.MediaSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val mediaId = inputData.getString("mediaId") ?: return@withContext Result.failure()
        val remoteUrl = inputData.getString("remoteUrl") ?: return@withContext Result.failure()
        val type = inputData.getString("type") ?: return@withContext Result.failure()
        val ownerId = inputData.getString("ownerId")

        // In a real app we'd use DI, but here we construct or use a singleton locator
        val storageManager = MediaStorageManager(applicationContext)
        val repository = MediaRepository(applicationContext, storageManager)
        
        try {
            val result = repository.syncManager.syncMedia(mediaId, remoteUrl, type, ownerId)
            if (result != null) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
