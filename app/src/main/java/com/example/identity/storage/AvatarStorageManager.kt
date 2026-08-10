package com.example.identity.storage

import android.content.Context
import com.example.identity.analytics.IdentityAnalytics
import com.example.identity.model.AvatarDownloadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class AvatarStorageManager(private val context: Context) {

    private val avatarsDir by lazy {
        File(context.filesDir, "avatars/users").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun downloadAvatar(userId: String, urlString: String): AvatarDownloadResult = withContext(Dispatchers.IO) {
        try {
            val file = getAvatarFile(userId)
            val url = URL(urlString)
            url.openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            IdentityAnalytics.trackAvatarDownloaded()
            AvatarDownloadResult.Success(file.absolutePath)
        } catch (e: Exception) {
            AvatarDownloadResult.Error(e)
        }
    }

    suspend fun saveAvatar(userId: String, bytes: ByteArray): AvatarDownloadResult = withContext(Dispatchers.IO) {
        try {
            val file = getAvatarFile(userId)
            file.writeBytes(bytes)
            AvatarDownloadResult.Success(file.absolutePath)
        } catch (e: Exception) {
            AvatarDownloadResult.Error(e)
        }
    }
    
    suspend fun updateAvatar(userId: String, urlString: String): AvatarDownloadResult {
        return downloadAvatar(userId, urlString)
    }

    suspend fun deleteAvatar(userId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getAvatarFile(userId)
        if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    fun avatarExists(userId: String): Boolean {
        return getAvatarFile(userId).exists()
    }

    fun getAvatarFile(userId: String): File {
        return File(avatarsDir, "$userId.webp")
    }
}
