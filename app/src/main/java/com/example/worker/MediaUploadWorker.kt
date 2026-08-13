package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PanaApplication
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
        val localUri = entity.localMediaUri ?: return Result.success() // Nothing to upload

        return try {
            val file = File(localUri)
            if (!file.exists()) {
                Log.e(TAG, "Local file does not exist: $localUri")
                try {
                    messageDao.updateMessageStatus(messageId, "failed")
                } catch (dbEx: Exception) {
                    Log.e(TAG, "Failed to update db status to failed on missing file", dbEx)
                }
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
                val msgsRepo = com.example.data.repository.MessagesRepository.getInstance()
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

                // Trigger metadata sync
                messagesRepository.scheduleSync()

                Result.success()
            } else {
                Log.e(TAG, "Upload failed: ${uploadResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaUploadWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
