package com.example.media.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.media.analytics.MediaAnalytics
import com.example.media.repository.MediaRepository
import com.example.media.social.SocialMediaCleaner
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MediaSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "MediaSyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting background MediaSyncWorker task")
            val storageManager = MediaStorageManager(applicationContext)
            val repository = MediaRepository(applicationContext, storageManager)

            // 1. Clean expired stories and reels cache
            SocialMediaCleaner.cleanExpiredStoriesAndReels(applicationContext)

            // 2. Refresh health analytics report
            val healthReport = MediaAnalytics.getHealthReport(applicationContext)
            Log.i(TAG, "Media Engine Health: CacheSize=${healthReport.totalCacheSizeBytes} bytes, HitRate=${healthReport.cacheHitRatePercentage}%")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MediaSyncWorker execution failed", e)
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<MediaSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MediaSyncWorkerJob",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
