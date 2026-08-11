package com.example.data.repository

import android.util.Log
import com.example.data.database.PanalinkDatabase
import com.example.PanaApplication
import com.example.data.model.Notification
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import com.example.data.database.LocalNotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class NotificationsRepository {
    private val TAG = "NotificationsRepository"
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = PanalinkDatabase.getDatabase(PanaApplication.instance).localNotificationDao()

    val notifications: Flow<List<Notification>> = dao.getNotificationsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val unreadCount: Flow<Int> = dao.getUnreadCountFlow()

    suspend fun fetchNotifications(): Result<List<Notification>> = withContext(Dispatchers.IO) {
        try {
            SessionManager.validateAndRefreshSessionIfNeeded()
            val service = SupabaseClient.apiService
            val token = SupabaseClient.currentToken
            if (service == null || token == null) {
                return@withContext Result.success(emptyList())
            }
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey
            val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.success(emptyList())

            val response = service.getNotifications(apiKey, bearer, "eq.$currentUid", select = "*")
            
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val parsed = dtos.map { it.toDomain() }
                
                // Fetch profiles for these notifications
                val userIds = dtos.mapNotNull { it.actorId }.filter { it.isNotBlank() }.distinct()
                val publicResult = PublicProfileRepository.getInstance().getPublicProfiles(userIds)
                val publicProfilesMap = if (publicResult is PublicProfileFetchResult.Success) {
                    publicResult.data
                } else emptyMap()

                val resolvedNotifications = parsed.map { notif ->
                    val profile = publicProfilesMap[notif.profile.id]
                    if (profile != null) {
                        notif.copy(
                            profile = notif.profile.copy(
                                displayName = PublicProfileResolver.resolveDisplayName(profile, notif.profile.displayName, notif.profile.id),
                                avatarUrl = CdnManager.resolveAvatarUrl(profile.avatarUrl) ?: notif.profile.avatarUrl
                            )
                        )
                    } else {
                        notif
                    }
                }
                
                val entities = resolvedNotifications.map { LocalNotificationEntity.fromDomain(it) }
                dao.insertAll(entities)
                
                Result.success(resolvedNotifications)
            } else {
                Log.e(TAG, "Error fetching notifications: ${response.errorBody()?.string()}")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching notifications", e)
            Result.success(emptyList())
        }
    }

    suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            // Update local first
            dao.markAsRead(notificationId)

            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey
            val body = mapOf("is_read" to true)
            service.markNotificationRead(apiKey, bearer, "eq.$notificationId", body)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception marking notification as read", e)
            Result.failure(e)
        }
    }

    suspend fun createNotification(targetUserId: String, type: String, entityId: String) = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        if (targetUserId.isBlank() || targetUserId == currentUid) return@withContext Result.success(Unit)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"
            val body = mapOf(
                "user_id" to targetUserId,
                "actor_id" to currentUid,
                "type" to type,
                "entity_id" to entityId
            )
            service.createNotification(apiKey, bearer, body)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.d(TAG, "Notification insert handled (RLS server-managed): ${e.message}")
            Result.success(Unit)
        }
    }

    suspend fun clearNotification(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            dao.delete(notificationId)

            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey
            service.clearNotification(apiKey, bearer, "eq.$notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting notification", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        try {
            dao.deleteAll()
            
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey
            service.clearAllNotifications(apiKey, bearer, "eq.$currentUid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception clearing all notifications", e)
            Result.failure(e)
        }
    }

    fun addLocalNotification(notification: Notification) {
        repoScope.launch {
            dao.insert(LocalNotificationEntity.fromDomain(notification))
        }
    }
}
