package com.example.media.dedup

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object MediaDeduplicationEngine {
    private const val TAG = "MediaDeduplicationEngine"

    fun calculateSha256(file: File): String? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calculating SHA-256 for ${file.name}", e)
            null
        }
    }

    /**
     * Deduplicates newly downloaded file against target storage directory.
     * Returns either the original file or an existing identical file.
     */
    fun deduplicateFile(newFile: File, targetDir: File): File {
        val newHash = calculateSha256(newFile) ?: return newFile
        
        targetDir.walkTopDown().forEach { existingFile ->
            if (existingFile.isFile && existingFile.absolutePath != newFile.absolutePath && existingFile.length() == newFile.length()) {
                val existingHash = calculateSha256(existingFile)
                if (existingHash == newHash) {
                    Log.i(TAG, "Deduplication hit: ${newFile.name} matches ${existingFile.name} (SHA-256: $newHash)")
                    newFile.delete() // Remove duplicate
                    return existingFile
                }
            }
        }

        return newFile
    }
}
