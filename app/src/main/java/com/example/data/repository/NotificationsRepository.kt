package com.example.data.repository

import android.util.Log
import com.example.data.model.Notification
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NotificationsRepository {
    private val TAG = "NotificationsRepo"
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: Flow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: Flow<Int> = _unreadCount.asStateFlow()

    suspend fun fetchNotifications() = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured) {
                val demoList = listOf(
                    Notification(
                        id = "demo_notif_1",
                        type = com.example.data.model.NotificationType.LIKE,
                        sourceId = "state_1",
                        profile = com.example.data.model.Profile("demo_1", "Pana Amigo", null),
                        timestamp = "2026-08-05T20:00:00.000000",
                        isRead = false,
                        actionText = "le gustó tu estado.",
                        previewText = "¡Qué buen estado, pana!"
                    ),
                    Notification(
                        id = "demo_notif_2",
                        type = com.example.data.model.NotificationType.MESSAGE,
                        sourceId = "chat_1",
                        profile = com.example.data.model.Profile("demo_2", "María Pana", null),
                        timestamp = "2026-08-05T19:30:00.000000",
                        isRead = false,
                        actionText = "te envió un mensaje.",
                        previewText = "¿Cómo estás hoy?"
                    )
                )
                _notifications.value = demoList
                _unreadCount.value = demoList.count { !it.isRead }
                return@withContext Result.success(demoList)
            }

            SessionManager.validateAndRefreshSessionIfNeeded()
            val service = SupabaseClient.apiService
            val token = SupabaseClient.currentToken

            if (service == null || token == null) {
                val currentLocal = _notifications.value
                return@withContext Result.success(currentLocal)
            }

            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey
            val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.success(_notifications.value)

            val response = service.getNotifications(apiKey, bearer, "eq.$currentUid", select = "*")
            
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val parsed = dtos.map { it.toDomain() }
                _notifications.value = parsed
                _unreadCount.value = parsed.count { !it.isRead }
                Result.success(parsed)
            } else {
                Log.e(TAG, "Error fetching notifications: ${response.errorBody()?.string()}")
                Result.success(_notifications.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching notifications", e)
            Result.success(_notifications.value)
        }
    }

    suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey

            val body = mapOf("is_read" to true)
            val response = service.markNotificationRead(apiKey, bearer, "eq.$notificationId", body)
            
            if (response.isSuccessful) {
                // Update local state immediately
                val current = _notifications.value.toMutableList()
                val index = current.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    val notif = current[index]
                    if (!notif.isRead) {
                        current[index] = notif.copy(isRead = true)
                        _notifications.value = current
                        _unreadCount.value = current.count { !it.isRead }
                    }
                }
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error marking notification read: ${response.errorBody()?.string()}")
                Result.failure(Exception("Failed to mark as read"))
            }
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
            val response = service.createNotification(apiKey, bearer, body)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // RLS policy: notifications insert is allowed for service_role only.
                // Database triggers automatically create notifications on server side.
                Log.d(TAG, "Notification insert response: ${response.code()} (RLS server-managed)")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Notification insert handled (RLS server-managed): ${e.message}")
            Result.success(Unit)
        }
    }

    suspend fun clearNotification(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey

            val response = service.clearNotification(apiKey, bearer, "eq.$notificationId")
            
            if (response.isSuccessful) {
                // Update local state immediately
                val current = _notifications.value.toMutableList()
                val index = current.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    current.removeAt(index)
                    _notifications.value = current
                    _unreadCount.value = current.count { !it.isRead }
                }
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error deleting notification: ${response.errorBody()?.string()}")
                Result.failure(Exception("Failed to delete notification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting notification", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val bearer = "Bearer $token"
            val apiKey = SupabaseClient.supabaseAnonKey

            val response = service.clearAllNotifications(apiKey, bearer, "eq.$currentUid")
            
            if (response.isSuccessful) {
                _notifications.value = emptyList()
                _unreadCount.value = 0
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error clearing all notifications: ${response.errorBody()?.string()}")
                Result.failure(Exception("Failed to clear notifications"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception clearing all notifications", e)
            Result.failure(e)
        }
    }

    fun addLocalNotification(notification: Notification) {
        val current = _notifications.value.toMutableList()
        current.add(0, notification)
        _notifications.value = current
        _unreadCount.value = current.count { !it.isRead }
    }
}
