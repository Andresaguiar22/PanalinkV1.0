package com.example.media.feed

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

                LaunchedEffect(remoteUrl) {
                    if (mediaState == null || mediaState?.localPath.isNullOrBlank() || !File(mediaState?.localPath ?: "").exists()) {
                        val type = if (remoteUrl.endsWith(".mp4") || remoteUrl.contains("video")) "video" else "image"
                        repository.syncManager.syncMedia(mediaId, remoteUrl, type, ownerId)
                    }
                }

                remember(mediaState, remoteUrl) {
                    val localPath = mediaState?.localPath
                    if (!localPath.isNullOrBlank()) {
                        val file = File(localPath)
                        if (file.exists() && file.length() > 0) {
                            MediaResource.Local(file.absolutePath)
                        } else {
                            MediaResource.Remote(remoteUrl)
                        }
                    } else if (mediaState?.syncState == "FAILED") {
                        MediaResource.Remote(remoteUrl)
                    } else {
                        MediaResource.Remote(remoteUrl)
                    }
                }
            }
        }
    }
}
