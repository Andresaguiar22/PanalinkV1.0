package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.ws.RealWebSocket
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    // Hardcoded keys to ensure absolute stability as requested
    val supabaseUrl: String = "https://tivqjfgjdxgzicrridaz.supabase.co"
    val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"

    // Forced to true to completely disable demonstration mode as requested
    val isConfigured: Boolean = true

    val moshi: Moshi = Moshi.Builder()
        .add(com.example.data.model.EmbeddedProfileAdapter())
        .add(com.example.data.model.ProfileSurrogateAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS) // Keep WebSockets alive on H+/3G
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("apikey", supabaseAnonKey)
                    .header("User-Agent", "Panalink/2.0 (Android; ${android.os.Build.MODEL}; ${android.os.Build.VERSION.RELEASE}) Shield/1.0")
                    .header("X-Client-Shield", "v1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val apiService: SupabaseApiService? by lazy {
        if (isConfigured) {
            val url = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
            Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SupabaseApiService::class.java)
        } else {
            null
        }
    }

    // Current Session State
    var currentToken: String? = null
    var currentRefreshToken: String? = null
    var currentUser: AuthUser? = null
    var currentProfile: Profile? = null
        set(value) {
            field = value
            currentProfileState.value = value
        }
    val currentProfileState = kotlinx.coroutines.flow.MutableStateFlow<com.example.data.model.Profile?>(null)
    
    var activeChatId: String? = null
    var isChatScreenActive: Boolean = false

    // Realtime Flow
    data class TypingStatus(val chatId: String, val userId: String, val isTyping: Boolean)
    data class UserPresence(val userId: String, val status: String, val lastSeen: Long)
    data class ReactionBroadcast(val messageId: String, val chatId: String, val userId: String, val emoji: String)

    private val _realtimeMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val realtimeMessages: SharedFlow<Message> = _realtimeMessages

    private val _realtimeMessageDeletions = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val realtimeMessageDeletions: SharedFlow<String> = _realtimeMessageDeletions

    private val _realtimeStatuses = MutableSharedFlow<UserState>(extraBufferCapacity = 64)
    val realtimeStatuses: SharedFlow<UserState> = _realtimeStatuses

    private val _realtimeTyping = MutableSharedFlow<TypingStatus>(extraBufferCapacity = 64)
    val realtimeTyping: SharedFlow<TypingStatus> = _realtimeTyping

    private val _realtimePresence = MutableSharedFlow<UserPresence>(extraBufferCapacity = 64)
    val realtimePresence: SharedFlow<UserPresence> = _realtimePresence

    // Realtime Flows
    // ... (existing flows)
    val realtimePresenceState = kotlinx.coroutines.flow.MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    
    val isConnected: Boolean get() = webSocket != null && isConfigured
    
    // Add this:
    val globalServerConfigUpdates = MutableSharedFlow<String>(replay = 1)

    private val _realtimeReactions = MutableSharedFlow<ReactionBroadcast>(extraBufferCapacity = 64)
    val realtimeReactions: SharedFlow<ReactionBroadcast> = _realtimeReactions
    
    data class SocialInteractionUpdate(val statusId: String, val isReel: Boolean, val eventType: String, val recordId: String, val record: org.json.JSONObject)
    
    private val _realtimeLikes = MutableSharedFlow<SocialInteractionUpdate>(extraBufferCapacity = 64)
    val realtimeLikes: SharedFlow<SocialInteractionUpdate> = _realtimeLikes
    
    private val _realtimeComments = MutableSharedFlow<SocialInteractionUpdate>(extraBufferCapacity = 64)
    val realtimeComments: SharedFlow<SocialInteractionUpdate> = _realtimeComments

    private val _realtimeNotifications = MutableSharedFlow<com.example.data.model.NotificationDto>(extraBufferCapacity = 64)
    val realtimeNotifications: SharedFlow<com.example.data.model.NotificationDto> = _realtimeNotifications

    // Music Social Flow
    data class MusicUpdate(val table: String, val eventType: String, val record: JSONObject)
    private val _realtimeMusicUpdates = MutableSharedFlow<MusicUpdate>(extraBufferCapacity = 64)
    val realtimeMusicUpdates: SharedFlow<MusicUpdate> = _realtimeMusicUpdates

    fun emitRealtimeTyping(status: TypingStatus) {
        clientScope.launch {
            _realtimeTyping.emit(status)
        }
    }

    fun emitRealtimePresence(presence: UserPresence) {
        clientScope.launch {
            val current = realtimePresenceState.value.toMutableMap()
            current[presence.userId] = presence
            realtimePresenceState.value = current
            _realtimePresence.emit(presence)
        }
    }

    fun emitRealtimeReaction(reaction: ReactionBroadcast) {
        clientScope.launch {
            _realtimeReactions.emit(reaction)
        }
    }

    fun emitRealtimeMessage(message: Message) {
        clientScope.launch {
            _realtimeMessages.emit(message)
        }
    }

    fun emitRealtimeStatus(status: UserState) {
        clientScope.launch {
            _realtimeStatuses.emit(status)
        }
    }

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- DEMO MODE IN-MEMORY STORAGE ---
    // If Supabase is not configured, we keep state in-memory to provide a highly functional WhatsApp clone experience
    val demoProfiles = mutableMapOf<String, Profile>()
    val demoChats = mutableMapOf<String, Chat>()
    val demoChatMembers = mutableListOf<ChatMember>()
    val demoMessages = mutableListOf<Message>()
    val demoUserStates = mutableListOf<UserState>()
    var demoUserEmail: String? = null
    var demoUserVerified: Boolean = false

    init {
        setupDemoData()
    }

    private fun setupDemoData() {
        Log.d(TAG, "Initializing Supabase Client. Configured: $isConfigured")
        
        // Seed some demo venezuelan panis
        val p1 = Profile("user_yonaiker", "Yonaiker 🇻🇪", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80", "2026-06-25T12:00:00Z")
        val p2 = Profile("user_gabriel", "Gabriel (Pana) 🌴", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80", "2026-06-25T12:00:00Z")
        val p3 = Profile("user_maria", "Maria Corina 💃", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80", "2026-06-25T12:00:00Z")
        
        demoProfiles[p1.id] = p1
        demoProfiles[p2.id] = p2
        demoProfiles[p3.id] = p3

        // Seed some chats
        val c1 = Chat("chat_1", "2026-06-25T12:00:00Z", "dm")
        val c2 = Chat("chat_2", "2026-06-25T12:00:00Z", "dm")
        demoChats[c1.id] = c1
        demoChats[c2.id] = c2

        demoChatMembers.add(ChatMember("chat_1", "user_yonaiker"))
        demoChatMembers.add(ChatMember("chat_1", "me_demo_id"))
        
        demoChatMembers.add(ChatMember("chat_2", "user_gabriel"))
        demoChatMembers.add(ChatMember("chat_2", "me_demo_id"))

        // Seed some messages
        val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val ago5m = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(System.currentTimeMillis() - 5 * 60 * 1000))
        val ago10m = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(System.currentTimeMillis() - 10 * 60 * 1000))

        demoMessages.add(Message("msg_1", "chat_1", "user_yonaiker", "Épa chamo! ¿Cómo va todo por allá?", ago10m, "seen"))
        demoMessages.add(Message("msg_2", "chat_1", "me_demo_id", "Todo fino mi pana, ¿y tú? ¿Qué cuenta la guaira?", ago5m, "seen"))
        demoMessages.add(Message("msg_3", "chat_1", "user_yonaiker", "Puro calor mano! Avísame si vas a bajar hoy para armar un compartir con unas frías 🍻", nowStr, "delivered"))

        demoMessages.add(Message("msg_4", "chat_2", "user_gabriel", "Hola chamo, tienes el contacto del técnico del aire?", ago10m, "seen"))

        // Seed some Whatsapp States (Expiring in 24h)
        val expiresAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(System.currentTimeMillis() + 24 * 3600 * 1000))
        demoUserStates.add(UserState(id = "st_1", authorId = "user_gabriel", mediaType = "text", caption = "Activo en Choroní, qué paraíso mental! 🏖️🌊", expiresAt = expiresAt, createdAt = ago10m))
        demoUserStates.add(UserState(id = "st_2", authorId = "user_maria", mediaUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80", mediaType = "image", caption = "Arepitas de reina pepiada para el desayuno 🇻🇪🤤", expiresAt = expiresAt, createdAt = ago5m))
    }

    // --- Realtime WebSocket Connection ---
    fun connectRealtime() {
        if (!isConfigured) return
        disconnectRealtime(resetAttempts = false)

        val token = currentToken
        var wsUrl = supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .removeSuffix("/") + "/realtime/v1/websocket?apikey=$supabaseAnonKey&vsn=1.0.0"

        if (!token.isNullOrEmpty()) {
            wsUrl += "&token=$token"
        }

        val logUrl = com.example.util.LogSanitizer.sanitize(wsUrl)
        Log.d(TAG, "Connecting Realtime WebSocket to: $logUrl")
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "🟢 WebSocket Realtime Abierto Exitosamente!")
                reconnectAttempt = 0
                reconnectJob?.cancel()
                reconnectJob = null
                
                val currentTokenLocal = currentToken
                
                // Join messages channel
                val joinMessages = JSONObject().apply {
                    put("topic", "realtime:public:messages")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply {
                                put("ack", false)
                                put("self", true)
                            })
                            put("presence", JSONObject().apply {
                                put("key", "")
                            })
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "messages")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "1")
                }
                webSocket.send(joinMessages.toString())

                // Join thread_messages channel
                val joinThreadMessages = JSONObject().apply {
                    put("topic", "realtime:public:thread_messages")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "thread_messages")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "2")
                }
                webSocket.send(joinThreadMessages.toString())

                // Join channel_messages channel
                val joinChannelMessages = JSONObject().apply {
                    put("topic", "realtime:public:channel_messages")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "channel_messages")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "chan_1")
                }
                webSocket.send(joinChannelMessages.toString())

                // Join user_reels channel (social schema)
                val joinUserReels = JSONObject().apply {
                    put("topic", "realtime:social:user_reels")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "social")
                                    put("table", "user_reels")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "3")
                }
                webSocket.send(joinUserReels.toString())
                
                // Join user_stories channel (social schema)
                val joinUserStories = JSONObject().apply {
                    put("topic", "realtime:social:user_stories")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "social")
                                    put("table", "user_stories")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "10")
                }
                webSocket.send(joinUserStories.toString())

                // Join presence channel
                val joinPresence = JSONObject().apply {
                    put("topic", "realtime:public")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("presence", JSONObject().apply {
                                put("key", currentUser?.id ?: "unknown")
                            })
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "presence_1")
                }
                webSocket.send(joinPresence.toString())
                
                // Track our presence
                trackPresence()
                
                // Join interaction channels for reels
                val joinReelInteractions = JSONObject().apply {
                    put("topic", "realtime:social:reel_interactions")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply { put("event", "*"); put("schema", "social"); put("table", "reel_likes") })
                                put(JSONObject().apply { put("event", "*"); put("schema", "social"); put("table", "reel_comments") })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "5")
                }
                webSocket.send(joinReelInteractions.toString())
                
                // Join interaction channels for stories
                val joinStoryInteractions = JSONObject().apply {
                    put("topic", "realtime:social:story_interactions")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply { put("event", "*"); put("schema", "social"); put("table", "story_likes") })
                                put(JSONObject().apply { put("event", "*"); put("schema", "social"); put("table", "story_comments") })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "6")
                }
                webSocket.send(joinStoryInteractions.toString())

                // Join global_server_config channel
                val joinGlobalServerConfig = JSONObject().apply {
                    put("topic", "realtime:public:global_server_config")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "global_server_config")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "4")
                }
                webSocket.send(joinGlobalServerConfig.toString())

                // Join notifications channel (public schema)
                val joinNotifications = JSONObject().apply {
                    put("topic", "realtime:public:notifications")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "notifications")
                                })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "notif_1")
                }
                webSocket.send(joinNotifications.toString())

                // Join music_social channel
                val joinMusicSocial = JSONObject().apply {
                    put("topic", "realtime:public:music_social")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val pgChanges = org.json.JSONArray().apply {
                                put(JSONObject().apply { put("event", "*"); put("schema", "public"); put("table", "music_playlists") })
                                put(JSONObject().apply { put("event", "*"); put("schema", "public"); put("table", "music_playlist_tracks") })
                                put(JSONObject().apply { put("event", "*"); put("schema", "public"); put("table", "music_playlist_collaborators") })
                                put(JSONObject().apply { put("event", "*"); put("schema", "public"); put("table", "music_playlist_shares") })
                            }
                            put("postgres_changes", pgChanges)
                        })
                        if (!currentTokenLocal.isNullOrEmpty()) {
                            put("user_token", currentTokenLocal)
                            put("access_token", currentTokenLocal)
                        }
                    })
                    put("ref", "music_1")
                }
                webSocket.send(joinMusicSocial.toString())

                // Start heartbeat
                heartbeatJob = clientScope.launch {
                    while (isActive) {
                        delay(30000)
                        val heartbeat = JSONObject().apply {
                            put("topic", "phoenix")
                            put("event", "heartbeat")
                            put("payload", JSONObject())
                            put("ref", "heartbeat_${System.currentTimeMillis()}")
                        }
                        webSocket.send(heartbeat.toString())
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (BuildConfig.DEBUG) {
                    val sanitizedText = com.example.util.LogSanitizer.sanitize(text)
                    Log.d(TAG, "WebSocket Frame: ${sanitizedText.take(200)}")
                }
                try {
                    val obj = JSONObject(text)
                    val event = obj.optString("event")
                    val topic = obj.optString("topic", "")
                    
                    var finalEvent = event
                    var finalPayload = obj.optJSONObject("payload")
                    if (event == "broadcast" && finalPayload != null) {
                        finalEvent = finalPayload.optString("event")
                        finalPayload = finalPayload.optJSONObject("payload") ?: finalPayload.optJSONObject("data")
                    }
                    
                    if (finalEvent == "typing") {
                        val chatId = finalPayload?.optString("chat_id") ?: ""
                        val userId = finalPayload?.optString("user_id") ?: ""
                        val isTyping = finalPayload?.optBoolean("is_typing") ?: false
                        clientScope.launch {
                            _realtimeTyping.emit(TypingStatus(chatId, userId, isTyping))
                        }
                    } else if (finalEvent == "presence" || event == "presence") {
                        val userId = finalPayload?.optString("user_id") ?: ""
                        val status = finalPayload?.optString("status") ?: "offline"
                        val lastSeen = finalPayload?.optLong("last_seen") ?: System.currentTimeMillis()
                        emitRealtimePresence(UserPresence(userId, status, lastSeen))
                    } else if (event == "presence_state") {
                        val payload = obj.optJSONObject("payload") ?: JSONObject()
                        payload.keys().forEach { userId ->
                            val userObj = payload.optJSONObject(userId)
                            val metasArray = userObj?.optJSONArray("metas")
                            val status = metasArray?.optJSONObject(0)?.optString("status") ?: "online"
                            val lastSeen = metasArray?.optJSONObject(0)?.optLong("last_seen") ?: System.currentTimeMillis()
                            emitRealtimePresence(UserPresence(userId, status, lastSeen))
                        }
                    } else if (event == "presence_diff") {
                        val payload = obj.optJSONObject("payload") ?: JSONObject()
                        val joins = payload.optJSONObject("joins") ?: JSONObject()
                        joins.keys().forEach { userId ->
                            val userObj = joins.optJSONObject(userId)
                            val metasArray = userObj?.optJSONArray("metas")
                            val status = metasArray?.optJSONObject(0)?.optString("status") ?: "online"
                            val lastSeen = metasArray?.optJSONObject(0)?.optLong("last_seen") ?: System.currentTimeMillis()
                            emitRealtimePresence(UserPresence(userId, status, lastSeen))
                        }
                        val leaves = payload.optJSONObject("leaves") ?: JSONObject()
                        leaves.keys().forEach { userId ->
                            emitRealtimePresence(UserPresence(userId, "offline", System.currentTimeMillis()))
                        }
                    } else if (finalEvent == "reaction") {
                        val messageId = finalPayload?.optString("message_id") ?: ""
                        val chatId = finalPayload?.optString("chat_id") ?: ""
                        val userId = finalPayload?.optString("user_id") ?: ""
                        val emoji = finalPayload?.optString("emoji") ?: ""
                        clientScope.launch {
                            _realtimeReactions.emit(ReactionBroadcast(messageId, chatId, userId, emoji))
                        }
                    } else if (event == "postgres_changes" || topic.startsWith("realtime:")) {
                        val payload = obj.optJSONObject("payload")
                        val dataObj = payload?.optJSONObject("data")
                        val table = dataObj?.optString("table") ?: payload?.optString("table") ?: topic.substringAfterLast(":")
                        val eventType = dataObj?.optString("type") ?: payload?.optString("type") ?: payload?.optString("event") ?: ""
                        Log.d(TAG, "onMessage realtime postgres_changes table=$table, eventType=$eventType")
                        
                        // Extract record in any of the possible structures: data.record, data, or payload.record
                        val record = dataObj?.optJSONObject("record") 
                            ?: dataObj 
                            ?: payload?.optJSONObject("record")
                            ?: dataObj?.optJSONObject("old_record")
                            ?: payload?.optJSONObject("old_record")
                            
                        if (record != null) {
                            if (eventType == "DELETE") {
                                val deletedId = record.optString("id", "")
                                if (deletedId.isNotEmpty()) {
                                    if (table == "messages") {
                                        Log.d(TAG, "Realtime DELETE event received for message ID: $deletedId in table $table")
                                        clientScope.launch {
                                            _realtimeMessageDeletions.emit(deletedId)
                                        }
                                    } else if (table.contains("likes") && (table.contains("reel") || table.contains("story"))) {
                                        val statusId = record.optString("reel_id", record.optString("story_id", record.optString("status_id", "")))
                                        if (statusId.isNotEmpty()) {
                                            clientScope.launch {
                                                _realtimeLikes.emit(SocialInteractionUpdate(statusId, table.contains("reel"), eventType, deletedId, record))
                                            }
                                        }
                                    } else if (table.contains("comments") && (table.contains("reel") || table.contains("story"))) {
                                        val statusId = record.optString("reel_id", record.optString("story_id", record.optString("status_id", "")))
                                        if (statusId.isNotEmpty()) {
                                            clientScope.launch {
                                                _realtimeComments.emit(SocialInteractionUpdate(statusId, table.contains("reel"), eventType, deletedId, record))
                                            }
                                        }
                                    }
                                }
                            } else if (table == "global_server_config" || topic.contains("global_server_config")) {
                                val cdnUrl = record.optString("cdn_url", "")
                                if (cdnUrl.isNotEmpty()) {
                                    clientScope.launch {
                                        globalServerConfigUpdates.emit(cdnUrl)
                                    }
                                }
                            } else if (table == "user_reels" || table == "user_stories" || topic.contains("user_reels") || topic.contains("user_stories")) {
                                val id = record.optString("id", UUID.randomUUID().toString())
                                val userId = record.optString("author_id", record.optString("userId", ""))
                                val mediaUrl = record.optString("media_url", record.optString("mediaUrl", ""))
                                val mediaType = record.optString("media_type", record.optString("mediaType", "video"))
                                val caption = record.optString("caption", record.optString("caption", ""))
                                val expiresAt = record.optString("expires_at", record.optString("expiresAt", ""))
                                val createdAt = record.optString("created_at", record.optString("createdAt", ""))
                                
                                val statusObj = UserState(
                                    id = id,
                                    authorId = userId,
                                    mediaUrl = if (mediaUrl.isNotEmpty()) mediaUrl else null,
                                    mediaType = mediaType,
                                    caption = if (caption.isNotEmpty()) caption else null,
                                    expiresAt = expiresAt,
                                    createdAt = if (createdAt.isNotEmpty()) createdAt else getNowIsoString(),
                                    type = if (table.contains("reels")) "reel" else "story"
                                )
                                Log.d(TAG, "Parsed separated status successfully from $table: $statusObj")
                                emitRealtimeStatus(statusObj)
                            } else if (table.contains("likes") && (table.contains("reel") || table.contains("story"))) {
                                val statusId = record.optString("reel_id", record.optString("story_id", record.optString("status_id", "")))
                                val recordId = record.optString("id", "")
                                if (statusId.isNotEmpty()) {
                                    Log.d(TAG, "Like changed for status: $statusId in $table")
                                    clientScope.launch {
                                        _realtimeLikes.emit(SocialInteractionUpdate(statusId, table.contains("reel"), eventType, recordId, record))
                                    }
                                }
                            } else if (table.contains("comments") && (table.contains("reel") || table.contains("story"))) {
                                val statusId = record.optString("reel_id", record.optString("story_id", record.optString("status_id", "")))
                                val recordId = record.optString("id", "")
                                if (statusId.isNotEmpty()) {
                                    Log.d(TAG, "Comment changed for status: $statusId in $table")
                                    clientScope.launch {
                                        _realtimeComments.emit(SocialInteractionUpdate(statusId, table.contains("reel"), eventType, recordId, record))
                                    }
                                }
                            } else if (table == "notifications" || topic.contains("notifications")) {
                                val notifId = record.optString("id", UUID.randomUUID().toString())
                                val userId = record.optString("user_id", "")
                                val actorId = record.optString("actor_id", "")
                                val type = record.optString("type", "like")
                                val entityId = record.optString("entity_id", "")
                                val isRead = record.optBoolean("is_read", false)
                                val createdAt = record.optString("created_at", getNowIsoString())

                                val currentUid = currentUser?.id ?: ""
                                if (userId == currentUid || userId.isEmpty()) {
                                    val dto = com.example.data.model.NotificationDto(
                                        id = notifId,
                                        userId = userId,
                                        actorId = actorId,
                                        type = type,
                                        entityId = entityId,
                                        isRead = isRead,
                                        createdAt = createdAt,
                                        actorProfile = null
                                    )
                                    clientScope.launch {
                                        Log.d(TAG, "Realtime notification received for user: $dto")
                                        _realtimeNotifications.emit(dto)
                                    }
                                }
                            } else if (table.startsWith("music_")) {
                                clientScope.launch {
                                    _realtimeMusicUpdates.emit(MusicUpdate(table, eventType, record))
                                }
                            } else {
                                val id = record.optString("id", "")
                                val chatId = when {
                                    record.has("chat_id") && !record.isNull("chat_id") -> record.optString("chat_id", "")
                                    record.has("thread_id") && !record.isNull("thread_id") -> record.optString("thread_id", "")
                                    else -> ""
                                }
                                
                                if (chatId.isEmpty()) {
                                    Log.w(TAG, "Received realtime message without thread_id/chat_id: $id")
                                    return@onMessage
                                }
                                val senderId = record.optString("sender_id", "")
                                // Preferir text_content (columna real de thread_messages). Algunos eventos
                                // (broadcasts parciales o filas legacy) traen "content" como null, y elegirlo
                                // por sobre text_content vacía las burbujas.
                                val content = when {
                                    record.has("text_content") && !record.isNull("text_content") -> record.optString("text_content", "")
                                    record.has("content") && !record.isNull("content") -> record.optString("content", "")
                                    else -> ""
                                }
                                val createdAt = record.optString("created_at", getNowIsoString())
                                
                                val deliveredAt = if (record.has("delivered_at") && !record.isNull("delivered_at")) record.optString("delivered_at") else null
                                val seenAt = if (record.has("seen_at") && !record.isNull("seen_at")) record.optString("seen_at") else null
                                val clientMessageUuid = record.optString("client_message_uuid", "")
                                
                                val calculatedStatus = when {
                                    !seenAt.isNullOrEmpty() -> "seen"
                                    !deliveredAt.isNullOrEmpty() -> "delivered"
                                    else -> record.optString("status", "sent")
                                }
                                val replyToMessageId = if (record.has("reply_to") && !record.isNull("reply_to")) record.optString("reply_to") else if (record.has("reply_to_message_id") && !record.isNull("reply_to_message_id")) record.optString("reply_to_message_id") else null
                                val thumbnailUrl = if (record.has("thumbnail_url") && !record.isNull("thumbnail_url")) record.optString("thumbnail_url") else null
                                val mediaUrl = if (record.has("media_url") && !record.isNull("media_url")) record.optString("media_url") else null
                                val mediaMime = if (record.has("media_mime") && !record.isNull("media_mime")) record.optString("media_mime") else null
                                val messageType = if (record.has("message_type") && !record.isNull("message_type")) record.optString("message_type") else null
                                val mediaSize = if (record.has("file_size") && !record.isNull("file_size")) record.optLong("file_size") else null
                                val mediaDuration = if (record.has("duration") && !record.isNull("duration")) record.optLong("duration") else null
                                val mediaWidth = if (record.has("width") && !record.isNull("width")) record.optInt("width") else null
                                val mediaHeight = if (record.has("height") && !record.isNull("height")) record.optInt("height") else null
                                val musicPlaylistId = if (record.has("music_playlist_id") && !record.isNull("music_playlist_id")) record.optString("music_playlist_id") else null

                                val displayContent = if (messageType == "sticker" && !mediaUrl.isNullOrEmpty()) {
                                    "[Sticker] $mediaUrl"
                                } else {
                                    content
                                }

                                if (chatId.isNotEmpty() && id.isNotEmpty()) {
                                    val msg = Message(
                                        id = id,
                                        chatId = chatId,
                                        senderId = senderId,
                                        content = displayContent,
                                        createdAt = createdAt,
                                        status = calculatedStatus,
                                        replyToMessageId = replyToMessageId,
                                        clientMessageUuid = clientMessageUuid,
                                        deliveredAt = deliveredAt,
                                        seenAt = seenAt,
                                        thumbnailUrl = thumbnailUrl,
                                        mediaUrl = mediaUrl,
                                        mediaMime = mediaMime,
                                        mediaSize = mediaSize,
                                        duration = mediaDuration,
                                        width = mediaWidth,
                                        height = mediaHeight,
                                        messageType = messageType ?: "text",
                                        musicPlaylistId = musicPlaylistId
                                    )
                                    
                                    val currentUid = currentUser?.id ?: ""
                                    if (senderId != currentUid) {
                                        // Mark as delivered on remote database
                                        clientScope.launch {
                                            try {
                                                com.example.data.repository.MessagesRepository.getInstance().markThreadDelivered(chatId)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error triggering markThreadDelivered", e)
                                            }
                                        }
                                        if (isChatScreenActive && activeChatId == chatId) {
                                            // Mark as read on remote database
                                            clientScope.launch {
                                                try {
                                                    com.example.data.repository.MessagesRepository.getInstance().markThreadRead(chatId)
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error triggering markThreadRead", e)
                                                }
                                            }
                                        }
                                    }

                                    clientScope.launch {
                                        Log.d("ChatE2ETrace", "1. Mensaje cifrado recibido | id=${msg.id}, chatId=${msg.chatId}, senderId=${msg.senderId}, content=${msg.content}")
                                        _realtimeMessages.emit(msg)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing websocket frame", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "🔴 WebSocket Realtime Failure: ${t.message} | Response: $response", t)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed (code=$code): $reason")
                if (code != 1000) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isConfigured) return
        reconnectJob?.cancel()
        reconnectJob = clientScope.launch {
            val delayMs = when (reconnectAttempt) {
                0 -> 2000L
                1 -> 5000L
                2 -> 10000L
                else -> 30000L
            }
            Log.d(TAG, "Scheduling reconnection in $delayMs ms (attempt $reconnectAttempt)...")
            delay(delayMs)
            reconnectAttempt++
            connectRealtime()
        }
    }

    fun disconnectRealtime(resetAttempts: Boolean = true) {
        Log.d(TAG, "Disconnecting Realtime WebSocket (resetAttempts=$resetAttempts)")
        reconnectJob?.cancel()
        reconnectJob = null
        if (resetAttempts) {
            reconnectAttempt = 0
        }
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // Helper to format ISO Date
    fun getNowIsoString(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private var demoTypingJob: Job? = null
    fun simulateDemoTyping(chatId: String) {
        val otherMemberId = demoChatMembers.firstOrNull { it.chatId == chatId && it.userId != "me_demo_id" }?.userId ?: return
        demoTypingJob?.cancel()
        demoTypingJob = clientScope.launch {
            delay(500)
            _realtimeTyping.emit(TypingStatus(chatId, otherMemberId, true))
            delay(3000)
            _realtimeTyping.emit(TypingStatus(chatId, otherMemberId, false))
        }
    }

    fun sendTypingStatus(chatId: String, isTyping: Boolean) {
        if (com.example.data.repository.PrivacyManager.isPremiumFeatureActive("hide_typing")) {
            return // Intercept and block typing status
        }
        val currentUid = currentUser?.id ?: ""
        if (isConfigured && webSocket != null) {
            val data = JSONObject().apply {
                put("chat_id", chatId)
                put("user_id", currentUid)
                put("is_typing", isTyping)
            }
            val msg = JSONObject().apply {
                put("topic", "realtime:public:messages")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "typing")
                    put("payload", data)
                })
                put("ref", "typing_${System.currentTimeMillis()}")
            }
            webSocket?.send(msg.toString())
        } else {
            if (isTyping) {
                simulateDemoTyping(chatId)
            }
        }
    }

    fun trackPresence() {
        val currentUid = currentUser?.id ?: return
        com.example.data.repository.PresenceRepository.startHeartbeat(currentUid)
        if (isConfigured && webSocket != null) {
            val payload = JSONObject().apply {
                put("topic", "realtime:public")
                put("event", "presence_track")
                put("payload", JSONObject().apply {
                    put("user_id", currentUid)
                    put("status", "online")
                    put("last_seen", System.currentTimeMillis())
                })
                put("ref", "track_${System.currentTimeMillis()}")
            }
            webSocket?.send(payload.toString())
        }
    }

    fun broadcastPresence(status: String) {
        val currentUid = currentUser?.id ?: ""
        if (isConfigured && webSocket != null) {
            val data = JSONObject().apply {
                put("user_id", currentUid)
                put("status", status)
                put("last_seen", System.currentTimeMillis())
            }
            val msg = JSONObject().apply {
                put("topic", "realtime:public:messages")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "presence")
                    put("payload", data)
                })
                put("ref", "presence_${System.currentTimeMillis()}")
            }
            webSocket?.send(msg.toString())
        } else {
            // Emulate self-presence to local flow
            clientScope.launch {
                val presence = UserPresence(currentUid, status, System.currentTimeMillis())
                val current = realtimePresenceState.value.toMutableMap()
                current[currentUid] = presence
                realtimePresenceState.value = current
                _realtimePresence.emit(presence)
            }
        }
    }

    fun broadcastReaction(messageId: String, chatId: String, emoji: String) {
        val currentUid = currentUser?.id ?: ""
        if (isConfigured && webSocket != null) {
            val data = JSONObject().apply {
                put("message_id", messageId)
                put("chat_id", chatId)
                put("user_id", currentUid)
                put("emoji", emoji)
            }
            val msg = JSONObject().apply {
                put("topic", "realtime:public:messages")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "reaction")
                    put("payload", data)
                })
                put("ref", "react_${System.currentTimeMillis()}")
            }
            webSocket?.send(msg.toString())
        } else {
            clientScope.launch {
                _realtimeReactions.emit(ReactionBroadcast(messageId, chatId, currentUid, emoji))
            }
        }
    }

    fun parseSupabaseError(errorStr: String?, defaultMessage: String): String {
        if (errorStr.isNullOrBlank()) return defaultMessage
        return try {
            val json = JSONObject(errorStr)
            val message = json.optString("message", "")
            val code = json.optString("code", "")
            val details = json.optString("details", "")
            val hint = json.optString("hint", "")

            val builder = StringBuilder()
            if (message.isNotEmpty()) {
                builder.append(message)
            } else {
                builder.append(defaultMessage)
            }
            if (code.isNotEmpty()) {
                builder.append(" (Code: ").append(code).append(")")
            }
            if (details.isNotEmpty() && details != "null") {
                builder.append("\nDetails: ").append(details)
            }
            if (hint.isNotEmpty() && hint != "null") {
                builder.append("\nHint: ").append(hint)
            }
            builder.toString()
        } catch (e: Exception) {
            "$defaultMessage: $errorStr"
        }
    }
}
