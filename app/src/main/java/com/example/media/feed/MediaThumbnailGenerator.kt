package com.example.media.feed

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaThumbnailGenerator {
    private const val TAG = "MediaThumbnailGenerator"

    suspend fun getOrGenerateVideoThumbnail(
        context: Context,
        mediaId: String,
        videoPath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(videoPath)
            if (!file.exists()) return@withContext null

            val storageManager = MediaStorageManager(context.applicationContext)
            val thumbFile = File(context.cacheDir, "thumb_${mediaId}.jpg")

            if (thumbFile.exists() && thumbFile.length() > 0) {
                return@withContext thumbFile.absolutePath
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.frameAtTime
            retriever.release()

            if (bitmap != null) {
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
                return@withContext thumbFile.absolutePath
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $videoPath", e)
            null
        }
    }
}
