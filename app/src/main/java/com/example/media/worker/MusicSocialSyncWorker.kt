package com.example.media.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.PanalinkDatabase
import com.example.media.audio.AudioRepository
import com.example.media.playlist.PlaylistRepository
import com.example.media.sync.MusicSocialSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * P6.7.6B - Music Social Sync Worker
 * Handles background synchronization of music metadata with Supabase.
 */
class MusicSocialSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "MusicSocialSyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting background Music Social Sync")
            
            val db = PanalinkDatabase.getDatabase(applicationContext)
            val playlistRepo = PlaylistRepository(db.playlistDao(), db.collaboratorDao())
            val invitationRepo = com.example.media.playlist.PlaylistInvitationRepository(
                db.invitationDao(),
                com.example.data.supabase.SupabaseClient.apiService ?: throw Exception("Supabase API not ready"),
                com.example.data.supabase.SupabaseClient.supabaseAnonKey
            )
            val audioRepo = AudioRepository(db.audioDao())
            
            val syncManager = MusicSocialSyncManager(
                context = applicationContext,
                supabaseApi = com.example.data.supabase.SupabaseClient.apiService ?: throw Exception("Supabase API not ready"),
                playlistRepo = playlistRepo,
                invitationRepo = invitationRepo,
                audioRepo = audioRepo,
                apiKey = com.example.data.supabase.SupabaseClient.supabaseAnonKey
            )
            
            val userId = com.example.data.supabase.SessionManager.getCurrentUserId() ?: return@withContext Result.failure()
            val authToken = com.example.data.supabase.SupabaseClient.currentToken ?: return@withContext Result.failure()

            // Perform full sync
            syncManager.syncFull(userId, authToken)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MusicSocialSyncWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<MusicSocialSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MusicSocialSyncWorkerJob",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
        
        fun runOnce(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<MusicSocialSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
}
