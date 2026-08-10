package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VoiceNoteCacheCleaner {
    private const val TAG = "VoiceNoteCacheCleaner"

    /**
     * Cleans up orphaned voice note files from cacheDir that are older than [maxAgeHours].
     * Ignores currently active files passed in [activeFilePaths].
     */
    suspend fun cleanOldVoiceNotes(
        context: Context,
        maxAgeHours: Long = 24,
        activeFilePaths: Set<String> = emptySet()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir ?: return@withContext
                val now = System.currentTimeMillis()
                val maxAgeMs = maxAgeHours * 60 * 60 * 1000L

                val files = cacheDir.listFiles { _, name ->
                    name.startsWith("voice_note_") && name.endsWith(".m4a")
                } ?: return@withContext

                var deletedCount = 0
                for (file in files) {
                    try {
                        if (activeFilePaths.contains(file.absolutePath)) {
                            continue
                        }
                        val age = now - file.lastModified()
                        if (age > maxAgeMs) {
                            if (file.delete()) {
                                deletedCount++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting cached file: ${file.name}", e)
                    }
                }
                if (deletedCount > 0) {
                    Log.i(TAG, "Cleaned $deletedCount old voice note cache file(s)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning old voice note cache", e)
            }
        }
    }
}
