package com.example.media.storage

import android.content.Context
import android.util.Log
import com.example.media.dedup.MediaDeduplicationEngine
import com.example.media.security.MediaSecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaStorageManager(private val context: Context) {
    private val TAG = "MediaStorageManager"
    private val client = OkHttpClient()

    private fun getMediaDirectory(type: String): File {
        val baseDir = File(context.filesDir, "media")
        val specificDir = when (type.uppercase()) {
            "AVATAR" -> File(baseDir, "profiles/avatars")
            "COVER" -> File(baseDir, "covers")
            "POST_IMAGE", "POST_VIDEO", "FEED" -> File(baseDir, "feed")
            "REEL_MEDIA", "REEL" -> File(baseDir, "reels")
            "STORY_MEDIA", "STORY" -> File(baseDir, "stories")
            "CHAT_IMAGE", "CHAT_VIDEO", "CHAT", "VOICE_NOTE", "DOCUMENT" -> File(baseDir, "chat")
            "THUMBNAIL" -> File(baseDir, "thumbnails")
            else -> File(baseDir, "other")
        }
        if (!specificDir.exists()) {
            specificDir.mkdirs()
        }
        return specificDir
    }

    suspend fun downloadMediaSafely(remoteUrl: String, type: String, id: String = UUID.randomUUID().toString()): File? = withContext(Dispatchers.IO) {
        if (!MediaSecurityValidator.isUrlSafe(remoteUrl)) {
            Log.e(TAG, "Unsafe URL: $remoteUrl")
            return@withContext null
        }

        val targetDir = getMediaDirectory(type)
        val ext = remoteUrl.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 } ?: "bin"
        
        val targetFile = File(targetDir, "media_${id}.$ext")
        if (targetFile.exists() && targetFile.length() > 0 && MediaSecurityValidator.validateFile(targetFile)) {
            return@withContext targetFile
        }

        val tempFile = File(targetDir, "media_${id}_${System.currentTimeMillis()}.tmp")
        
        try {
            val request = Request.Builder().url(remoteUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download media: ${response.code}")
                    return@withContext null
                }
                
                val body = response.body ?: return@withContext null
                
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (tempFile.length() > 0 && MediaSecurityValidator.validateFile(tempFile)) {
                // Atomic rename
                if (tempFile.renameTo(targetFile)) {
                    // Deduplicate identical content across targetDir
                    val finalFile = MediaDeduplicationEngine.deduplicateFile(targetFile, targetDir)
                    Log.i(TAG, "Successfully downloaded media to ${finalFile.absolutePath}")
                    return@withContext finalFile
                } else {
                    Log.e(TAG, "Failed to rename temp file to target file")
                    tempFile.delete()
                }
            } else {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading media", e)
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }
}
