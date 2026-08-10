package com.example.identity.storage

import android.content.Context
import android.util.Log
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStorageCleaner(
    private val context: Context,
    private val database: PanalinkDatabase
) {
    private val TAG = "MediaStorageCleaner"

    suspend fun cleanOrphanFiles(): Int = withContext(Dispatchers.IO) {
        Log.i(TAG, "Iniciando limpieza inteligente de almacenamiento...")
        var deletedCount = 0

        // Obtenemos todos los paths referenciados en Room para no borrarlos
        val profiles = database.profileDao().getAllProfilesSync()
        val roomAvatarPaths = profiles.mapNotNull { it.avatarLocalPath }.toSet()
        val roomCoverPaths = profiles.mapNotNull { it.coverLocalPath }.toSet()

        val avatarsDir = File(context.filesDir, "media/avatars")
        val coversDir = File(context.filesDir, "media/covers")

        // Limpiar avatares
        if (avatarsDir.exists()) {
            avatarsDir.listFiles()?.forEach { file ->
                if (!roomAvatarPaths.contains(file.absolutePath)) {
                    // Es huérfano, borrar
                    val deleted = file.delete()
                    if (deleted) deletedCount++
                    Log.i(TAG, "Borrado avatar huérfano: ${file.name}")
                } else if (file.length() == 0L) {
                    // Archivo corrupto (0 bytes), pero referenciado, mejor lo borramos para que se vuelva a descargar
                    val deleted = file.delete()
                    if (deleted) deletedCount++
                    Log.i(TAG, "Borrado avatar corrupto: ${file.name}")
                }
            }
        }

        // Limpiar portadas (covers)
        if (coversDir.exists()) {
            coversDir.listFiles()?.forEach { file ->
                if (!roomCoverPaths.contains(file.absolutePath)) {
                    val deleted = file.delete()
                    if (deleted) deletedCount++
                    Log.i(TAG, "Borrado cover huérfano: ${file.name}")
                } else if (file.length() == 0L) {
                    val deleted = file.delete()
                    if (deleted) deletedCount++
                    Log.i(TAG, "Borrado cover corrupto: ${file.name}")
                }
            }
        }

        Log.i(TAG, "Limpieza inteligente finalizada. Archivos eliminados: $deletedCount")
        return@withContext deletedCount
    }
}
