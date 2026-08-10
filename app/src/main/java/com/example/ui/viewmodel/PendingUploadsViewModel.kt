package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.PanaApplication
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.data.supabase.SupabaseClient
import com.example.util.MediaCleanupManager
import com.example.worker.SocialMediaUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UploadProgressInfo(
    val uploadId: String,
    val progressPercent: Int,
    val bytesWritten: Long,
    val totalBytes: Long,
    val statusText: String
)

class PendingUploadsViewModel : ViewModel() {
    private val db = PanalinkDatabase.getDatabase(PanaApplication.instance)
    private val pendingUploadDao = db.pendingUploadDao()

    private val _activeUploads = MutableStateFlow<List<PendingUploadEntity>>(emptyList())
    val activeUploads: StateFlow<List<PendingUploadEntity>> = _activeUploads.asStateFlow()

    private val _allUploads = MutableStateFlow<List<PendingUploadEntity>>(emptyList())
    val allUploads: StateFlow<List<PendingUploadEntity>> = _allUploads.asStateFlow()

    private val _uploadProgressMap = MutableStateFlow<Map<String, UploadProgressInfo>>(emptyMap())
    val uploadProgressMap: StateFlow<Map<String, UploadProgressInfo>> = _uploadProgressMap.asStateFlow()

    init {
        observeUploads()
        observeWorkManagerProgress()
        runAutoCleanup(PanaApplication.instance)
    }

    private fun observeWorkManagerProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WorkManager.getInstance(PanaApplication.instance)
                    .getWorkInfosByTagFlow("social_upload")
                    .collect { workInfos ->
                        val newMap = mutableMapOf<String, UploadProgressInfo>()
                        for (info in workInfos) {
                            if (info.state == androidx.work.WorkInfo.State.RUNNING ||
                                info.state == androidx.work.WorkInfo.State.ENQUEUED) {
                                val progressData = info.progress
                                val uploadId = progressData.getString("uploadId")
                                    ?: info.tags.firstOrNull { it.startsWith("upload_") }?.removePrefix("upload_")
                                if (uploadId != null) {
                                    val percent = progressData.getInt("progress", 0)
                                    val bytesWritten = progressData.getLong("bytesWritten", 0L)
                                    val totalBytes = progressData.getLong("totalBytes", 0L)
                                    val statusText = progressData.getString("status") ?: "Procesando..."
                                    newMap[uploadId] = UploadProgressInfo(
                                        uploadId = uploadId,
                                        progressPercent = percent,
                                        bytesWritten = bytesWritten,
                                        totalBytes = totalBytes,
                                        statusText = statusText
                                    )
                                }
                            }
                        }
                        _uploadProgressMap.value = newMap
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeUploads() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = SupabaseClient.currentUser?.id.orEmpty()
            val activeFlow = if (userId.isNotEmpty()) {
                pendingUploadDao.getActiveUploadsByUserFlow(userId)
            } else {
                pendingUploadDao.getAllActiveUploadsFlow()
            }
            launch {
                activeFlow.collect { list ->
                    _activeUploads.value = list
                }
            }

            val allFlow = if (userId.isNotEmpty()) {
                pendingUploadDao.getUploadsByUser(userId)
            } else {
                pendingUploadDao.getAllUploadsFlow()
            }
            launch {
                allFlow.collect { list ->
                    _allUploads.value = list
                }
            }
        }
    }

    fun retryUpload(context: Context, uploadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = pendingUploadDao.getUploadById(uploadId) ?: return@launch
            val updated = entity.copy(
                status = "pending",
                errorMessage = null,
                retryCount = 0,
                updatedAt = System.currentTimeMillis()
            )
            pendingUploadDao.updateUpload(updated)

            val uploadWorkRequest = OneTimeWorkRequestBuilder<SocialMediaUploadWorker>()
                .setInputData(workDataOf("uploadId" to uploadId))
                .addTag("social_upload")
                .addTag("upload_$uploadId")
                .addTag("social_upload_$uploadId")
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "social_upload_$uploadId",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
        }
    }

    fun cancelUpload(context: Context, uploadId: String, deleteLocalFile: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WorkManager.getInstance(context.applicationContext)
                    .cancelUniqueWork("social_upload_$uploadId")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val entity = pendingUploadDao.getUploadById(uploadId)
            if (entity != null) {
                if (deleteLocalFile) {
                    try {
                        val file = File(entity.localFilePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    pendingUploadDao.deleteUploadById(uploadId)
                } else {
                    val cancelledEntity = entity.copy(
                        status = "cancelled",
                        errorMessage = "Cancelado por el usuario",
                        updatedAt = System.currentTimeMillis()
                    )
                    pendingUploadDao.updateUpload(cancelledEntity)
                }
            }
        }
    }

    fun dismissUpload(uploadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingUploadDao.deleteUploadById(uploadId)
        }
    }

    fun clearCompletedUploads() {
        viewModelScope.launch(Dispatchers.IO) {
            pendingUploadDao.clearCompletedUploads()
        }
    }

    fun runAutoCleanup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            MediaCleanupManager.cleanOrphanMedia(context)
        }
    }
}
