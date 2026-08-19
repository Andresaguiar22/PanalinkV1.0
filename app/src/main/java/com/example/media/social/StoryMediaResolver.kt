package com.example.media.social

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserState
import com.example.data.repository.CdnManager
import com.example.media.model.MediaResource
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import java.io.File

object StoryMediaResolver {

    @Composable
    fun rememberResolvedStoryMediaResource(
        state: UserState
    ): MediaResource {
        val rawRemoteUrl = state.mediaUrl
        val localVideoPath = state.localVideoPath

        if (!localVideoPath.isNullOrBlank()) {
            val file = File(localVideoPath)
            if (file.exists() && file.length() > 0) {
                return MediaResource.Local(file.absolutePath)
            }
        }

        if (rawRemoteUrl.isNullOrBlank()) {
            return MediaResource.Missing
        }

        if (rawRemoteUrl.startsWith("file://") || rawRemoteUrl.startsWith("/")) {
            val cleanPath = rawRemoteUrl.removePrefix("file://")
            val file = File(cleanPath)
            return if (file.exists() && file.length() > 0) {
                MediaResource.Local(file.absolutePath)
            } else {
                MediaResource.Missing
            }
        }

        val remoteUrl = remember(rawRemoteUrl) {
            CdnManager.resolveMediaUrlSync(rawRemoteUrl)
        }

        val context = LocalContext.current
        val repository = remember {
            val storage = MediaStorageManager(context.applicationContext)
            MediaRepository(context.applicationContext, storage)
        }

        val mediaId = remember(rawRemoteUrl, state.id) {
            "story_${state.id}_${kotlin.math.abs(rawRemoteUrl.hashCode())}"
        }

        val mediaState by repository.observeMedia(mediaId)
            .collectAsStateWithLifecycle(initialValue = null)

        LaunchedEffect(remoteUrl, state.id) {
            if (mediaState == null || mediaState?.localPath.isNullOrBlank() || !File(mediaState?.localPath ?: "").exists()) {
                val type = if (state.mediaType == "video" || remoteUrl.endsWith(".mp4", ignoreCase = true) || remoteUrl.contains("/video/")) "video" else "image"
                repository.syncManager.syncMedia(mediaId, remoteUrl, type, state.userId)
            }
        }

        return remember(mediaState, remoteUrl) {
            val localPath = mediaState?.localPath
            if (!localPath.isNullOrBlank()) {
                val file = File(localPath)
                if (file.exists() && file.length() > 0) {
                    MediaResource.Local(file.absolutePath)
                } else {
                    MediaResource.Remote(remoteUrl)
                }
            } else {
                MediaResource.Remote(remoteUrl)
            }
        }
    }
}
