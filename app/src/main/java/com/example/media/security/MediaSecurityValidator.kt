package com.example.media.security

import android.util.Log
import java.io.File

object MediaSecurityValidator {
    private const val TAG = "MediaSecurityValidator"
    private const val MAX_ALLOWED_FILE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB max

    private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "mp4", "m4a", "mp3", "aac", "pdf", "txt", "tmp", "bin")

    fun sanitizePath(path: String): String {
        // Prevent Path Traversal attacks (/../ or ..\)
        return path.replace("..", "").replace("//", "/")
    }

    fun isPathSafe(baseDir: File, targetFile: File): Boolean {
        return try {
            val canonicalBase = baseDir.canonicalPath
            val canonicalTarget = targetFile.canonicalPath
            canonicalTarget.startsWith(canonicalBase)
        } catch (e: Exception) {
            Log.e(TAG, "Path safety check failed for $targetFile", e)
            false
        }
    }

    fun validateFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        if (file.length() <= 0 || file.length() > MAX_ALLOWED_FILE_SIZE_BYTES) {
            Log.w(TAG, "File size invalid: ${file.length()} bytes")
            return false
        }

        val ext = file.extension.lowercase()
        if (ext.isNotEmpty() && !ALLOWED_EXTENSIONS.contains(ext)) {
            Log.w(TAG, "Disallowed file extension: $ext")
            return false
        }

        return true
    }

    fun isUrlSafe(url: String): Boolean {
        if (url.isBlank()) return false
        if (url.contains("..")) return false
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("/")
    }
}
