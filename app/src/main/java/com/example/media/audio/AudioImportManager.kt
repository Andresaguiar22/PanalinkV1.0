package com.example.media.audio

import android.content.Context
import android.net.Uri
import com.example.media.dedup.MediaDeduplicationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * P6.7.1 - Audio Import Manager
 * Imports audio files from Storage Access Framework (SAF), computes SHA-256 for deduplication, extracts metadata, saves to internal storage and persists in Room.
 */
class AudioImportManager(
    private val context: Context,
    private val audioRepository: AudioRepository
) {

    suspend fun importAudioFromUri(
        uri: Uri,
        userId: String,
        trackType: String = "IMPORTED"
    ): AudioTrackEntity? = withContext(Dispatchers.IO) {
        try {
            val audioDir = File(context.filesDir, "media/audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            val tempFile = File(audioDir, "import_temp_${UUID.randomUUID()}.tmp")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext null
            }

            // Calculate SHA-256 hash for deduplication
            val fileHash = MediaDeduplicationEngine.calculateSha256(tempFile)

            // Check if hash already exists in repository
            if (!fileHash.isNullOrEmpty()) {
                val existingTrack = audioRepository.getTrackByHash(fileHash)
                if (existingTrack != null && File(existingTrack.filePath).exists()) {
                    tempFile.delete() // Clean temp file, return existing track
                    return@withContext existingTrack
                }
            }

            // Move temp file to final destination
            val ext = getExtensionFromUri(uri)
            val finalFile = File(audioDir, "track_${UUID.randomUUID()}.$ext")
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            // Extract metadata (title, artist, album, duration, embedded cover art)
            val metadata = AudioMetadataReader.extractMetadata(context, finalFile)

            val track = AudioTrackEntity(
                id = "track_${UUID.randomUUID()}",
                userId = userId,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                coverPath = metadata.coverPath,
                durationMs = metadata.durationMs,
                filePath = finalFile.absolutePath,
                fileHash = fileHash,
                trackType = trackType
            )

            audioRepository.saveTrack(track)
            track
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importMultipleAudios(
        uris: List<Uri>,
        userId: String,
        trackType: String = "IMPORTED"
    ): List<AudioTrackEntity> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            importAudioFromUri(uri, userId, trackType)
        }
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val path = uri.path ?: ""
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3", "m4a", "aac", "wav", "ogg", "flac" -> ext
            else -> "mp3"
        }
    }
}
