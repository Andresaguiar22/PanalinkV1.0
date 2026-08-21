package com.example.data.repository

import android.util.Log
import com.example.data.supabase.SupabaseClient
import com.example.util.PresenceHistoryTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PresenceRepository"

enum class UserPresenceStatus(val rawValue: String, val label: String) {
    ONLINE("online", "En línea"),
    AWAY("away", "Ausente"),
    BUSY("busy", "En llamada"),
    OFFLINE("offline", "Desconectado")
}

enum class SecondaryPresenceStatus(val rawValue: String, val label: String) {
    NONE("none", ""),
    TYPING("typing", "Escribiendo..."),
    RECORDING_AUDIO("recording_audio", "Grabando audio..."),
    UPLOADING_FILE("uploading_file", "Subiendo archivo..."),
    VOICE_CALL("voice_call", "En llamada de voz"),
    VIDEO_CALL("video_call", "En videollamada"),
    MESSAGES_ONLY("messages_only", "Solo mensajes"),
    DND("dnd", "No molestar")
}

enum class CallAvailability(val label: String) {
    AVAILABLE("Disponible para llamadas"),
    MESSAGES_ONLY("Prefiere mensajes")
}

data class UserPresenceInfo(
    val userId: String,
    val status: UserPresenceStatus,
    val secondaryStatus: SecondaryPresenceStatus = SecondaryPresenceStatus.NONE,
    val callAvailability: CallAvailability = CallAvailability.AVAILABLE,
    val lastSeen: Long = System.currentTimeMillis()
)

object PresenceRepository {
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    // Deduplication cache to prevent duplicate processing from Broadcast and postgres_changes
    // Key: userId_status_windowTimestamp
    private val deduplicationCache = ConcurrentHashMap<String, Long>()
    private const val DEDUPLICATION_WINDOW_MS = 5000L

    private val _currentUserStatus = MutableStateFlow(UserPresenceStatus.ONLINE)
    val currentUserStatus: StateFlow<UserPresenceStatus> = _currentUserStatus.asStateFlow()

    private val _currentUserSecondaryStatus = MutableStateFlow(SecondaryPresenceStatus.NONE)
    val currentUserSecondaryStatus: StateFlow<SecondaryPresenceStatus> = _currentUserSecondaryStatus.asStateFlow()

    private val _currentUserCallAvailability = MutableStateFlow(CallAvailability.AVAILABLE)
    val currentUserCallAvailability: StateFlow<CallAvailability> = _currentUserCallAvailability.asStateFlow()

    private val _presenceMap = MutableStateFlow<Map<String, UserPresenceInfo>>(emptyMap())
    val presenceMap: StateFlow<Map<String, UserPresenceInfo>> = _presenceMap.asStateFlow()

    private val gracePeriodJobs = ConcurrentHashMap<String, Job>()
    var gracePeriodDurationMs: Long = 12_000L

