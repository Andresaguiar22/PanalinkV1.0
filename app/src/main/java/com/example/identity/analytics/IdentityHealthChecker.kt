package com.example.identity.analytics

import android.content.Context
import android.util.Log
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IdentityHealthChecker(
    private val context: Context,
    private val database: PanalinkDatabase
) {
    private val TAG = "IdentityHealthChecker"

    data class HealthReport(
        val totalProfiles: Int,
        val incompleteProfiles: Int,
        val missingAvatarsInRoom: Int,
        val totalAvatarsInStorage: Int,
        val totalCoversInStorage: Int,
        val orphanAvatars: Int,
        val orphanCovers: Int,
        val corruptFiles: Int
    )

    suspend fun runAudit(): HealthReport = withContext(Dispatchers.IO) {
        Log.i(TAG, "Iniciando auditoría de salud de Identidad (IMCE)...")

        val profiles = database.profileDao().getAllProfilesSync()
        
        var incompleteCount = 0
        var missingAvatarsCount = 0
        val roomProfileIds = mutableSetOf<String>()
        val roomAvatarPaths = mutableSetOf<String>()
        val roomCoverPaths = mutableSetOf<String>()

        profiles.forEach { profile ->
            roomProfileIds.add(profile.id)
            if (profile.displayName.isBlank()) {
                incompleteCount++
            }
            if (profile.avatarUrl != null && profile.avatarLocalPath == null) {
                missingAvatarsCount++
            }
            if (profile.avatarLocalPath != null) {
                roomAvatarPaths.add(profile.avatarLocalPath)
            }
            if (profile.coverLocalPath != null) {
                roomCoverPaths.add(profile.coverLocalPath)
            }
        }

        // Storage Audit
        val avatarsDir = File(context.filesDir, "media/avatars")
        val coversDir = File(context.filesDir, "media/covers")

        var totalAvatars = 0
        var totalCovers = 0
        var orphanAvatars = 0
        var orphanCovers = 0
        var corruptFiles = 0

        if (avatarsDir.exists()) {
            avatarsDir.listFiles()?.forEach { file ->
                totalAvatars++
                if (!roomAvatarPaths.contains(file.absolutePath)) {
                    orphanAvatars++
                }
                if (file.length() == 0L) {
                    corruptFiles++
                }
            }
        }

        if (coversDir.exists()) {
            coversDir.listFiles()?.forEach { file ->
                totalCovers++
                if (!roomCoverPaths.contains(file.absolutePath)) {
                    orphanCovers++
                }
                if (file.length() == 0L) {
                    corruptFiles++
                }
            }
        }

        val report = HealthReport(
            totalProfiles = profiles.size,
            incompleteProfiles = incompleteCount,
            missingAvatarsInRoom = missingAvatarsCount,
            totalAvatarsInStorage = totalAvatars,
            totalCoversInStorage = totalCovers,
            orphanAvatars = orphanAvatars,
            orphanCovers = orphanCovers,
            corruptFiles = corruptFiles
        )

        Log.i(TAG, "Auditoría finalizada: $report")
        return@withContext report
    }
}
