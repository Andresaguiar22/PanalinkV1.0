package com.example.identity.storage

import android.content.Context
import com.example.identity.model.CoverDownloadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class CoverStorageManager(private val context: Context) {

    private val coversDir by lazy {
        File(context.filesDir, "covers/users").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun downloadCover(userId: String, urlString: String): CoverDownloadResult = withContext(Dispatchers.IO) {
        try {
            val file = getCoverFile(userId)
            val url = URL(urlString)
            url.openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            CoverDownloadResult.Success(file.absolutePath)
        } catch (e: Exception) {
            CoverDownloadResult.Error(e)
        }
    }

    suspend fun saveCover(userId: String, bytes: ByteArray): CoverDownloadResult = withContext(Dispatchers.IO) {
        try {
            val file = getCoverFile(userId)
            file.writeBytes(bytes)
            CoverDownloadResult.Success(file.absolutePath)
        } catch (e: Exception) {
            CoverDownloadResult.Error(e)
        }
    }
    
    suspend fun updateCover(userId: String, urlString: String): CoverDownloadResult {
        return downloadCover(userId, urlString)
    }

    suspend fun deleteCover(userId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getCoverFile(userId)
        if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    fun coverExists(userId: String): Boolean {
        return getCoverFile(userId).exists()
    }

    fun getCoverFile(userId: String): File {
        return File(coversDir, "$userId.webp")
    }
}