    init {
        // Escuchar el flujo global de Realtime Presence de SupabaseClient
        scope.launch {
            SupabaseClient.realtimePresenceState.collect { rawMap ->
                val currentMap = _presenceMap.value.toMutableMap()
                val currentTime = System.currentTimeMillis()

                rawMap.forEach { (userId, presence) ->
                    // System Deduplication Check
                    val timeWindow = presence.lastSeen / DEDUPLICATION_WINDOW_MS
                    val dedupKey = "${userId}_${presence.status}_$timeWindow"
                    val lastProcessed = deduplicationCache[dedupKey]

                    if (lastProcessed == null || currentTime - lastProcessed >= DEDUPLICATION_WINDOW_MS) {
                        deduplicationCache[dedupKey] = currentTime
                        
                        // Clean up old deduplication keys periodically
                        if (deduplicationCache.size > 200) {
                            val cutoff = currentTime - (DEDUPLICATION_WINDOW_MS * 2)
                            deduplicationCache.entries.removeIf { it.value < cutoff }
                        }

                        val statusEnum = when (presence.status.lowercase()) {
                            "online" -> UserPresenceStatus.ONLINE
                            "away" -> UserPresenceStatus.AWAY
                            "busy", "in_call", "on_call" -> UserPresenceStatus.BUSY
                            else -> UserPresenceStatus.OFFLINE
                        }

                        if (statusEnum == UserPresenceStatus.OFFLINE) {
                            val prevPresence = currentMap[userId]
                            if (prevPresence != null && prevPresence.status != UserPresenceStatus.OFFLINE) {
                                // Schedule Grace Period before marking OFFLINE
                                if (gracePeriodJobs[userId] == null) {
                                    gracePeriodJobs[userId] = scope.launch {
                                        delay(gracePeriodDurationMs)
                                        val updatedMap = _presenceMap.value.toMutableMap()
                                        val finalInfo = UserPresenceInfo(
                                            userId = userId,
                                            status = UserPresenceStatus.OFFLINE,
                                            secondaryStatus = SecondaryPresenceStatus.NONE,
                                            callAvailability = prevPresence.callAvailability,
                                            lastSeen = presence.lastSeen
                                        )
                                        updatedMap[userId] = finalInfo
                                        _presenceMap.value = updatedMap
                                        PresenceHistoryTracker.recordEvent(userId, UserPresenceStatus.OFFLINE, presence.lastSeen)
                                        gracePeriodJobs.remove(userId)
                                    }
                                }
                            } else {
                                val info = UserPresenceInfo(
                                    userId = userId,
                                    status = UserPresenceStatus.OFFLINE,
                                    secondaryStatus = SecondaryPresenceStatus.NONE,
                                    lastSeen = presence.lastSeen
                                )
                                currentMap[userId] = info
                                PresenceHistoryTracker.recordEvent(userId, UserPresenceStatus.OFFLINE, presence.lastSeen)
                            }
                        } else {
                            // User is ONLINE, AWAY, or BUSY -> cancel any pending grace period job
                            gracePeriodJobs[userId]?.cancel()
                            gracePeriodJobs.remove(userId)

                            val info = UserPresenceInfo(
                                userId = userId,
                                status = statusEnum,
                                secondaryStatus = currentMap[userId]?.secondaryStatus ?: SecondaryPresenceStatus.NONE,
                                callAvailability = currentMap[userId]?.callAvailability ?: CallAvailability.AVAILABLE,
                                lastSeen = presence.lastSeen
                            )
                            currentMap[userId] = info
                            PresenceHistoryTracker.recordEvent(userId, statusEnum, presence.lastSeen)
                        }
                    }
                }

                _presenceMap.value = currentMap
            }
        }
    }

    /** Effective status honoring the Privacy/Presence center settings. */
    private fun effectiveStatus(): UserPresenceStatus {
        val raw = try {
            val uid = SupabaseClient.currentUser?.id ?: "guest"
            val prefs = com.example.PanaApplication.instance
                .getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
            val invisible = prefs.getBoolean("profile_invisibility_$uid", false)
            val presence = prefs.getString("profile_presence_$uid", "online") ?: "online"
            when {
                invisible || presence == "invisible" -> UserPresenceStatus.OFFLINE
                presence == "busy" -> UserPresenceStatus.BUSY
                else -> _currentUserStatus.value
            }
        } catch (e: Exception) {
            _currentUserStatus.value
        }
        return raw
    }

    /** True when the user hides their last-seen from everyone. */
    private fun isLastSeenHidden(): Boolean {
        return try {
            val uid = SupabaseClient.currentUser?.id ?: "guest"
            val prefs = com.example.PanaApplication.instance
                .getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
            val invisible = prefs.getBoolean("profile_invisibility_$uid", false)
            val lastSeenPref = prefs.getString("privacy_last_seen_$uid", "Mis Contactos") ?: "Mis Contactos"
            invisible || lastSeenPref == "Nadie"
        } catch (e: Exception) {
            false
        }
    }

    /** Called by the Presence/Privacy center so the heartbeat picks up manual states. */
    fun applyManualStatusFromSettings(status: String) {
        _currentUserStatus.value = when (status) {
            "busy" -> UserPresenceStatus.BUSY
            "invisible" -> UserPresenceStatus.OFFLINE
            else -> UserPresenceStatus.ONLINE
        }
        if (SupabaseClient.isConnected) {
            SupabaseClient.broadcastPresence(effectiveStatus().rawValue)
        }
    }

