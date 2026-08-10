package com.example.notification.cache

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.example.notification.engine.cache.NotificationAvatarCache as EngineAvatarCache

@Keep
object NotificationAvatarCache {

    fun get(url: String): Bitmap? = EngineAvatarCache.get(url)

    fun put(url: String, bitmap: Bitmap) = EngineAvatarCache.put(url, bitmap)

    fun clear() = EngineAvatarCache.clear()
}
