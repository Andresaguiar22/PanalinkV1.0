package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.MessagesRepository
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
        val localUri = entity.localMediaUri

        // The upload may already have completed on a previous Worker attempt.
        // In that case the media URL is present and the only pending operation
        // is the metadata/message sync. Do not silently finish without syncing.
        if (localUri.isNullOrEmpty()) {
            if (!entity.mediaUrl.isNullOrEmpty() && entity.status == "sending") {
                Log.i(TAG, "Media already uploaded for $messageId; synchronizing message metadata")
                return try {
                    if (messagesRepository.syncPendingMessages()) {
                        Log.i(TAG, "Message metadata synchronized successfully: $messageId")
                        Result.success()
                    } else {
                        Log.w(TAG, "Message metadata sync returned false: $messageId")
                        Result.retry()
                    }
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
                try {
                    messageDao.updateMessageStatus(messageId, "failed")
                } catch (dbEx: Exception) {
                    Log.e(TAG, "Failed to update DB status to failed on missing file", dbEx)
                }
                return Result.failure()
            }

            val mimeType = entity.mediaMime ?: "application/octet-stream"
            val typeLabel = entity.messageType ?: "text"
            val userId = entity.senderId

            Log.i(TAG, "Processing and uploading $typeLabel ($mimeType), size=${file.length()} bytes")

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

            // Persist the remote media information before attempting the DB sync.
            // This makes the operation resumable if the process dies between upload and sync.
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

            if (shouldKeep) {
                messageDao.insertMessage(updatedEntity)
            } else {
                messageDao.deleteMessageById(updatedEntity.id)
                Log.i(TAG, "Message $messageId was filtered locally after upload; no remote sync needed")
                return Result.success()
            }

            // IMPORTANT FOR VOICE NOTES:
            // Do the metadata sync after the media upload has completed instead of
            // merely enqueueing another unique sync with KEEP. A running SyncWorker
            // could otherwise have already skipped this message while mediaUrl was null.
            return try {
                if (messagesRepository.syncPendingMessages()) {
                    Log.i(TAG, "Media + message sync completed successfully: $messageId")
                    Result.success()
                } else {
                    Log.w(TAG, "Media uploaded but message sync returned false: $messageId")
                    Result.retry()
                }
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