    /**
     * Heartbeat cada 30 segundos usando únicamente Realtime Broadcast.
     * CERO escrituras a la base de datos PostgreSQL durante el heartbeat.
     */
    fun startHeartbeat(currentUserId: String) {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                if (SupabaseClient.isConnected) {
                    SupabaseClient.broadcastPresence(effectiveStatus().rawValue)
                }
                delay(30_000L) // 30 segundos exactos
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Actualiza el estado del usuario local.
     * Si es un cambio manual o eventos críticos (Login, Logout, OFFLINE, Cambio dispositivo),
     * escribe a la tabla public.user_presence en PostgreSQL.
     */
    fun updateMyStatus(status: UserPresenceStatus, isManualOrLifecycle: Boolean = true) {
        _currentUserStatus.value = status
        val currentUid = SupabaseClient.currentUser?.id ?: "me"
        PresenceHistoryTracker.recordEvent(currentUid, status)

        if (SupabaseClient.isConnected) {
            // Honors Privacy center: invisible users always broadcast offline.
            SupabaseClient.broadcastPresence(effectiveStatus().rawValue)
        }

        // Persistir a PostgreSQL SOLAMENTE en eventos de ciclo de vida/cambio manual
        if (isManualOrLifecycle) {
            persistPresenceToDatabase(status)
        }
    }

    fun setSecondaryStatus(secondaryStatus: SecondaryPresenceStatus) {
        _currentUserSecondaryStatus.value = secondaryStatus
    }

    fun onLogin(userId: String) {
        updateMyStatus(UserPresenceStatus.ONLINE, isManualOrLifecycle = true)
        startHeartbeat(userId)
    }

    fun onLogout() {
        stopHeartbeat()
        updateMyStatus(UserPresenceStatus.OFFLINE, isManualOrLifecycle = true)
    }

    fun onDeviceConnected(userId: String) {
        updateMyStatus(UserPresenceStatus.ONLINE, isManualOrLifecycle = true)
        startHeartbeat(userId)
    }

    private fun persistPresenceToDatabase(status: UserPresenceStatus) {
        scope.launch {
            try {
                // Privacy center: never publish last_seen/status when hidden.
                if (isLastSeenHidden() && status != UserPresenceStatus.OFFLINE) {
                    Log.d(TAG, "persistPresenceToDatabase skipped: user hides presence")
                    return@launch
                }
                val currentUid = SupabaseClient.currentUser?.id ?: return@launch
                val service = SupabaseClient.apiService ?: return@launch
                val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())

                val body = mapOf(
                    "user_id" to currentUid,
                    "status" to status.rawValue,
                    "last_seen_at" to nowIso,
                    "updated_at" to nowIso
                )

                service.upsertUserPresence(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = "Bearer ${SupabaseClient.currentToken ?: SupabaseClient.supabaseAnonKey}",
                    presence = body
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist user_presence to DB: ${e.localizedMessage}")
            }
        }
    }

    fun updateMyCallAvailability(availability: CallAvailability) {
        _currentUserCallAvailability.value = availability
    }

    fun getPresenceForUser(userId: String): UserPresenceInfo {
        return _presenceMap.value[userId] ?: UserPresenceInfo(userId, UserPresenceStatus.OFFLINE)
    }

    fun isUserAvailableForCall(userId: String): Pair<Boolean, String> {
        val presence = getPresenceForUser(userId)
        return when (presence.status) {
            UserPresenceStatus.BUSY -> Pair(false, "El usuario está en otra llamada")
            UserPresenceStatus.OFFLINE -> Pair(false, "El usuario está desconectado")
            UserPresenceStatus.ONLINE, UserPresenceStatus.AWAY -> {
                if (presence.callAvailability == CallAvailability.MESSAGES_ONLY) {
                    Pair(false, "Este usuario prefiere mensajes")
                } else {
                    Pair(true, "Disponible")
                }
            }
        }
    }
}

