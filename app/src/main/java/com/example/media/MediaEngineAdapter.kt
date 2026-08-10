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

object MediaEngineAdapter {
    
    @Composable
    fun rememberResolvedMediaUrl(remoteUrl: String?, type: String, ownerId: String? = null): String? {
        if (remoteUrl.isNullOrBlank() || remoteUrl.startsWith("file://") || remoteUrl.startsWith("/")) return remoteUrl
        
        val mediaId = remember(remoteUrl) {
            "media_${kotlin.math.abs(remoteUrl.hashCode())}"
        }
        
        val context = LocalContext.current
        val repository = remember {
            val storage = MediaStorageManager(context.applicationContext)
            MediaRepository(context.applicationContext, storage)
        }
        
        val mediaState by repository.observeMedia(mediaId).collectAsStateWithLifecycle(initialValue = null)
        
        LaunchedEffect(remoteUrl) {
            if (mediaState == null || mediaState?.localPath.isNullOrBlank() || !File(mediaState?.localPath ?: "").exists()) {
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
}
