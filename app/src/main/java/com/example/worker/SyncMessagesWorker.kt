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
                try {
                    applicationContext.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putLong(com.example.ui.settings.models.SettingsKeys.LAST_MESSAGES_SYNC_AT, System.currentTimeMillis())
                        .apply()
                } catch (e: Exception) {
                    Log.w("SyncMessagesWorker", "Could not persist last sync timestamp", e)
                }
                Result.success()
            } else {
                // IMPORTANT: never terminate the unique sync chain as FAILED.
                // MessagesRepository uses enqueueUniqueWork(..., KEEP, ...). A FAILED
                // unique work remains the active work and later KEEP enqueues are ignored,
                // which can strand pending messages forever. Keep the worker retryable so
                // WorkManager's persistent backoff can resume the queue when connectivity,
                // auth or the server becomes healthy again.
                Log.w(
                    "SyncMessagesWorker",
                    "Sync incomplete; keeping persistent retry (attempt=${runAttemptCount + 1})"
                )
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(
                "SyncMessagesWorker",
                "Error during sync attempt ${runAttemptCount + 1}: ${e.localizedMessage}",
                e
            )

            // Same rule as above: a sync failure is recoverable state, not terminal state.
            Result.retry()
        }
    }
}
