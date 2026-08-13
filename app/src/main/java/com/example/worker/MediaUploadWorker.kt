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

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        Log.i(TAG, "Starting media upload for message: $messageId")

        val entity = messageDao.getMessageById(messageId) ?: return Result.failure()
        val localUri = entity.localMediaUri ?: run {
            if (!entity.mediaUrl.isNullOrBlank()) {
                messagesRepository.syncPendingMessages()
            }
            return Result.success()
        }

        return try {
            val file = File(localUri)
            if (!file.exists()) {
                Log.e(TAG, "Local file does not exist: $localUri")
                messageDao.updateMessageStatus(messageId, "failed")
                return Result.failure()
            }

            val mimeType = entity.mediaMime ?: "application/octet-stream"
            val typeLabel = entity.messageType ?: "text"
            val userId = entity.senderId

            val uploadResult = PanalinkMediaManager.uploadMediaAndThumbnail(
                context = context,
                mediaFile = file,
                mimeType = mimeType,
                typeLabel = typeLabel,
                userId = userId,
                caption = entity.content ?: "Multimedia message"
            )

            if (!uploadResult.isSuccess) {
                Log.e(TAG, "Upload failed: ${uploadResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

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

            val effectiveClearedAt = messagesRepository.getEffectiveClearedAt(updatedEntity.chatId, null)
            val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                messageId = updatedEntity.id,
                messageClientUuid = updatedEntity.clientMessageUuid,
                messageCreatedAt = updatedEntity.createdAt,
                lastClearedAt = effectiveClearedAt,
                deletedMessageIds = messagesRepository.getUserDeletedMessageIds()
            )

            if (!shouldKeep) {
                messageDao.deleteMessageById(updatedEntity.id)
                return Result.success()
            }

            messageDao.insertMessage(updatedEntity)

            // The old implementation only scheduled a UNIQUE sync with KEEP here.
            // If another sync was already running, this upload could remain in Room as
            // "sending" even though its media was already uploaded and the recipient
            // could receive it. Reconcile immediately now that mediaUrl is available.
            val synced = messagesRepository.syncPendingMessages()
            if (!synced) {
                Log.w(TAG, "Metadata sync was partial; scheduling reconciliation for $messageId")
                messagesRepository.scheduleSync()
                return Result.retry()
            }

            // syncPendingMessages reconciles temp_* using clientMessageUuid. If the
            // definitive row is not yet visible locally, schedule another pass rather
            // than falsely treating the upload as permanently complete.
            val finalEntity = if (!updatedEntity.clientMessageUuid.isNullOrBlank()) {
                messageDao.getMessageByClientUuid(updatedEntity.clientMessageUuid!!)
            } else {
                messageDao.getMessageById(updatedEntity.id)
            }

            if (finalEntity != null && finalEntity.status == "sending") {
                Log.w(TAG, "Message $messageId still sending after reconciliation; retrying")
                messagesRepository.scheduleSync()
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
