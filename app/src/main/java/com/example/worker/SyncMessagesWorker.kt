package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.MessagesRepository

class SyncMessagesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("SyncMessagesWorker", "Starting background sync of pending messages...")
        val repository = MessagesRepository.getInstance()

        return try {
            val allSynced = repository.syncAllPendingAndUpdatedMessages()

            if (allSynced) {
                Log.i("SyncMessagesWorker", "Background sync completed successfully")
                Result.success()
            } else if (runAttemptCount < 5) {
                Log.w(
                    "SyncMessagesWorker",
                    "Sync incomplete; scheduling retry ${runAttemptCount + 1}/5"
                )
                Result.retry()
            } else {
                Log.e("SyncMessagesWorker", "Sync incomplete after 5 attempts")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(
                "SyncMessagesWorker",
                "Error during sync attempt ${runAttemptCount + 1}/5: ${e.localizedMessage}",
                e
            )

            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
