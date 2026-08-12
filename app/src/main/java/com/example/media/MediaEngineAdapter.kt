package com.example.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import java.io.File
import java.net.URI

object MediaEngineAdapter {

    @Composable
    fun rememberResolvedMediaUrl(remoteUrl: String?, type: String, ownerId: String? = null): String? {
        if (remoteUrl.isNullOrBlank() || remoteUrl.startsWith("file://") || remoteUrl.startsWith("/")) return remoteUrl

        // Never include the CDN host in the persistent media identity. Dynamic
        // Cloudflare URLs must continue to point to the same local asset.
        val mediaId = remember(remoteUrl, type) {
            stableMediaId(remoteUrl, type)
        }

        val context = LocalContext.current
        val repository = remember {
            val storage = MediaStorageManager(context.applicationContext)
            MediaRepository(context.applicationContext, storage)
        }

        val mediaState by repository.observeMedia(mediaId)
            .collectAsStateWithLifecycle(initialValue = null)

        LaunchedEffect(remoteUrl, mediaId) {
            val localPath = mediaState?.localPath
            if (localPath.isNullOrBlank() || !File(localPath).exists() || File(localPath).length() <= 0L) {
                repository.syncManager.syncMedia(mediaId, remoteUrl, type, ownerId)
            }
        }

        return remember(mediaState, remoteUrl) {
            val localPath = mediaState?.localPath
            if (!localPath.isNullOrBlank()) {
                val file = File(localPath)
                if (file.exists() && file.length() > 0) {
                    return@remember file.absolutePath
                }
            }
            remoteUrl
        }
    }

    private fun stableMediaId(url: String, type: String): String {
        val logicalKey = runCatching {
            val uri = URI(url)
            buildString {
                append(type.uppercase()).append('|')
                append(uri.path ?: url)
                uri.query?.let { append('?').append(it) }
            }
        }.getOrElse { "${type.uppercase()}|$url" }

        return "media_${logicalKey.hashCode().toUInt().toString(16)}"
    }
}
