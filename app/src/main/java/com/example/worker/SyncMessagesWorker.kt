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
        try {
        } catch (e: Exception) {}
        
        val result = try {
            val allSynced = repository.syncAllPendingAndUpdatedMessages()
            if (allSynced) {
                Result.success()
            } else {
                if (runAttemptCount < 5) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error during sync: ${e.localizedMessage}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }

        try {
        } catch (e: Exception) {}

        return result
    }
}
