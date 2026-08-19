package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest

/**
 * Persistent offline media store.
 *
 * Unlike Context.cacheDir, files here survive normal Android cache eviction.
 * The store is deliberately content-addressed by the remote URL so the same
 * media is never downloaded twice just because the CDN tunnel changed.
 */
object OfflineMediaCache {
    private const val TAG = "OfflineMediaCache"
    private const val DIRECTORY = "offline_media"
    private const val MAX_FILE_BYTES = 100L * 1024L * 1024L

    private fun root(context: Context): File =
        context.applicationContext.filesDir.resolve(DIRECTORY).also { it.mkdirs() }

    private fun key(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun extension(url: String, mime: String?): String {
        val fromMime = when {
            mime.equals("image/jpeg", true) -> ".jpg"
            mime.equals("image/png", true) -> ".png"
            mime.equals("image/webp", true) -> ".webp"
            mime.equals("video/mp4", true) -> ".mp4"
            mime.equals("audio/mp4", true) -> ".m4a"
            mime.equals("audio/mpeg", true) -> ".mp3"
            mime.equals("application/pdf", true) -> ".pdf"
            else -> ""
        }
        if (fromMime.isNotEmpty()) return fromMime
        return try {
            val path = URI(url).path.orEmpty()
            val ext = path.substringAfterLast('.', "")
            if (ext.length in 1..8) ".${ext.lowercase()}" else ".bin"
        } catch (_: Exception) {
            ".bin"
        }
    }

    fun fileFor(context: Context, url: String, mime: String? = null): File =
        root(context).resolve(key(url) + extension(url, mime))

    fun existingUri(context: Context, url: String?, mime: String? = null): String? {
        if (url.isNullOrBlank()) return null
        val file = fileFor(context, url, mime)
        return file.takeIf { it.isFile && it.length() > 0L }?.let { Uri.fromFile(it).toString() }
    }

    /** Streams one remote file into persistent storage. */
    fun download(context: Context, url: String, mime: String?, bytes: (String) -> ByteArray?): String? {
        if (url.isBlank()) return null
        val target = fileFor(context, url, mime)
        if (target.isFile && target.length() > 0L) return Uri.fromFile(target).toString()

        return try {
            val data = bytes(url) ?: return null
            if (data.size.toLong() > MAX_FILE_BYTES) {
                Log.w(TAG, "Skipping oversized media: ${data.size} bytes")
                return null
            }
            val tmp = File(target.parentFile, target.name + ".part")
            FileOutputStream(tmp).use { it.write(data) }
            if (!tmp.renameTo(target)) {
                tmp.delete()
                return null
            }
            Uri.fromFile(target).toString()
        } catch (t: Throwable) {
            Log.w(TAG, "Persistent media download failed: ${t.message}")
            null
        }
    }

    fun clear(context: Context) {
        root(context).deleteRecursively()
    }
}
