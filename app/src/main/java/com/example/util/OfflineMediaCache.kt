package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.repository.CdnManager
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest

/** Persistent media store that is not subject to Android cache eviction. */
object OfflineMediaCache {
    private const val TAG = "OfflineMediaCache"
    private const val DIRECTORY = "offline_media"
    const val MAX_FILE_BYTES = 100L * 1024L * 1024L

    private fun root(context: Context): File =
        context.applicationContext.filesDir.resolve(DIRECTORY).also { it.mkdirs() }

    private fun canonicalKey(url: String): String {
        val trimmed = url.trim()
        val identity = if (CdnManager.isCdnRelated(trimmed)) {
            val path = try { URI(trimmed).path.orEmpty() } catch (_: Exception) { trimmed }
            "cdn:${path.substringAfterLast('/')}"
        } else {
            trimmed
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
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
        } catch (_: Exception) { ".bin" }
    }

    fun fileFor(context: Context, url: String, mime: String? = null): File =
        root(context).resolve(canonicalKey(url) + extension(url, mime))

    fun existingUri(context: Context, url: String?, mime: String? = null): String? {
        if (url.isNullOrBlank()) return null
        val file = fileFor(context, url, mime)
        return file.takeIf { it.isFile && it.length() > 0L }?.let { Uri.fromFile(it).toString() }
    }

    /** Streams an OkHttp response into persistent storage without readBytes(). */
    fun saveStream(context: Context, url: String, mime: String?, body: okhttp3.ResponseBody): String? {
        val target = fileFor(context, url, mime)
        if (target.isFile && target.length() > 0L) return Uri.fromFile(target).toString()
        val declared = body.contentLength()
        if (declared > MAX_FILE_BYTES) {
            Log.w(TAG, "Skipping oversized media: $declared bytes")
            return null
        }
        val tmp = File(target.parentFile, target.name + ".part")
        return try {
            body.byteStream().use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_FILE_BYTES) throw IllegalStateException("media exceeds persistent cache limit")
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            if (!tmp.renameTo(target)) throw IllegalStateException("unable to finalize cached media")
            Uri.fromFile(target).toString()
        } catch (t: Throwable) {
            tmp.delete()
            Log.w(TAG, "Persistent media download failed: ${t.message}")
            null
        }
    }

    fun clear(context: Context) {
        root(context).deleteRecursively()
    }
}
