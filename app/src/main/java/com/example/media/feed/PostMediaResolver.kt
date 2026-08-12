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

                // Always resolve legacy/dynamic CDN URLs before downloading.
                // Supabase Storage and unrelated external URLs are left untouched
                // by CdnManager; only known CDN origins are rewritten.
                val resolvedRemoteUrl = remember(remoteUrl) {
                    CdnManager.resolveMediaUrlSync(remoteUrl).ifBlank { remoteUrl }
                }

                LaunchedEffect(remoteUrl) {
                    // Ensure the current Supabase CDN candidate has a chance to
                    // become active before syncing a legacy CDN URL.
                    runCatching { CdnManager.getCDNUrl(forceRefresh = false) }
                    repository.syncManager.syncMedia(mediaId, remoteUrl, typeFor(remoteUrl), ownerId)
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
                        // The remote fallback must also use the active CDN so a
                        // changed Cloudflare URL is immediately consumable by UI.
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
