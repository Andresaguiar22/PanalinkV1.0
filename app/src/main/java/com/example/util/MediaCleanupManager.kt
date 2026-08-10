package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaCleanupManager {
    private const val TAG = "MediaCleanupManager"
    private const val MAX_AGE_MS = 48 * 60 * 60 * 1000L // 48 hours

    suspend fun cleanOrphanMedia(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = PanalinkDatabase.getDatabase(context)
            val activeUploads = db.pendingUploadDao().getUploadsByStatus("pending") +
                    db.pendingUploadDao().getUploadsByStatus("uploading") +
                    db.pendingUploadDao().getUploadsByStatus("failed")

            val activePaths = activeUploads.mapNotNull { it.localFilePath }.toSet()

            val pendingMediaDir = File(context.filesDir, "pending_media")
            if (pendingMediaDir.exists() && pendingMediaDir.isDirectory) {
                val now = System.currentTimeMillis()
                val files = pendingMediaDir.listFiles() ?: emptyArray()

                var deletedCount = 0
                var freedBytes = 0L

                for (file in files) {
                    val filePath = file.absolutePath
                    val isReferenced = activePaths.contains(filePath)
                    val age = now - file.lastModified()

                    if (!isReferenced && age > MAX_AGE_MS) {
                        val length = file.length()
                        if (file.delete()) {
                            deletedCount++
                            freedBytes += length
                        }
                    }
                }
                Log.i(TAG, "Limpieza completada: $deletedCount archivos eliminados ($freedBytes bytes liberados)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la limpieza automática de media", e)
        }
    }
}
