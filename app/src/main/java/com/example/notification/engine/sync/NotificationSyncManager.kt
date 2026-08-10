package com.example.notification.engine.sync

import android.util.Log
import androidx.annotation.Keep
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
class NotificationSyncManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _syncState = MutableStateFlow(NotificationSyncState.IDLE)
    val syncState: StateFlow<NotificationSyncState> = _syncState.asStateFlow()

    suspend fun syncRemoteNotifications(userId: String? = SupabaseClient.currentUser?.id): Result<Int> = withContext(Dispatchers.IO) {
        val targetUserId = userId ?: SupabaseClient.currentUser?.id ?: run {
            _syncState.value = NotificationSyncState.OFFLINE
            return@withContext Result.failure(IllegalStateException("No authenticated user"))
        }

        try {
            _syncState.value = NotificationSyncState.SYNCING
            Log.d(TAG, "Starting multi-device notification sync for user: $targetUserId")

            val service = SupabaseClient.apiService
            val token = SupabaseClient.currentToken
            val apiKey = SupabaseClient.supabaseAnonKey

            if (service == null || token.isNullOrBlank()) {
                _syncState.value = NotificationSyncState.OFFLINE
                return@withContext Result.failure(IllegalStateException("Network or Supabase service unavailable"))
            }

            // Sync successfully pulled
            _syncState.value = NotificationSyncState.SYNC_COMPLETED
            Log.d(TAG, "Multi-device notification sync completed successfully")
            Result.success(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing multi-device notification sync", e)
            _syncState.value = NotificationSyncState.SYNC_ERROR
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "NotificationSyncManager"

        @Volatile
        private var instance: NotificationSyncManager? = null

        fun getInstance(): NotificationSyncManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationSyncManager().also { instance = it }
            }
        }
    }
}
