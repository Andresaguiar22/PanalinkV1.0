package com.example.creative.post

import android.content.Context
import android.net.Uri
import com.example.creative.core.CreativeLayer
import com.example.media.dedup.MediaDeduplicationEngine
import com.example.media.storage.MediaStorageManager
import java.io.File
import java.io.FileOutputStream

/**
 * P6.6.2 - Post Media Importer
 * Transforms gallery URIs into internal deduplicated files and CreativeLayers.
 */
object PostMediaImporter {

    fun importMediaUri(
        context: Context,
        uri: Uri,
        pageIndex: Int = 0,
        aspectRatio: String = "4:5"
    ): PostPage {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val isVideo = mimeType.contains("video", ignoreCase = true) || uri.toString().endsWith(".mp4", ignoreCase = true)

        // 1. Copy uri to temp file
        val tempDir = File(context.cacheDir, "post_imports")
        if (!tempDir.exists()) tempDir.mkdirs()

        val extension = if (isVideo) ".mp4" else ".jpg"
        val tempFile = File(tempDir, "import_${System.currentTimeMillis()}$extension")

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Pass through MediaStorageManager & Deduplication
        val storageManager = MediaStorageManager(context)
        val finalFile = if (tempFile.exists() && tempFile.length() > 0) {
            val dedupDir = File(context.filesDir, "media_dedup")
            if (!dedupDir.exists()) dedupDir.mkdirs()
            MediaDeduplicationEngine.deduplicateFile(tempFile, dedupDir)
        } else {
            tempFile
        }

        val filePath = finalFile.absolutePath

        // 3. Create CreativeLayer
        val mainLayer: CreativeLayer = if (isVideo) {
            CreativeLayer.Video(
                id = java.util.UUID.randomUUID().toString(),
                videoUriOrPath = filePath,
                xFraction = 0.5f,
                yFraction = 0.5f,
                scale = 1.0f,
                rotation = 0f
            )
        } else {
            CreativeLayer.Image(
                id = java.util.UUID.randomUUID().toString(),
                imageUriOrPath = filePath,
                xFraction = 0.5f,
                yFraction = 0.5f,
                scale = 1.0f,
                rotation = 0f
            )
        }

        return PostPage(
            id = java.util.UUID.randomUUID().toString(),
            pageIndex = pageIndex,
            layers = listOf(mainLayer),
            aspectRatio = aspectRatio
        )
    }

    fun importMultipleMedia(
        context: Context,
        uris: List<Uri>,
        aspectRatio: String = "4:5"
    ): List<PostPage> {
        return uris.mapIndexed { idx, uri ->
            importMediaUri(context, uri, pageIndex = idx, aspectRatio = aspectRatio)
        }
    }
}
