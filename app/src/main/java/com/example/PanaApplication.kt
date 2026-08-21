package com.example

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PanaApplication : Application(), ImageLoaderFactory, DefaultLifecycleObserver, Configuration.Provider {
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                // Persistent application storage: Android may purge cacheDir while offline.
                coil.disk.DiskCache.Builder()
                    .directory(filesDir.resolve("image_cache"))
                    .maxSizeBytes(150 * 1024 * 1024)
                    .build()
            }
            .components {
                add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                add(coil.intercept.Interceptor { chain ->
                    val request = chain.request
                    val data = request.data
                    if (data is String) {
                        val resolvedUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(data)
                        var currentRequest = request
                        if (resolvedUrl != data) {
                            currentRequest = request.newBuilder().data(resolvedUrl).build()
                        }

                        try {
                            return@Interceptor chain.proceed(currentRequest)
                        } catch (e: Exception) {
                            val isNetworkError = e is java.net.ConnectException ||
                                e is java.net.SocketTimeoutException ||
                                e is java.net.UnknownHostException ||
                                e is java.io.IOException
                            val hasRetried = request.headers["X-CDN-Retried"] == "true"
                            if (isNetworkError && !hasRetried && com.example.data.repository.CdnManager.isCdnRelated(data)) {
                                android.util.Log.w("CoilInterceptor", "Network error resolving media, forcing CDN refresh: ${e.message}")
                                kotlinx.coroutines.runBlocking {
                                    com.example.data.repository.CdnManager.getCDNUrl(forceRefresh = true)
                                }
                                val finalResolvedUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(data)
                                if (finalResolvedUrl != resolvedUrl && finalResolvedUrl.isNotEmpty()) {
                                    val retryRequest = request.newBuilder()
                                        .data(finalResolvedUrl)
                                        .addHeader("X-CDN-Retried", "true")
                                        .build()
                                    return@Interceptor chain.proceed(retryRequest)
                                }
                            }
                            throw e
                        }
                    }
                    chain.proceed(request)
                })
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this

        try {
            val audit = com.example.util.SecurityManager.getSecurityAudit(this)
            android.util.Log.i("Shield", "Application Shield Status: ${audit.status} (${audit.score}/100)")
        } catch (e: Throwable) {
            android.util.Log.e("Shield", "Security audit failed at startup", e)
        }

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "FirebaseApp initialization failed safely", e)
        }

        // Los canales deben existir desde el arranque: FCM los usa incluso con la app cerrada
        try {
            com.example.service.NotificationHelper.createNotificationChannels(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "Notification channel creation failed safely", e)
        }

        try {
            SessionManager.init(this)
            com.example.data.repository.CdnManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "SessionManager/CdnManager init failed safely", e)
        }

        try {
            com.example.util.NetworkMonitor.startMonitoring(this)
            observeConnectivityRestore()
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "NetworkMonitor start failed safely", e)
        }

        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "ProcessLifecycleOwner observer failed safely", e)
        }

        applicationScope.launch {
            try {
                com.example.data.repository.CdnManager.getCDNUrl()
            } catch (e: Throwable) {
                android.util.Log.e("PanaApplication", "Error pre-fetching CDN URL at startup", e)
            }
        }

        scheduleOfflineMediaWarmup()
    }

    /**
     * Offline-first como WhatsApp/Telegram: cuando vuelve la conectividad reconectamos
     * Realtime al instante y disparamos la sincronización de mensajes pendientes, en
     * lugar de esperar hasta 30s de backoff o a que el usuario recargue manualmente.
     */
    private fun observeConnectivityRestore() {
        applicationScope.launch {
            var wasOnline = com.example.util.NetworkMonitor.isOnline.value
            com.example.util.NetworkMonitor.isOnline.collect { isOnline ->
                if (isOnline && !wasOnline) {
                    android.util.Log.i("PanaApplication", "Connectivity restored: reconnecting realtime + syncing pending")
                    try {
                        SupabaseClient.connectRealtime()
                    } catch (e: Throwable) {
                        android.util.Log.e("PanaApplication", "Realtime reconnect on restore failed", e)
                    }
                    try {
                        com.example.data.repository.MessagesRepository.getInstance().scheduleSync()
                    } catch (e: Throwable) {
                        android.util.Log.e("PanaApplication", "Sync scheduling on restore failed", e)
                    }
                    try {
                        com.example.data.repository.CdnManager.getCDNUrl(forceRefresh = true)
                    } catch (e: Throwable) {
                        android.util.Log.e("PanaApplication", "CDN refresh on restore failed", e)
                    }
                }
                wasOnline = isOnline
            }
        }
    }

    private fun scheduleOfflineMediaWarmup() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val immediate = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.OfflineMediaCacheWorker>()
                .setConstraints(constraints)
                .addTag("offline_media_warmup")
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
                "offline_media_warmup_now",
                androidx.work.ExistingWorkPolicy.KEEP,
                immediate
            )

            val periodic = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.OfflineMediaCacheWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("offline_media_periodic")
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "offline_media_periodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )
        } catch (t: Throwable) {
            android.util.Log.w("PanaApplication", "Offline media warm-up scheduling failed: ${t.message}")
        }
    }

    var isAppInForeground: Boolean = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        android.util.Log.d("PanaApplication", "App in foreground: connecting realtime")
        try {
            if (SupabaseClient.currentUser != null) {
                SupabaseClient.connectRealtime()
            }
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "Error connecting realtime onStart", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        android.util.Log.d("PanaApplication", "App in background: disconnecting realtime")
        try {
            SupabaseClient.disconnectRealtime(resetAttempts = true)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "Error disconnecting realtime onStop", e)
        }
    }

    companion object {
        lateinit var instance: PanaApplication
            private set
    }
}
