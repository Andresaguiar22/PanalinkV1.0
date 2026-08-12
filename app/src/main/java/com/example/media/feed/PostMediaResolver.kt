package com.example.media.feed

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.CdnManager
import com.example.media.model.MediaResource
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import java.io.File
import java.net.URI

object PostMediaResolver {

    @Composable
    fun rememberResolvedMediaResources(
        mediaUrls: List<String>?,
        ownerId: String? = null
    ): List<MediaResource> {
        if (mediaUrls.isNullOrEmpty()) return emptyList()

        val context = LocalContext.current
        val repository = remember {
            val storage = MediaStorageManager(context.applicationContext)
            MediaRepository(context.applicationContext, storage)
        }

        return mediaUrls.map { remoteUrl ->
            if (remoteUrl.startsWith("file://") || remoteUrl.startsWith("/")) {
                val file = File(remoteUrl.replace("file://", ""))
                if (file.exists() && file.length() > 0) {
                    MediaResource.Local(file.absolutePath)
                } else {
                    MediaResource.Missing
                }
            } else {
                // Cache identity is based on the logical media path, not the CDN host.
                // A Cloudflare/bore URL rotation therefore keeps the same local file.
                val mediaId = remember(remoteUrl) { stableMediaId(remoteUrl) }

                val mediaState by repository.observeMedia(mediaId)
                    .collectAsStateWithLifecycle(initialValue = null)

                val resolvedRemoteUrl by produceState(
                    initialValue = CdnManager.resolveMediaUrlSync(remoteUrl),
                    key1 = remoteUrl
                ) {
                    runCatching { CdnManager.getCDNUrl(forceRefresh = false) }
                    value = CdnManager.resolveMediaUrlSync(remoteUrl).ifBlank { remoteUrl }
                }

                LaunchedEffect(remoteUrl, mediaId) {
                    repository.syncManager.syncMedia(
                        mediaId,
                        remoteUrl,
                        typeFor(remoteUrl),
                        ownerId
                    )
                }

                remember(mediaState, remoteUrl, resolvedRemoteUrl) {
                    val localPath = mediaState?.localPath
                    if (!localPath.isNullOrBlank()) {
                        val file = File(localPath)
                        if (file.exists() && file.length() > 0) {
                            MediaResource.Local(file.absolutePath)
                        } else {
                            MediaResource.Remote(resolvedRemoteUrl)
                        }
                    } else {
                        MediaResource.Remote(resolvedRemoteUrl)
                    }
                }
            }
        }
    }

    private fun stableMediaId(url: String): String {
        return runCatching {
            val uri = URI(url)
            val logicalKey = buildString {
                append(uri.path ?: url)
                uri.query?.let { append('?').append(it) }
            }
            "media_${kotlin.math.abs(logicalKey.hashCode())}"
        }.getOrElse {
            "media_${kotlin.math.abs(url.hashCode())}"
        }
    }

    private fun typeFor(url: String): String {
        val normalized = url.lowercase().substringBefore('#').substringBefore('?')
        return if (
            normalized.contains("/videos/") ||
            normalized.endsWith(".mp4") ||
            normalized.endsWith(".webm") ||
            normalized.endsWith(".mov") ||
            normalized.endsWith(".m4v") ||
            normalized.contains("video")
        ) "POST_VIDEO" else "POST_IMAGE"
    }
}
