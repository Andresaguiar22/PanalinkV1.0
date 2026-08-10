package com.example.media.social

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SocialMediaCleaner {
    private const val TAG = "SocialMediaCleaner"
    private const val STORY_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val MAX_REELS_CACHE_BYTES = 200 * 1024 * 1024L // 200 MB

    suspend fun cleanExpiredStoriesAndReels(context: Context) = withContext(Dispatchers.IO) {
        try {
            val baseDir = File(context.filesDir, "media")
            
            // 1. Clean stories older than 24 hours
            val storiesDir = File(baseDir, "stories")
            if (storiesDir.exists() && storiesDir.isDirectory) {
                val now = System.currentTimeMillis()
                storiesDir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > STORY_EXPIRATION_MS) {
                        Log.i(TAG, "Cleaning expired story file: ${file.name}")
                        file.delete()
                    }
                }
            }

            // 2. Clean reels LRU if total size > 200MB
            val reelsDir = File(baseDir, "reels")
            if (reelsDir.exists() && reelsDir.isDirectory) {
                val files = reelsDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
                var totalSize = files.sumOf { it.length() }

                if (totalSize > MAX_REELS_CACHE_BYTES) {
                    for (file in files) {
                        if (totalSize <= MAX_REELS_CACHE_BYTES) break
                        val size = file.length()
                        if (file.delete()) {
                            totalSize -= size
                            Log.i(TAG, "LRU deleted reel file: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning social media cache", e)
        }
    }
}
