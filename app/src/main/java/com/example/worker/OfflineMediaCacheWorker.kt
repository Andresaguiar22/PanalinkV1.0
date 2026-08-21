package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.CdnManager
import com.example.util.OfflineMediaCache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Persistent warm-up for recent media without loading entire conversations into RAM. */
class OfflineMediaCacheWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun doWork(): Result {
        // Storage center "auto-download" settings decide whether the warm-up
        // runs on the current network type.
        if (!isAutoDownloadAllowedNow()) {
            Log.d("OfflineMediaCacheWorker", "Auto-download disabled for current network; skipping warm-up")
            return Result.success()
        }
        val dao = PanalinkDatabase.getDatabase(applicationContext).messageDao()
        var failures = 0
        var processed = 0
        return try {
            for (chatId in dao.getDistinctChatIds()) {
                if (isStopped) break
                // Query only the newest 40 rows instead of materializing a whole chat.
                for (message in dao.getMessagesForChatPaged(chatId, 40, null).asReversed()) {
                    if (isStopped) break
                    val mediaUrl = message.mediaUrl
                    if (!mediaUrl.isNullOrBlank() && message.messageType != "text") {
                        val resolved = CdnManager.resolveMediaUrl(mediaUrl)
                        if (OfflineMediaCache.existingUri(applicationContext, resolved, message.mediaMime) == null) {
                            if (!download(resolved, message.mediaMime)) failures++ else processed++
                        }
                    }
                    val thumbUrl = message.thumbnailUrl
                    if (!thumbUrl.isNullOrBlank()) {
                        val resolvedThumb = CdnManager.resolveMediaUrl(thumbUrl)
                        if (OfflineMediaCache.existingUri(applicationContext, resolvedThumb, "image/jpeg") == null) {
                            if (!download(resolvedThumb, "image/jpeg")) failures++ else processed++
                        }
                    }
                }
            }
            Log.d("OfflineMediaCacheWorker", "Offline media warm-up: $processed downloaded, $failures failed")
            if (failures > 0 && processed == 0) Result.retry() else Result.success()
        } catch (t: Throwable) {
            Log.w("OfflineMediaCacheWorker", "Warm-up failed: ${t.message}")
            Result.retry()
        }
    }

    /** Reads the Storage center prefs and checks the active network transport. */
    private fun isAutoDownloadAllowedNow(): Boolean {
        return try {
            val prefs = applicationContext.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
            val keys = com.example.ui.settings.models.SettingsKeys
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            when {
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ->
                    prefs.getBoolean(keys.STORAGE_AUTO_DOWNLOAD_WIFI, true)
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    if (cm.isActiveNetworkMetered)
                        prefs.getBoolean(keys.STORAGE_AUTO_DOWNLOAD_MOBILE, true)
                    else
                        true
                else -> prefs.getBoolean(keys.STORAGE_AUTO_DOWNLOAD_WIFI, true)
            }
        } catch (t: Throwable) {
            true
        }
    }

    private fun download(url: String, mime: String?): Boolean {
        if (url.isBlank()) return false
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                OfflineMediaCache.saveStream(applicationContext, url, mime, body) != null
            }
        } catch (t: Throwable) {
            Log.d("OfflineMediaCacheWorker", "Download skipped: ${t.message}")
            false
        }
    }
}
