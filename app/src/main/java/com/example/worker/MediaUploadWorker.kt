package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.PanalinkDatabase
import com.example.util.PanalinkMediaManager
import com.example.data.repository.MessagesRepository
import com.example.data.repository.UploadRepository
import java.io.File

class MediaUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "MediaUploadWorker"
    private val messageDao = PanalinkDatabase.getDatabase(context).messageDao()
    private val uploadRepository = UploadRepository()
    private val messagesRepository = MessagesRepository.getInstance()

    companion object {
        private const val MAX_UPLOAD_ATTEMPTS = 5
    }

    private suspend fun markFailed(messageId: String) {
        try {
            messageDao.updateMessageStatus(messageId, "failed")
        } catch (dbEx: Exception) {
            Log.e(TAG, "Failed to persist terminal failed state for $messageId", dbEx)
        }
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        Log.i(TAG, "Starting media upload for message: $messageId (attempt=$runAttemptCount)")

        val entity = messageDao.getMessageById(messageId) ?: return Result.failure()
        val localUri = entity.localMediaUri

        // A worker can legitimately be replayed after the media URL was already
        // persisted. In that case there is nothing left to upload; let the
        // normal metadata reconciliation finish the message.
        if (localUri.isNullOrBlank()) {
            if (!entity.mediaUrl.isNullOrBlank()) {
                messagesRepository.scheduleSync()
                return Result.success()
            }
            markFailed(messageId)
            return Result.failure()
        }

        return try {
            val file = File(localUri)
            if (!file.exists()) {
                Log.e(TAG, "Local file does not exist: $localUri")
                markFailed(messageId)
                return Result.failure()
            }

            val mimeType = entity.mediaMime ?: "application/octet-stream"
            val typeLabel = entity.messageType ?: "text"
            val userId = entity.senderId

            Log.i(TAG, "Processing and uploading $typeLabel ($mimeType)...")

            val uploadResult = PanalinkMediaManager.uploadMediaAndThumbnail(
                context = context,
                mediaFile = file,
                mimeType = mimeType,
                typeLabel = typeLabel,
                userId = userId,
                caption = entity.content ?: "Multimedia message"
            )

            if (uploadResult.isSuccess) {
                val mediaInfo = uploadResult.getOrThrow()
                Log.i(TAG, "Upload successful: mediaUrl=${mediaInfo.url}, thumbUrl=${mediaInfo.thumbnailUrl}")

                val updatedEntity = entity.copy(
                    mediaUrl = mediaInfo.url,
                    thumbnailUrl = mediaInfo.thumbnailUrl ?: entity.thumbnailUrl,
                    mediaMime = mediaInfo.mime ?: entity.mediaMime,
                    mediaSize = mediaInfo.size,
                    mediaDuration = mediaInfo.duration,
                    mediaWidth = mediaInfo.width,
                    mediaHeight = mediaInfo.height,
                    localMediaUri = null,
                    status = "sending"
                )
                val msgsRepo = MessagesRepository.getInstance()
                val effectiveClearedAt = msgsRepo.getEffectiveClearedAt(updatedEntity.chatId, null)
                val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                    messageId = updatedEntity.id,
                    messageClientUuid = updatedEntity.clientMessageUuid,
                    messageCreatedAt = updatedEntity.createdAt,
                    lastClearedAt = effectiveClearedAt,
                    deletedMessageIds = msgsRepo.getUserDeletedMessageIds()
                )
                if (shouldKeep) {
                    messageDao.insertMessage(updatedEntity)
                } else {
                    messageDao.deleteMessageById(updatedEntity.id)
                }

                messagesRepository.scheduleSync()
                Result.success()
            } else {
                val error = uploadResult.exceptionOrNull()
                Log.e(TAG, "Upload failed (attempt=$runAttemptCount): ${error?.message}", error)
                if (runAttemptCount + 1 >= MAX_UPLOAD_ATTEMPTS) {
                    markFailed(messageId)
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker (attempt=$runAttemptCount): ${e.localizedMessage}", e)
            if (runAttemptCount + 1 >= MAX_UPLOAD_ATTEMPTS) {
                markFailed(messageId)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}