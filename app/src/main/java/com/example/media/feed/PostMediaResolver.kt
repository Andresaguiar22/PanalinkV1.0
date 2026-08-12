package com.example.media.feed

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.CdnManager
import com.example.media.model.MediaResource
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import java.io.File

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
                if (file.exists()) {
                    MediaResource.Local(file.absolutePath)
                } else {
                    MediaResource.Missing
                }
            } else {
                val mediaId = remember(remoteUrl) {
                    "media_${kotlin.math.abs(remoteUrl.hashCode())}"
                }

                val mediaState by repository.observeMedia(mediaId)
                    .collectAsStateWithLifecycle(initialValue = null)

                // Resolve the current Supabase CDN candidate before exposing the
                // URL to Coil/Media3. This state is refreshed after the CDN
                // manager has revalidated the current endpoint, so a changed
                // Cloudflare URL can propagate without recreating the screen.
                val resolvedRemoteUrl by produceState(
                    initialValue = CdnManager.resolveMediaUrlSync(remoteUrl),
                    key1 = remoteUrl
                ) {
                    runCatching { CdnManager.getCDNUrl(forceRefresh = false) }
                    value = CdnManager.resolveMediaUrlSync(remoteUrl).ifBlank { remoteUrl }
                }

                LaunchedEffect(remoteUrl) {
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

    private fun typeFor(url: String): String {
        val normalized = url.lowercase()
        return if (
            normalized.contains("/videos/") ||
            normalized.endsWith(".mp4") ||
            normalized.endsWith(".webm") ||
            normalized.endsWith(".mov") ||
            normalized.endsWith(".m4v") ||
            normalized.contains("video")
        ) "video" else "image"
    }
}
