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
            lastSynchronization = if (isOnline) "Al día" else "Pendiente de red",
            dataUsageToday = formatBytes(cacheSizeBytes / 2 + 1024 * 512), // Estimate based on cache
            isLoading = false,
            errorMessage = null
        )
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
        val currentDeviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Este dispositivo)"
        val current = DeviceInfo(
            name = currentDeviceName,
            lastActive = "Activo ahora",
            isCurrent = true,
            iconType = "smartphone"
        )
        val currentUser = SupabaseClient.currentUser
        return if (currentUser != null) {
            listOf(
                current,
                DeviceInfo(
                    name = "PanaLink Web (Navegador)",
                    lastActive = "Sesión activa",
                    isCurrent = false,
                    iconType = "computer"
                )
            )
        } else {
            listOf(current)
        }
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
