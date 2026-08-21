package com.example.ui.settings.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.data.database.PanalinkDatabase
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.ActivityUiState
import com.example.ui.settings.models.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityRepository(private val context: Context) {

    suspend fun loadActivitySummary(): ActivityUiState = withContext(Dispatchers.IO) {
        val messagesCount = getMessagesCount()
        val callsCount = getCallsCount()
        
        val dbFile = context.getDatabasePath("panalink_database")
        val dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        
        val cacheSizeBytes = calculateDirSize(context.cacheDir)
        val filesSizeBytes = calculateDirSize(context.filesDir)
        val totalMediaAndFiles = cacheSizeBytes + filesSizeBytes
        val totalStorageBytes = dbSizeBytes + totalMediaAndFiles

        val (isOnline, connStatus) = checkConnectionStatus()
        val devices = getActiveDevices()

        ActivityUiState(
            messagesCount = messagesCount,
            callsCount = callsCount,
            storageUsed = formatBytes(totalStorageBytes),
            databaseSize = formatBytes(dbSizeBytes),
            mediaSize = formatBytes(totalMediaAndFiles),
            connectionStatus = connStatus,
            isOnline = isOnline,
            activeDevices = devices,
            lastSynchronization = formatLastSync(),
            dataUsageToday = getRealDataUsage(),
            isLoading = false,
            errorMessage = null
        )
    }

    /** Real per-app network usage (rx+tx) since last device boot via TrafficStats. */
    private fun getRealDataUsage(): String {
        return try {
            val uid = android.os.Process.myUid()
            val rx = android.net.TrafficStats.getUidRxBytes(uid)
            val tx = android.net.TrafficStats.getUidTxBytes(uid)
            if (rx < 0 || tx < 0) return "No disponible"
            formatBytes(rx + tx) + " desde el último reinicio"
        } catch (e: Exception) {
            "No disponible"
        }
    }

    /** Real timestamp of the last successful messages sync (written by SyncMessagesWorker). */
    private fun formatLastSync(): String {
        return try {
            val prefs = context.getSharedPreferences(
                com.example.ui.settings.models.SettingsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE
            )
            val at = prefs.getLong(com.example.ui.settings.models.SettingsKeys.LAST_MESSAGES_SYNC_AT, 0L)
            if (at <= 0L) return "Aún no sincronizado"
            val diffMs = System.currentTimeMillis() - at
            when {
                diffMs < 60_000L -> "Ahora mismo"
                diffMs < 3_600_000L -> "Hace ${diffMs / 60_000L} min"
                diffMs < 86_400_000L -> "Hace ${diffMs / 3_600_000L} h"
                else -> "Hace ${diffMs / 86_400_000L} días"
            }
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    private fun getMessagesCount(): Long {
        return try {
            val db = PanalinkDatabase.getDatabase(context)
            val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM local_messages")
            cursor.use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCallsCount(): Long {
        return try {
            val db = PanalinkDatabase.getDatabase(context)
            val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM local_messages WHERE messageType = 'call' OR messageType = 'audio_call' OR messageType = 'video_call' OR content LIKE '%durationSeconds%'")
            cursor.use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateDirSize(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        try {
            dir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        } catch (_: Exception) { }
        return size
    }

    private fun checkConnectionStatus(): Pair<Boolean, String> {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val network = cm.activeNetwork
                if (network != null) {
                    val caps = cm.getNetworkCapabilities(network)
                    if (caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                        val speed = if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "Excelente (Wi-Fi)" else "Buena (Móvil)"
                        return Pair(true, speed)
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(false, "Desconectado")
    }

    private fun getActiveDevices(): List<DeviceInfo> {
        // Only the real current device is known locally; there is no multi-device
        // session registry in the backend, so we do not fabricate entries.
        val currentDeviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Este dispositivo)"
        return listOf(
            DeviceInfo(
                name = currentDeviceName,
                lastActive = "Activo ahora",
                isCurrent = true,
                iconType = "smartphone"
            )
        )
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val k = 1024.0
        val sizes = arrayOf("B", "KB", "MB", "GB")
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceIn(0, sizes.lastIndex)
        val value = bytes / Math.pow(k, i.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", value, sizes[i])
    }
}
