package com.example.data.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.media3.common.util.UnstableApi

@UnstableApi
object CacheDataSourceFactory {
    private const val TAG = "CacheDataSourceFactory"

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        // DefaultHttpDataSource setup with snappy timeouts optimized for quick loading and robust user-agent to bypass CDN blocks
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 Panalink/1.0")
            .setConnectTimeoutMs(15000) // 15s connect timeout
            .setReadTimeoutMs(15000)    // 15s read timeout
            .setAllowCrossProtocolRedirects(true)

        return try {
            val simpleCache = VideoCacheManager.getCache(context)
            if (simpleCache != null) {
                CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setCacheReadDataSourceFactory(FileDataSource.Factory())
                    .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(simpleCache))
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                httpDataSourceFactory
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize CacheDataSource, falling back to network-only factory", e)
            httpDataSourceFactory
        }
    }

    /**
     * Pre-fetches the first 5MB of a video URL into the shared SimpleCache in the background.
     * When ExoPlayer plays this video later, it starts instantly from local cache.
     */
    fun prefetchVideo(context: Context, url: String?) {
        if (url.isNullOrBlank() || !url.startsWith("http")) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataSource = getCacheDataSourceFactory(context).createDataSource()
                // Fetch first 5 megabytes for smoother initial playback
                val prefetchBytes = 5 * 1024 * 1024L
                val dataSpec = DataSpec(Uri.parse(url), 0, prefetchBytes)
                val buffer = ByteArray(65536) // Larger buffer for faster copy
                
                Log.d(TAG, "Aggressive pre-fetching starting for: $url")
                dataSource.open(dataSpec)
                var bytesRead = 0L
                
                while (bytesRead < prefetchBytes) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == -1) break
                    bytesRead += read
                }
                dataSource.close()
                Log.d(TAG, "Successfully pre-fetched ${bytesRead / 1024} KB for video: $url")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-fetch cancelled or failed for $url: ${e.localizedMessage}")
            }
        }
    }
}
