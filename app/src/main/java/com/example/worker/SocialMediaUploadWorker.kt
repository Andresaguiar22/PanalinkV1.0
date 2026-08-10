package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.data.repository.StatesRepository
import com.example.data.repository.ProfilesRepository
import com.example.data.repository.UploadRepository
import java.io.File
import com.example.data.supabase.SupabaseClient

class SocialMediaUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "SocialMediaUploadWorker"
    private val db = PanalinkDatabase.getDatabase(context)
    private val pendingUploadDao = db.pendingUploadDao()
    private val statesRepository = StatesRepository()
    private val profilesRepository = ProfilesRepository()

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString("uploadId") ?: return Result.failure()
        Log.i(TAG, "Starting social media upload for: $uploadId")

        val entity = pendingUploadDao.getUploadById(uploadId) ?: run {
            Log.e(TAG, "PendingUploadEntity not found for id: $uploadId")
            return Result.failure()
        }

        // Validate local file
        val file = File(entity.localFilePath)
        if (!file.exists()) {
            Log.e(TAG, "Local file does not exist: ${entity.localFilePath}")
            val failedEntity = entity.copy(
                status = "failed",
                errorMessage = "Archivo local no encontrado",
                updatedAt = System.currentTimeMillis()
            )
            pendingUploadDao.updateUpload(failedEntity)
            return Result.failure()
        }

        // Update state to uploading
        val uploadingEntity = entity.copy(
            status = "uploading",
            updatedAt = System.currentTimeMillis()
        )
        pendingUploadDao.updateUpload(uploadingEntity)

        try {
            setProgress(workDataOf(
                "uploadId" to uploadId,
                "progress" to 10,
                "bytesWritten" to 0L,
                "totalBytes" to file.length(),
                "status" to "Iniciando subida...",
                "uploadType" to entity.uploadType
            ))

            var uploadedUrl: String? = entity.remoteUrl
            var finalUploadFile = file

            if (uploadedUrl == null && entity.mimeType.startsWith("video/") && !file.name.contains("_compressed_")) {
                setProgress(workDataOf(
                    "uploadId" to uploadId,
                    "progress" to 15,
                    "bytesWritten" to 0L,
                    "totalBytes" to file.length(),
                    "status" to "Comprimiendo video...",
                    "uploadType" to entity.uploadType
                ))
                try {
                    val pendingMediaDir = File(context.filesDir, "pending_media")
                    if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                    val compressedFile = File.createTempFile("reel_compressed_", ".mp4", pendingMediaDir)
                    
                    val compressed = com.example.util.VideoCompressorHelper.compressVideo(
                        context,
                        android.net.Uri.fromFile(file),
                        null
                    ) { compProgress ->
                        val p = 10 + (compProgress * 0.15).toInt()
                        setProgressAsync(workDataOf(
                            "uploadId" to uploadId,
                            "progress" to p,
                            "bytesWritten" to 0L,
                            "totalBytes" to file.length(),
                            "status" to "Comprimiendo video ($compProgress%)...",
                            "uploadType" to entity.uploadType
                        ))
                    }
                    if (compressed.exists() && compressed.length() > 0) {
                        if (file.name.contains("upload_temp_") || file.name.contains("reel_selected_")) {
                            try { file.delete() } catch (e: Exception) {}
                        }
                        finalUploadFile = compressed
                        val updatedEntity = uploadingEntity.copy(
                            localFilePath = compressed.absolutePath,
                            updatedAt = System.currentTimeMillis()
                        )
                        pendingUploadDao.updateUpload(updatedEntity)
                        Log.i(TAG, "Video compressed successfully: ${compressed.absolutePath}")
                    } else {
                        // Cleanup failed compressed file if created
                        if (compressed.exists()) {
                            try { compressed.delete() } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al comprimir video, subiendo original", e)
                }
            }

            // 1. Upload CDN if not already uploaded
            if (uploadedUrl == null) {
                val totalLength = finalUploadFile.length().coerceAtLeast(1L)
                setProgress(workDataOf(
                    "uploadId" to uploadId,
                    "progress" to 25,
                    "bytesWritten" to 0L,
                    "totalBytes" to totalLength,
                    "status" to "Subiendo archivo al CDN...",
                    "uploadType" to entity.uploadType
                ))

                // Perform direct upload to CDN via UploadRepository (this guarantees we don't delete on fail)
                val currentUid = entity.userId.ifEmpty { SupabaseClient.currentUser?.id ?: "anonymous" }
                val captionForUpload = entity.caption ?: "Social Media Upload"
                var lastUpdateMs = 0L

                val uploadResult = UploadRepository().uploadVideo(
                    mediaFile = finalUploadFile,
                    mediaMimeType = entity.mimeType,
                    caption = captionForUpload,
                    userId = currentUid,
                    onProgress = { bytesWritten, totalBytes ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMs > 300 || bytesWritten == totalBytes) {
                            lastUpdateMs = now
                            val uploadPct = 25 + ((bytesWritten.toDouble() / totalBytes.coerceAtLeast(1L).toDouble()) * 60.0).toInt()
                            setProgressAsync(workDataOf(
                                "uploadId" to uploadId,
                                "progress" to uploadPct,
                                "bytesWritten" to bytesWritten,
                                "totalBytes" to totalBytes,
                                "status" to "Subiendo archivo...",
                                "uploadType" to entity.uploadType
                            ))
                        }
                    }
                )

                if (uploadResult.isSuccess) {
                    uploadedUrl = uploadResult.getOrThrow().url
                    Log.d(TAG, "CDN upload successful: $uploadedUrl")
                    // Save remoteUrl in DB in case of future steps failing, so we don't re-upload to CDN!
                    val partialSavedEntity = uploadingEntity.copy(
                        remoteUrl = uploadedUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                    pendingUploadDao.updateUpload(partialSavedEntity)
                } else {
                    val error = uploadResult.exceptionOrNull()?.localizedMessage ?: "Unknown CDN upload error"
                    Log.e(TAG, "CDN upload failed: $error")
                    return handleFailure(entity, error)
                }
            }

            setProgress(workDataOf(
                "uploadId" to uploadId,
                "progress" to 85,
                "bytesWritten" to finalUploadFile.length(),
                "totalBytes" to finalUploadFile.length(),
                "status" to "Registrando publicación...",
                "uploadType" to entity.uploadType
            ))

            // 2. Register record in Supabase depending on uploadType
            var createdState: com.example.data.model.UserState? = null
            val success = when (entity.uploadType) {
                "STATE" -> {
                    val mediaType = when {
                        entity.mimeType.startsWith("video/") -> "video"
                        entity.mimeType.startsWith("audio/") -> "audio"
                        else -> "image"
                    }
                    val stateResult = statesRepository.createState(
                        mediaType = mediaType,
                        caption = entity.caption,
                        isReel = false,
                        presetMediaUrl = uploadedUrl
                    )
                    createdState = stateResult.getOrNull()
                    stateResult.isSuccess
                }
                "REEL" -> {
                    val mediaType = when {
                        entity.mimeType.startsWith("video/") -> "video"
                        entity.mimeType.startsWith("audio/") -> "audio"
                        else -> "image"
                    }
                    val stateResult = statesRepository.createState(
                        mediaType = mediaType,
                        caption = entity.caption,
                        isReel = true,
                        presetMediaUrl = uploadedUrl
                    )
                    createdState = stateResult.getOrNull()
                    stateResult.isSuccess
                }
                "PROFILE" -> {
                    // Get displayName from user profile first, then update profile photo url
                    val currentUid = entity.userId.ifEmpty { SupabaseClient.currentUser?.id ?: "anonymous" }
                    val profileResult = profilesRepository.getProfile(currentUid)
                    val displayName = profileResult.getOrNull()?.displayName ?: SupabaseClient.currentProfile?.displayName ?: "Usuario"
                    val updateResult = profilesRepository.updateProfile(currentUid, displayName, uploadedUrl)
                    updateResult.isSuccess
                }
                else -> {
                    Log.e(TAG, "Unknown uploadType: ${entity.uploadType}")
                    false
                }
            }

            if (success) {
                Log.i(TAG, "Upload completed successfully for $uploadId")
                
                // --- Save to Local States DB for Instant Visibility ---
                try {
                    val currentUid = entity.userId.ifEmpty { SupabaseClient.currentUser?.id ?: "anonymous" }
                    val myProfile = profilesRepository.getProfile(currentUid).getOrNull() 
                        ?: SupabaseClient.currentProfile 
                        ?: com.example.data.model.Profile(currentUid, "Yo", null)
                    
                    val newState = createdState ?: com.example.data.model.UserState(
                        id = java.util.UUID.randomUUID().toString(),
                        authorId = currentUid,
                        userIdField = currentUid,
                        mediaUrl = uploadedUrl ?: "",
                        mediaType = when {
                            entity.mimeType.startsWith("video/") -> "video"
                            entity.mimeType.startsWith("audio/") -> "audio"
                            else -> "image"
                        },
                        caption = entity.caption,
                        createdAt = SupabaseClient.getNowIsoString(),
                        type = if (entity.uploadType == "REEL") "reel" else "story",
                        localVideoPath = entity.localFilePath
                    )
                    
                    val stateWithUser = com.example.data.model.UserStateWithUser(newState, myProfile)
                    
                    // Cleanup optimistic states for this user/caption to avoid duplicates
                    try {
                        db.statesDao().deleteById("optimistic_$uploadId")
                        db.statesDao().deleteOptimistic(currentUid, entity.caption)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cleanup optimistic states", e)
                    }

                    // Save to Room via Repository
                    statesRepository.saveStateLocally(stateWithUser, entity.localFilePath)
                    Log.i(TAG, "Saved new state locally for instant UI update")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save state locally after upload", e)
                }

                setProgress(workDataOf(
                    "uploadId" to uploadId,
                    "progress" to 100,
                    "bytesWritten" to finalUploadFile.length(),
                    "totalBytes" to finalUploadFile.length(),
                    "status" to "Completado",
                    "uploadType" to entity.uploadType
                ))

                // Update Room to completed
                val completedEntity = entity.copy(
                    status = "completed",
                    remoteUrl = uploadedUrl,
                    updatedAt = System.currentTimeMillis()
                )
                pendingUploadDao.updateUpload(completedEntity)

                // Delete local file on absolute success ONLY if it's NOT a reel/story
                // For reels/stories, we keep the local file for instant playback from ROM (Internal Storage)
                try {
                    if (entity.uploadType != "REEL" && entity.uploadType != "STATE") {
                        if (finalUploadFile.exists()) {
                            finalUploadFile.delete()
                            Log.i(TAG, "Successfully cleaned up local file: ${finalUploadFile.absolutePath}")
                        }
                        if (file.exists() && file != finalUploadFile) {
                            file.delete()
                        }
                    } else {
                        Log.i(TAG, "Keeping local file for instant ROM playback: ${finalUploadFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete physical file", e)
                }

                return Result.success()
            } else {
                return handleFailure(entity, "Fallo al registrar en la base de datos de Supabase")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during social upload", e)
            return handleFailure(entity, e.localizedMessage ?: "Excepción desconocida")
        }
    }

    private suspend fun handleFailure(entity: PendingUploadEntity, error: String): Result {
        val nextRetryCount = entity.retryCount + 1
        return if (nextRetryCount >= 3) {
            Log.e(TAG, "Max retries reached. Marking upload as failed. Error: $error")
            val failedEntity = entity.copy(
                status = "failed",
                errorMessage = error,
                retryCount = nextRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            pendingUploadDao.updateUpload(failedEntity)
            Result.failure()
        } else {
            Log.w(TAG, "Temporary failure ($nextRetryCount/3). Scheduling retry. Error: $error")
            val retryingEntity = entity.copy(
                status = "pending", // Reset to pending for retry flow representation
                retryCount = nextRetryCount,
                errorMessage = error,
                updatedAt = System.currentTimeMillis()
            )
            pendingUploadDao.updateUpload(retryingEntity)
            Result.retry()
        }
    }
}
