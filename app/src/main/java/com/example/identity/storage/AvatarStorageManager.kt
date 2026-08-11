package com.example.identity.storage

import android.content.Context
import androidx.annotation.Keep
import com.example.identity.model.AvatarDownloadResult
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Keep
class AvatarStorageManager(private val context: Context) {
    
    suspend fun downloadAvatar(userId: String, url: String): AvatarDownloadResult = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars/users")
            if (!avatarsDir.exists()) avatarsDir.mkdirs()
            
            val file = File(avatarsDir, "${userId}_avatar.jpg")
            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            AvatarDownloadResult.Success(file.absolutePath)
        } catch (e: Exception) {
            AvatarDownloadResult.Error(e.message ?: "Unknown error")
        }
    }
}
