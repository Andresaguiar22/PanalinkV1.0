package com.example.media.analytics

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicLong

data class MediaHealthReport(
    val totalCacheSizeBytes: Long,
    val cacheHitCount: Long,
    val cacheMissCount: Long,
    val downloadSuccessCount: Long,
    val downloadFailureCount: Long,
    val cacheHitRatePercentage: Float,
    val offlineSuccessRatePercentage: Float
)

object MediaAnalytics {
    private const val TAG = "MediaAnalytics"

    private val cacheHitCounter = AtomicLong(0)
    private val cacheMissCounter = AtomicLong(0)
    private val downloadSuccessCounter = AtomicLong(0)
    private val downloadFailureCounter = AtomicLong(0)

    fun logCacheHit(mediaId: String) {
        cacheHitCounter.incrementAndGet()
        Log.i(TAG, "Event: cache_hit | mediaId: $mediaId")
    }

    fun logCacheMiss(mediaId: String) {
        cacheMissCounter.incrementAndGet()
        Log.i(TAG, "Event: cache_miss | mediaId: $mediaId")
    }

    fun logDownloadStarted(mediaId: String) {
        Log.i(TAG, "Event: download_started | mediaId: $mediaId")
    }

    fun logDownloadCompleted(mediaId: String, durationMs: Long, sizeBytes: Long) {
        downloadSuccessCounter.incrementAndGet()
        Log.i(TAG, "Event: download_completed | mediaId: $mediaId | durationMs: $durationMs | sizeBytes: $sizeBytes")
    }

    fun logDownloadFailed(mediaId: String, error: String) {
        downloadFailureCounter.incrementAndGet()
        Log.e(TAG, "Event: download_failed | mediaId: $mediaId | error: $error")
    }

    fun logOfflinePlayback(mediaId: String) {
        Log.i(TAG, "Event: offline_playback | mediaId: $mediaId")
    }

    fun getHealthReport(context: Context): MediaHealthReport {
        val baseDir = File(context.filesDir, "media")
        val cacheSize = if (baseDir.exists()) {
            baseDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L

        val hits = cacheHitCounter.get()
        val misses = cacheMissCounter.get()
        val totalReqs = hits + misses
        val hitRate = if (totalReqs > 0) (hits.toFloat() / totalReqs.toFloat()) * 100f else 100f

        val successes = downloadSuccessCounter.get()
        val failures = downloadFailureCounter.get()
        val totalDownloads = successes + failures
        val offlineSuccess = if (totalDownloads > 0) (successes.toFloat() / totalDownloads.toFloat()) * 100f else 100f

        return MediaHealthReport(
            totalCacheSizeBytes = cacheSize,
            cacheHitCount = hits,
            cacheMissCount = misses,
            downloadSuccessCount = successes,
            downloadFailureCount = failures,
            cacheHitRatePercentage = hitRate,
            offlineSuccessRatePercentage = offlineSuccess
        )
    }
}
