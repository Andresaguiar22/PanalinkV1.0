package com.example.identity.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class MediaStorageManager(private val context: Context) {
    suspend fun downloadAvatar(userId: String, urlString: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "media/avatars")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$userId.webp")
            if (file.exists()) return@withContext file.absolutePath
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            
            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadCover(userId: String, urlString: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "media/covers")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$userId.webp")
            if (file.exists()) return@withContext file.absolutePath
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            
            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadPostPhoto(postId: String, photoId: String, urlString: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "media/posts/$postId")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$photoId.webp")
            if (file.exists()) return@withContext file.absolutePath
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            
            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadMessagePhoto(messageId: String, photoId: String, urlString: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "media/messages/$messageId")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$photoId.webp")
            if (file.exists()) return@withContext file.absolutePath
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            
            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
