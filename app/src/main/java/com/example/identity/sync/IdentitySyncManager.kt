package com.example.identity.sync

import android.content.Context
import android.util.Log
import com.example.data.supabase.SupabaseClient
import com.example.identity.analytics.IdentityAnalytics
import com.example.identity.model.CachedProfile
import com.example.identity.model.ProfileSyncState
import com.example.identity.repository.IdentityRepository
import com.example.identity.storage.AvatarStorageManager
import com.example.identity.storage.CoverStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IdentitySyncManager(
    private val context: Context,
    private val repository: IdentityRepository,
    private val avatarStorage: AvatarStorageManager,
    private val coverStorage: CoverStorageManager
) {
    private val TAG = "IdentitySyncManager"

    suspend fun syncProfile(userId: String): ProfileSyncState = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val service = SupabaseClient.apiService ?: return@withContext ProfileSyncState.FAILED
            val token = SupabaseClient.currentToken ?: return@withContext ProfileSyncState.FAILED
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val response = try {
                service.getProfile(apiKey, bearer, "eq.$userId")
            } catch (e: Exception) {
                null
            }

            if (response?.isSuccessful == true) {
                val remoteProfile = response.body()?.firstOrNull()
                if (remoteProfile != null) {
                    val existing = repository.getProfile(userId)
                    
                    val updatedProfile = existing?.copy(
                        profile = remoteProfile,
                        lastSyncedAt = System.currentTimeMillis(),
                        isDirty = false
                    ) ?: CachedProfile(
                        profile = remoteProfile,
                        lastSyncedAt = System.currentTimeMillis()
                    )

                    repository.saveProfile(updatedProfile)

                    // Download avatar if missing
                    if (remoteProfile.avatarUrl != null && updatedProfile.avatarLocalPath == null) {
                        avatarStorage.downloadAvatar(userId, remoteProfile.avatarUrl)
                    }

                    // Download cover if missing
                    // if (remoteProfile.coverUrl != null && updatedProfile.coverLocalPath == null) {
                    //     coverStorage.downloadCover(userId, remoteProfile.coverUrl)
                    // }

                    val timeTaken = System.currentTimeMillis() - startTime
                    IdentityAnalytics.trackProfileSync(true, timeTaken)
                    return@withContext ProfileSyncState.SUCCESS
                }
            }

            // Fallback to existing
            val existing = repository.getProfile(userId)
            if (existing != null) {
                return@withContext ProfileSyncState.SUCCESS
            }

            ProfileSyncState.FAILED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile $userId", e)
            IdentityAnalytics.trackProfileSync(false, 0)
            ProfileSyncState.FAILED
        }
    }

    suspend fun syncProfiles(userIds: List<String>) = withContext(Dispatchers.IO) {
        // Batch sync logic
        userIds.forEach { syncProfile(it) }
    }
}
