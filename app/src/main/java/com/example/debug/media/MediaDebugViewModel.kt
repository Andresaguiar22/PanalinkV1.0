package com.example.debug.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.media.analytics.MediaAnalytics
import com.example.media.analytics.MediaHealthReport
import com.example.media.lifecycle.MediaLifecycleManager
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class MediaDebugUiState(
    val report: MediaHealthReport? = null,
    val localFilesCount: Int = 0,
    val formattedCacheSize: String = "0 MB",
    val isCleaning: Boolean = false,
    val message: String? = null
)

class MediaDebugViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = MediaStorageManager(application)
    private val repository = MediaRepository(application, storageManager)
    private val lifecycleManager = MediaLifecycleManager(application, repository, storageManager)

    private val _uiState = MutableStateFlow(MediaDebugUiState())
    val uiState: StateFlow<MediaDebugUiState> = _uiState

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val report = MediaAnalytics.getHealthReport(context)
            
            val baseDir = File(context.filesDir, "media")
            var count = 0
            var bytes = 0L
            if (baseDir.exists()) {
                baseDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        count++
                        bytes += file.length()
                    }
                }
            }

            val formattedSize = "%.2f MB".format(bytes.toDouble() / (1024 * 1024))

            _uiState.value = _uiState.value.copy(
                report = report,
                localFilesCount = count,
                formattedCacheSize = formattedSize
            )
        }
    }

    fun purgeExpiredCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val deletedCount = lifecycleManager.purgeExpiredCache(24 * 60 * 60 * 1000L)
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                message = "Limpieza completada: $deletedCount archivos purgados"
            )
            refreshStats()
        }
    }
}
