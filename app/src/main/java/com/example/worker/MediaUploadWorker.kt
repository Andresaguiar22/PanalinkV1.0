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

    /**
     * Sync the pending queue, but decide this worker's result from the state
     * of its own message. A different pending message must not make this
     * worker retry after its upload was already persisted successfully.
     */
    private suspend fun syncOwnMessageMetadata(messageId: String): Result {
        return try {
            messagesRepository.syncPendingMessages()

            // syncPendingMessages() may reconcile/replace the temporary row
            // with the server UUID, so the original worker id can disappear.
            val current = messageDao.getMessageById(messageId)
            when {
                current == null -> {
                    Log.d(TAG, "Message $messageId was reconciled/replaced/deleted during metadata sync")
                    Result.success()
                }
                current.status == "sent" && !current.mediaUrl.isNullOrBlank() -> {
                    Log.d(TAG, "Message $messageId metadata is synchronized")
                    Result.success()
                }
                else -> {
                    Log.w(TAG, "Message $messageId remains pending after metadata sync; retrying")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing metadata for message $messageId", e)
            Result.retry()
        }
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        Log.i(TAG, "Starting media upload for message: $messageId")

        val entity = messageDao.getMessageById(messageId) ?: return Result.success()

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

        // A media message must remain pending until both stages finish:
        // 1) the binary is uploaded and a mediaUrl exists;
        // 2) the message metadata is persisted in Supabase.
        // Never report success here while an unresolved media message still
        // has neither a local file nor a remote URL, otherwise WorkManager
        // drops the task and the message can remain stuck forever in Room.
        val localUri = entity.localMediaUri
        if (localUri.isNullOrEmpty()) {
            if (!entity.mediaUrl.isNullOrBlank() && entity.status != "sent") {
                Log.d(TAG, "Media already uploaded for $messageId; syncing message metadata")
                return syncOwnMessageMetadata(messageId)
            }

            if (entity.status == "sent") {
                return Result.success()
            }

            // There is no recoverable input left for this worker. Retrying
            // cannot recreate a deleted/missing local file and would only
            // loop the same broken job until WorkManager exhausts retries.
            Log.e(
                TAG,
                "Pending media message $messageId has no local URI and no mediaUrl; cannot recover upload"
            )
            messageDao.updateMessageStatus(messageId, "failed")
            return Result.failure()
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
            if (mediaInfo.url.isBlank()) {
                Log.e(TAG, "Media upload returned an empty URL for message $messageId")
                return Result.retry()
            }

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
            // same pending message to the metadata sync path directly.
            syncOwnMessageMetadata(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
