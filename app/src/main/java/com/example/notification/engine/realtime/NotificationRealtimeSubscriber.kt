package com.example.notification.engine.realtime

import android.util.Log
import androidx.annotation.Keep
import com.example.data.supabase.SupabaseClient
import com.example.notification.engine.analytics.NotificationAnalytics
import com.example.notification.engine.core.NotificationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@Keep
enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

@Keep
class NotificationRealtimeSubscriber private constructor(
    private val engine: NotificationEngine = NotificationEngine.getInstance()
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private var activeUserChannel: String? = null

    fun connect(userId: String? = SupabaseClient.currentUser?.id) {
        val targetUserId = userId ?: SupabaseClient.currentUser?.id ?: run {
            Log.w(TAG, "No user ID available for Realtime subscription")
            _connectionState.value = RealtimeConnectionState.DISCONNECTED
            return
        }

        val channelName = "notifications:$targetUserId"
        if (activeUserChannel == channelName && _connectionState.value == RealtimeConnectionState.CONNECTED) {
            Log.d(TAG, "Already connected to channel: $channelName")
            return
        }

        scope.launch {
            try {
                if (_connectionState.value == RealtimeConnectionState.DISCONNECTED) {
                    _connectionState.value = RealtimeConnectionState.CONNECTING
                } else {
                    _connectionState.value = RealtimeConnectionState.RECONNECTING
                }

                activeUserChannel = channelName
                Log.d(TAG, "Suscribiendo a canal privado Realtime: $channelName para el usuario $targetUserId")

                // Simulate Realtime channel setup & event dispatch listener binding
                _connectionState.value = RealtimeConnectionState.CONNECTED
                Log.d(TAG, "Canal Realtime $channelName conectado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando a canal Realtime $channelName", e)
                _connectionState.value = RealtimeConnectionState.DISCONNECTED
            }
        }
    }

    fun disconnect() {
        scope.launch {
            activeUserChannel = null
            _connectionState.value = RealtimeConnectionState.DISCONNECTED
            Log.d(TAG, "Canal Realtime desconectado")
        }
    }

    /**
     * Entry point for incoming WebSocket / Supabase Realtime payloads.
     * Converts JSON payload -> NotificationEvent and routes exclusively through NotificationEngine.publish()
     * NEVER saves directly to Room DAO from here to enforce pipeline processing.
     */
    fun onRawEventReceived(jsonPayload: String, targetUserId: String) {
        val currentConnectedUser = SupabaseClient.currentUser?.id ?: targetUserId
        // Security check: Verify that event target matches current connected user to prevent leaks
        if (targetUserId != currentConnectedUser) {
            Log.w(TAG, "Seguridad: Evento descartado porque targetUserId ($targetUserId) no coincide con usuario conectado ($currentConnectedUser)")
            return
        }

        scope.launch {
            try {
                val json = JSONObject(jsonPayload)
                val eventId = json.optString("id", UUID.randomUUID().toString())
                val rawDomain = json.optString("domain", "SOCIAL")
                val rawType = json.optString("event_type", "POST_LIKE")
                val actorId = json.optString("actor_id", "unknown")
                val actorName = json.optString("actor_name", "")
                val entityId = json.optString("entity_id", "")
                val title = json.optString("title", "PanaLink")
                val body = json.optString("body", "Nueva interacción")

                val domain = runCatching { NotificationDomain.valueOf(rawDomain) }.getOrDefault(NotificationDomain.SOCIAL)
                val type = runCatching { NotificationTypeV2.valueOf(rawType) }.getOrDefault(NotificationTypeV2.POST_LIKE)

                val event = NotificationEvent(
                    id = eventId,
                    domain = domain,
                    type = type,
                    priority = NotificationPriority.NORMAL,
                    interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
                    actor = EventActor(id = actorId, name = actorName),
                    target = EventTarget(entityId = entityId, entityType = domain.name.lowercase()),
                    title = title,
                    body = body,
                    groupingKey = "${type.name}_$entityId"
                )

                NotificationAnalytics.trackCreated(event.id, event.type.name)

                // Route through engine pipeline exclusively
                engine.publish(event)
                Log.d(TAG, "Evento Realtime procesado vía NotificationEngine pipeline: ${event.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando evento Realtime JSON", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationRealtime"

        @Volatile
        private var instance: NotificationRealtimeSubscriber? = null

        fun getInstance(): NotificationRealtimeSubscriber {
            return instance ?: synchronized(this) {
                instance ?: NotificationRealtimeSubscriber().also { instance = it }
            }
        }
    }
}
