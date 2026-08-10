package com.example.creative.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoFrameProcessor {
    private const val TAG = "VideoFrameProcessor"

    suspend fun extractFrameAt(
        videoPath: String,
        timeUs: Long = 0L
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!File(videoPath).exists()) return@withContext null
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            return@withContext bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frame at $timeUs us from $videoPath", e)
            return@withContext null
        }
    }
}
