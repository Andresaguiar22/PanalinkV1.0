package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.MessagesRepository
<<<<<<< HEAD
import com.example.data.repository.UploadRepository
import com.example.util.MessageFilter
import com.example.service.PanaLinkNotificationManager
=======
import com.example.util.MessageFilter
import com.example.util.PanalinkMediaManager
>>>>>>> origin/refactor/full-app-turbo
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
                runAttemptCount >= MAX_ATTEMPTS -> {
                    Log.e(TAG, "Message $messageId metadata sync exhausted retries; marking failed")
                    messageDao.updateMessageStatus(messageId, "failed")
                    Result.failure()
                }
                else -> {
                    Log.w(TAG, "Message $messageId remains pending after metadata sync; retrying")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing metadata for message $messageId", e)
            if (runAttemptCount >= MAX_ATTEMPTS) {
                messageDao.updateMessageStatus(messageId, "failed")
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        Log.i(TAG, "Starting media upload for message: $messageId (attempt=${runAttemptCount + 1})")

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
        val localUri = entity.localMediaUri
        if (localUri.isNullOrEmpty()) {
            if (!entity.mediaUrl.isNullOrBlank() && entity.status != "sent") {
                Log.d(TAG, "Media already uploaded for $messageId; syncing message metadata")
                return syncOwnMessageMetadata(messageId)
            }

            if (entity.status == "sent") {
                return Result.success()
            }

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

<<<<<<< HEAD
            if (uploadResult.isSuccess) {
                val mediaInfo = uploadResult.getOrThrow()
                Log.i(TAG, "Upload successful: mediaUrl=${mediaInfo.url}, thumbUrl=${mediaInfo.thumbnailUrl}")

                // Update Room with remote URLs and full metadata
                val updatedEntity = entity.copy(
                    mediaUrl = mediaInfo.url,
                    thumbnailUrl = mediaInfo.thumbnailUrl ?: entity.thumbnailUrl,
                    mediaMime = mediaInfo.mime ?: entity.mediaMime,
                    mediaSize = mediaInfo.size,
                    mediaDuration = mediaInfo.duration,
                    mediaWidth = mediaInfo.width,
                    mediaHeight = mediaInfo.height,
                    localMediaUri = null,
                    status = "sending" // Ready for metadata sync
                )

                val effectiveClearedAt = messagesRepository.getEffectiveClearedAt(updatedEntity.chatId, null)
                val shouldKeep = MessageFilter.shouldKeepMessage(
                    messageId = updatedEntity.id,
                    messageClientUuid = updatedEntity.clientMessageUuid,
                    messageCreatedAt = updatedEntity.createdAt,
                    lastClearedAt = effectiveClearedAt,
                    deletedMessageIds = messagesRepository.getUserDeletedMessageIds()
                )

                if (!shouldKeep) {
                    messageDao.deleteMessageById(updatedEntity.id)
                } else {
                    messageDao.insertMessage(updatedEntity)
                    // Trigger metadata sync
                    messagesRepository.scheduleSync()
                    PanaLinkNotificationManager.showUploadSuccessNotification(context)
                }

                Result.success()
=======
            if (!uploadResult.isSuccess) {
                val error = uploadResult.exceptionOrNull()
                Log.e(TAG, "Media upload failed: ${error?.message}", error)
                if (runAttemptCount >= MAX_ATTEMPTS) {
                    messageDao.updateMessageStatus(messageId, "failed")
                    Result.failure()
                } else {
                    Result.retry()
                }
>>>>>>> origin/refactor/full-app-turbo
            } else {
                val mediaInfo = uploadResult.getOrThrow()
                if (mediaInfo.url.isBlank()) {
                    Log.e(TAG, "Media upload returned an empty URL for message $messageId")
                    if (runAttemptCount >= MAX_ATTEMPTS) {
                        messageDao.updateMessageStatus(messageId, "failed")
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                } else {
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
                        Result.success()
                    } else {
                        messageDao.insertMessage(updatedEntity)
                        // Once the URL exists, the metadata sync path is responsible
                        // for replacing the temporary row or marking it SENT.
                        val syncResult = syncOwnMessageMetadata(messageId)
                        if (syncResult is Result.Success) {
                            com.example.service.PanaLinkNotificationManager.showUploadSuccessNotification(context)
                        }
                        syncResult
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker: ${e.localizedMessage}", e)
            if (runAttemptCount >= MAX_ATTEMPTS) {
                messageDao.updateMessageStatus(messageId, "failed")
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
