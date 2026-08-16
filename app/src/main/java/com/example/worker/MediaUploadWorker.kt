package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.MessagesRepository
import com.example.util.MessageFilter
import com.example.util.PanalinkMediaManager
import java.io.File

class MediaUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "MediaUploadWorker"
    private val messageDao = PanalinkDatabase.getDatabase(context).messageDao()
    private val messagesRepository = MessagesRepository.getInstance()

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        Log.i(TAG, "Starting media upload for message: $messageId")

        val entity = messageDao.getMessageById(messageId) ?: return Result.failure()

        val effectiveClearedAt = messagesRepository.getEffectiveClearedAt(entity.chatId, null)
        val shouldKeepBeforeUpload = MessageFilter.shouldKeepMessage(
            messageId = entity.id,
            messageClientUuid = entity.clientMessageUuid,
            messageCreatedAt = entity.createdAt,
            lastClearedAt = effectiveClearedAt,
            deletedMessageIds = messagesRepository.getUserDeletedMessageIds()
        )
        if (!shouldKeepBeforeUpload) {
            messageDao.deleteMessageById(entity.id)
            return Result.success()
        }

        val localUri = entity.localMediaUri
        if (localUri.isNullOrEmpty()) {
            // The file may already have been uploaded by a previous attempt.
            // In that case only the message metadata still needs to be synced.
            if (!entity.mediaUrl.isNullOrEmpty() && entity.status == "sending") {
                return try {
                    if (messagesRepository.syncPendingMessages()) Result.success() else Result.retry()
                } catch (e: Exception) {
                    Log.e(TAG, "Error synchronizing already-uploaded message $messageId", e)
                    Result.retry()
                }
            }
            return Result.success()
        }

        return try {
            val file = File(localUri)
            if (!file.exists() || file.length() <= 0L) {
                Log.e(TAG, "Local media file does not exist or is empty: $localUri")
                messageDao.updateMessageStatus(messageId, "failed")
                return Result.failure()
            }

            val mimeType = entity.mediaMime ?: "application/octet-stream"
            val typeLabel = entity.messageType ?: "text"
            val uploadResult = PanalinkMediaManager.uploadMediaAndThumbnail(
                context = context,
                mediaFile = file,
                mimeType = mimeType,
                typeLabel = typeLabel,
                userId = entity.senderId,
                caption = entity.content ?: "Multimedia message"
            )

            if (!uploadResult.isSuccess) {
                Log.e(TAG, "Media upload failed: ${uploadResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

            val mediaInfo = uploadResult.getOrThrow()
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

            val effectiveClearedAtAfterUpload = messagesRepository.getEffectiveClearedAt(updatedEntity.chatId, null)
            val shouldKeepAfterUpload = MessageFilter.shouldKeepMessage(
                messageId = updatedEntity.id,
                messageClientUuid = updatedEntity.clientMessageUuid,
                messageCreatedAt = updatedEntity.createdAt,
                lastClearedAt = effectiveClearedAtAfterUpload,
                deletedMessageIds = messagesRepository.getUserDeletedMessageIds()
            )

            if (!shouldKeepAfterUpload) {
                messageDao.deleteMessageById(updatedEntity.id)
                return Result.success()
            }

            messageDao.insertMessage(updatedEntity)

            // The media worker owns the upload. Once the URL exists, hand the
            // same pending message to the metadata sync path directly instead
            // of scheduling another SyncMessagesWorker cycle.
            try {
                if (messagesRepository.syncPendingMessages()) Result.success() else Result.retry()
            } catch (e: Exception) {
                Log.e(TAG, "Media uploaded but metadata sync failed: $messageId", e)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
