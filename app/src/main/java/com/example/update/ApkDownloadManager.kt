package com.example.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

sealed class DownloadState {
    object Idle : DownloadState()
    object CheckingExisting : DownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ApkDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeCall: okhttp3.Call? = null

    suspend fun downloadApk(
        downloadUrl: String,
        expectedSha256: String,
        versionCode: Long
    ): File? = withContext(Dispatchers.IO) {
        cancelDownload() // Ensure any prior download is cancelled

        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val shaPrefix = if (expectedSha256.length >= 8) expectedSha256.take(8) else expectedSha256
        val destinationFile = File(updatesDir, "panalink_update_${versionCode}_$shaPrefix.apk")
        val tempFile = File(updatesDir, "panalink_update_${versionCode}_$shaPrefix.apk.tmp")

        _downloadState.value = DownloadState.CheckingExisting

        // Clean up older or other update files in the updates directory
        try {
            updatesDir.listFiles()?.forEach { file ->
                if (file.name != destinationFile.name && file.name != tempFile.name) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup failures
        }

        // If a file already exists with correct hash, skip downloading
        if (destinationFile.exists() && ApkIntegrityVerifier.verifySha256(destinationFile, expectedSha256)) {
            _downloadState.value = DownloadState.Success(destinationFile)
            return@withContext destinationFile
        }

        // Clean up any stale temp file
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        _downloadState.value = DownloadState.Downloading(0f, 0L, 0L)
        
        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                val call = okHttpClient.newCall(request)
                activeCall = call

                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Server responded with code ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val contentLength = body.contentLength()

                    if (contentLength > 0) {
                        val availableBytes = try {
                            val stat = android.os.StatFs(context.cacheDir.absolutePath)
                            stat.availableBytes
                        } catch (e: Exception) {
                            Long.MAX_VALUE
                        }
                        val safetyMargin = 5 * 1024 * 1024L // 5 MB safety margin
                        if (availableBytes < (contentLength + safetyMargin)) {
                            throw IOException("Espacio en disco insuficiente para descargar la actualización. Se requieren ${(contentLength + safetyMargin) / 1024 / 1024}MB, pero solo hay ${availableBytes / 1024 / 1024}MB libres.")
                        }
                    }

                    body.byteStream().use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                    _downloadState.value = DownloadState.Downloading(progress, totalBytesRead, contentLength)
                                } else {
                                    _downloadState.value = DownloadState.Downloading(-1f, totalBytesRead, 0L)
                                }
                            }
                        }
                    }

                    // Check integrity of downloaded file
                    if (ApkIntegrityVerifier.verifySha256(tempFile, expectedSha256)) {
                        if (destinationFile.exists()) {
                            destinationFile.delete()
                        }
                        tempFile.renameTo(destinationFile)
                        _downloadState.value = DownloadState.Success(destinationFile)
                        return@withContext destinationFile
                    } else {
                        tempFile.delete()
                        throw IOException("Downloaded APK has invalid SHA-256 signature")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                tempFile.delete()
                retryCount++
                if (retryCount < maxRetries) {
                    _downloadState.value = DownloadState.Downloading(0f, 0, 0)
                }
            }
        }

        _downloadState.value = DownloadState.Error(lastException?.message ?: "Unknown download failure")
        return@withContext null
    }

    fun cancelDownload() {
        activeCall?.cancel()
        activeCall = null
        _downloadState.value = DownloadState.Idle
    }
}
