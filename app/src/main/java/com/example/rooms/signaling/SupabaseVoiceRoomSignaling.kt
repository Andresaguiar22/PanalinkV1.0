package com.example.rooms.signaling

import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Implementacion de [VoiceRoomSignaling] sobre Supabase Realtime con un WebSocket
 * PROPIO y aislado del socket global de SupabaseClient.
 *
 * Aislamiento deliberado: el dispatcher de mensajes del chat privado trata cualquier
 * tabla desconocida como mensaje de chat (fallback else en SupabaseClient.onMessage),
 * asi que los canales de salas NUNCA se joinean en el socket compartido.
 *
 * Canales por sala:
 *  - realtime:public:voice_room_seats    (postgres_changes, filtro room_id)
 *  - realtime:public:voice_room_members  (postgres_changes, filtro room_id)
 *  - realtime:public:voice_room_messages (postgres_changes, filtro room_id)
 *  - voice_room:{roomId}                 (broadcast efimero para SDP/ICE WebRTC)
 */
class SupabaseVoiceRoomSignaling(
    private val myUserId: String
) : VoiceRoomSignaling {

    companion object { private const val TAG = "VoiceRoomSignaling" }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WS longevo: sin read timeout
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var currentRoomId: String? = null
    private var intentionallyClosed = false
    private var refCounter = 0

    private val _tableEvents = MutableSharedFlow<VoiceRoomSignaling.TableEvent>(extraBufferCapacity = 128)
    override val tableEvents: SharedFlow<VoiceRoomSignaling.TableEvent> = _tableEvents

    private val _signalEvents = MutableSharedFlow<VoiceRoomSignaling.SignalEvent>(extraBufferCapacity = 128)
    override val signalEvents: SharedFlow<VoiceRoomSignaling.SignalEvent> = _signalEvents

    private val _connectionState = MutableStateFlow(false)
    override val connectionState: StateFlow<Boolean> = _connectionState

    override suspend fun joinRoom(roomId: String) {
        intentionallyClosed = false
        currentRoomId = roomId
        connect()
    }

    override suspend fun leaveRoom() {
        intentionallyClosed = true
        currentRoomId = null
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        try { webSocket?.close(1000, "leave_room") } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = false
    }

    private fun connect() {
        val roomId = currentRoomId ?: return
        val token = SupabaseClient.currentToken
        var wsUrl = SupabaseClient.supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .removeSuffix("/") + "/realtime/v1/websocket?apikey=${SupabaseClient.supabaseAnonKey}&vsn=1.0.0"
        if (!token.isNullOrEmpty()) wsUrl += "&token=$token"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "🟢 WS de sala abierto")
                _connectionState.value = true
                joinChannels(ws, roomId, token)
                startHeartbeat(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleFrame(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "🔴 WS de sala fallo: ${t.message}")
                _connectionState.value = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                if (!intentionallyClosed) scheduleReconnect()
            }
        })
    }

    private fun joinChannels(ws: WebSocket, roomId: String, token: String?) {
        listOf("voice_room_seats", "voice_room_members", "voice_room_messages").forEach { table ->
            ws.send(buildPgChangeJoin(table, roomId, token).toString())
        }
        ws.send(buildBroadcastJoin(roomId, token).toString())
    }

    private fun buildPgChangeJoin(table: String, roomId: String, token: String?): JSONObject =
        JSONObject().apply {
            put("topic", "realtime:public:$table")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", table)
                            put("filter", "room_id=eq.$roomId")
                        })
                    })
                })
                if (!token.isNullOrEmpty()) {
                    put("user_token", token)
                    put("access_token", token)
                }
            })
            put("ref", "vr_${refCounter++}")
        }

    private fun buildBroadcastJoin(roomId: String, token: String?): JSONObject =
        JSONObject().apply {
            put("topic", "voice_room:$roomId")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("broadcast", JSONObject().apply {
                        put("ack", false)
                        put("self", false) // mi propia senal no me interesa
                    })
                })
                if (!token.isNullOrEmpty()) {
                    put("user_token", token)
                    put("access_token", token)
                }
            })
            put("ref", "vr_${refCounter++}")
        }

    private fun handleFrame(text: String) {
        try {
            val obj = JSONObject(text)
            val event = obj.optString("event")
            val topic = obj.optString("topic", "")

            if (event == "postgres_changes") {
                val payload = obj.optJSONObject("payload") ?: return
                val data = payload.optJSONObject("data") ?: return
                val table = data.optString("table")
                if (!table.startsWith("voice_room")) return
                val eventType = data.optString("type")
                val record = when (eventType) {
                    "DELETE" -> data.optJSONObject("old_record")
                    else -> data.optJSONObject("record")
                } ?: return
                scope.launch {
                    _tableEvents.emit(VoiceRoomSignaling.TableEvent(table, eventType, record))
                }
            } else if (event == "broadcast" && topic.startsWith("voice_room:")) {
                val payload = obj.optJSONObject("payload") ?: return
                val eventName = payload.optString("event")
                val body = payload.optJSONObject("payload") ?: return
                val from = body.optString("from")
                val to = body.optString("to")
                if (to.isNotEmpty() && to != myUserId) return // senal dirigida a otro
                if (from == myUserId) return
                if (eventName == "offer" || eventName == "answer" || eventName == "ice") {
                    scope.launch {
                        _signalEvents.emit(VoiceRoomSignaling.SignalEvent(eventName, from, to, body))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando frame de sala", e)
        }
    }

    private fun sendBroadcast(eventName: String, body: JSONObject) {
        val roomId = currentRoomId ?: return
        val msg = JSONObject().apply {
            put("topic", "voice_room:$roomId")
            put("event", "broadcast")
            put("payload", JSONObject().apply {
                put("type", "broadcast")
                put("event", eventName)
                put("payload", body)
            })
            put("ref", "vr_${refCounter++}")
        }
        webSocket?.send(msg.toString())
    }

    override suspend fun sendOffer(roomId: String, toUserId: String, sdp: String) =
        sendBroadcast("offer", JSONObject().apply {
            put("from", myUserId); put("to", toUserId); put("sdp", sdp)
        })

    override suspend fun sendAnswer(roomId: String, toUserId: String, sdp: String) =
        sendBroadcast("answer", JSONObject().apply {
            put("from", myUserId); put("to", toUserId); put("sdp", sdp)
        })

    override suspend fun sendIceCandidate(roomId: String, toUserId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) =
        sendBroadcast("ice", JSONObject().apply {
            put("from", myUserId); put("to", toUserId)
            put("sdpMid", sdpMid); put("sdpMLineIndex", sdpMLineIndex); put("candidate", candidate)
        })

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30000)
                try {
                    ws.send(JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", "vr_hb_${System.currentTimeMillis()}")
                    }.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat fallo: ${e.message}")
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (intentionallyClosed) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(3000)
            if (!intentionallyClosed && currentRoomId != null) {
                Log.d(TAG, "Reconectando WS de sala...")
                connect()
            }
        }
    }
}
