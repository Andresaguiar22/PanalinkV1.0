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
                        if (resolvedUrl != data) {
                            val newRequest = request.newBuilder().data(resolvedUrl).build()
                            return@Interceptor chain.proceed(newRequest)
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
