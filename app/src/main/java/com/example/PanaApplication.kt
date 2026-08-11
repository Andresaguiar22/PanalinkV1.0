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
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
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
                                    android.util.Log.i("CoilInterceptor", "CDN updated! Retrying with new URL: $finalResolvedUrl")
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

        // Shield System: Immediate Security Audit
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
        
        try {
            SessionManager.init(this)
            com.example.data.repository.CdnManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "SessionManager/CdnManager init failed safely", e)
        }
        
        try {
            com.example.util.NetworkMonitor.startMonitoring(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "NetworkMonitor start failed safely", e)
        }
        
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Throwable) {
            android.util.Log.e("PanaApplication", "ProcessLifecycleOwner observer failed safely", e)
        }

        // Pre-fetch dynamic CDN URL on startup asynchronously
        applicationScope.launch {
            try {
                com.example.data.repository.CdnManager.getCDNUrl()
            } catch (e: Throwable) {
                android.util.Log.e("PanaApplication", "Error pre-fetching CDN URL at startup", e)
            }
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
