package com.example.notification.preferences

import androidx.annotation.Keep
import com.example.notification.engine.model.InterruptivenessLevel
import java.util.concurrent.ConcurrentHashMap

@Keep
class FavoriteContactsRepository private constructor() {

    private val favoriteContactIds = ConcurrentHashMap.newKeySet<String>()

    fun addFavorite(userId: String) {
        favoriteContactIds.add(userId)
    }

    fun removeFavorite(userId: String) {
        favoriteContactIds.remove(userId)
    }

    fun isFavorite(userId: String): Boolean = favoriteContactIds.contains(userId)

    fun resolveInterruptivenessForFavorite(userId: String, isCall: Boolean): InterruptivenessLevel {
        if (!isFavorite(userId)) return InterruptivenessLevel.STATUS_BAR_ONLY
        return if (isCall) InterruptivenessLevel.FULLSCREEN else InterruptivenessLevel.HEADS_UP
    }

    companion object {
        @Volatile
        private var instance: FavoriteContactsRepository? = null

        fun getInstance(): FavoriteContactsRepository {
            return instance ?: synchronized(this) {
                instance ?: FavoriteContactsRepository().also { instance = it }
            }
        }
    }
}
