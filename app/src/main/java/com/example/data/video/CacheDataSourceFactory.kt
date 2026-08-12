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
import androidx.media3.datasource.cache.CacheKeyFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi

@UnstableApi
object CacheDataSourceFactory {
    private const val TAG = "CacheDataSourceFactory"
    private const val PREFETCH_BYTES = 5 * 1024 * 1024L

    /**
     * CDN-independent cache key. A Cloudflare quick tunnel can change host while the
     * logical media path stays the same, so the host must never become part of the
     * Media3 cache identity.
     */
    private val logicalMediaCacheKeyFactory = CacheKeyFactory { dataSpec ->
        runCatching {
            val uri = Uri.parse(dataSpec.uri.toString())
            buildString {
                append(uri.path ?: uri.toString())
                uri.query?.let { append('?').append(it) }
            }
        }.getOrElse { dataSpec.uri.toString() }
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Panalink/1.0 Android Media3")
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(12000)
            .setAllowCrossProtocolRedirects(true)

        return try {
            val simpleCache = VideoCacheManager.getCache(context)
            if (simpleCache != null) {
                CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setCacheKeyFactory(logicalMediaCacheKeyFactory)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setCacheReadDataSourceFactory(FileDataSource.Factory())
                    .setCacheWriteDataSinkFactory(
                        CacheDataSink.Factory()
                            .setCache(simpleCache)
                            .setFragmentSize(5 * 1024 * 1024L)
                    )
            } else {
                httpDataSourceFactory
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize CacheDataSource, falling back to network-only factory", e)
            httpDataSourceFactory
        }
    }

    /**
     * Caches the beginning of a video so playback can start quickly while the full
     * persistent reel download continues independently.
     */
    fun prefetchVideo(context: Context, url: String?) {
        if (url.isNullOrBlank() || !url.startsWith("http")) return
        CoroutineScope(Dispatchers.IO).launch {
            val dataSource = try {
                getCacheDataSourceFactory(context).createDataSource()
            } catch (e: Exception) {
                Log.w(TAG, "Unable to create prefetch data source", e)
                return@launch
            }

            try {
                val dataSpec = DataSpec(Uri.parse(url), 0, PREFETCH_BYTES)
                val buffer = ByteArray(64 * 1024)
                dataSource.open(dataSpec)
                var bytesRead = 0L
                while (bytesRead < PREFETCH_BYTES) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == -1) break
                    bytesRead += read
                }
                Log.d(TAG, "Prefetched ${bytesRead / 1024} KB: $url")
            } catch (e: Exception) {
                Log.w(TAG, "Prefetch failed/cancelled: $url", e)
            } finally {
                runCatching { dataSource.close() }
            }
        }
    }
}
