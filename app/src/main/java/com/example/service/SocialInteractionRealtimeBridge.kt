package com.example.service

import android.util.Log
import com.example.data.repository.StatesRepository
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseRealtimeRecordResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SocialInteractionRealtimeBridge {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var started = false

    fun start() {
        if (started) return
        started = true
        reconnectAttempt = 0
        connect()
    }

    fun stop() {
        started = false
        reconnectJob?.cancel()
        reconnectJob = null
        socket?.close(1000, "bridge stopped")
        socket = null
    }

    fun close() {
        stop()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun connect() {
        if (!started || !SupabaseClient.isConfigured) return
        socket?.close(1000, "reconnect")
        socket = null

        val base = SupabaseClient.supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .removeSuffix("/")
        val token = SupabaseClient.currentToken
        var url = "$base/realtime/v1/websocket?apikey=${SupabaseClient.supabaseAnonKey}&vsn=1.0.0"
        if (!token.isNullOrBlank()) url += "&token=$token"

        socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    joinSocialInteractions(webSocket, token)
                    Log.i(TAG, "Social interaction realtime bridge connected")
                }

                override fun onMessage(webSocket: WebSocket, text: String) = processFrame(text)

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Social realtime bridge failure: ${t.message}")
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (started && code != 1000) scheduleReconnect()
                }
            }
        )
    }

    private fun joinSocialInteractions(webSocket: WebSocket, token: String?) {
        val changes = JSONArray()
        listOf("reel_favorites", "reel_shares", "story_favorites", "story_shares").forEach { table ->
            changes.put(JSONObject().apply {
                put("event", "*")
                put("schema", "social")
                put("table", table)
            })
        }

        val payload = JSONObject().apply {
            put("config", JSONObject().apply { put("postgres_changes", changes) })
            if (!token.isNullOrBlank()) {
                put("access_token", token)
                put("user_token", token)
            }
        }
        webSocket.send(JSONObject().apply {
            put("topic", "realtime:social:interaction_bridge")
            put("event", "phx_join")
            put("payload", payload)
            put("ref", "social_${System.currentTimeMillis()}")
        }.toString())
    }

    private fun processFrame(text: String) {
        try {
            val root = JSONObject(text)
            if (root.optString("event") != "postgres_changes") return
            val payload = root.optJSONObject("payload") ?: return
            val data = payload.optJSONObject("data") ?: return
            val table = data.optString("table")
            if (table !in setOf("reel_favorites", "reel_shares", "story_favorites", "story_shares")) return

            val eventType = data.optString("type")
            val isReel = table.startsWith("reel_")
            val resolved = SupabaseRealtimeRecordResolver.resolve(payload, data, eventType, isReel) ?: run {
                Log.w(TAG, "Ignoring $table event without a resolvable target")
                return
            }
            val interactionType = if (table.endsWith("favorites")) "FAVORITE" else "SHARE"
            val update = SupabaseClient.SocialInteractionUpdate(
                statusId = resolved.statusId,
                isReel = isReel,
                eventType = eventType,
                recordId = resolved.recordId,
                record = resolved.record
            )
            scope.launch {
                try {
                    StatesRepository().handleRealtimeSocialInteraction(update, interactionType)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to route $interactionType realtime event", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid social realtime frame", e)
        }
    }

    private fun scheduleReconnect() {
        if (!started) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = when (reconnectAttempt) {
                0 -> 2000L
                1 -> 5000L
                2 -> 10000L
                else -> 30000L
            }
            delay(delayMs)
            reconnectAttempt++
            if (isActive && started) connect()
        }
    }

    companion object { private const val TAG = "SocialRealtimeBridge" }
}
