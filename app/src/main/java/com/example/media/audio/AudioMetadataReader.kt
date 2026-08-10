package com.example.media.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ExtractedAudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverPath: String? = null
)

/**
 * P6.7.1 - Audio Metadata Reader
 * Extracts ID3/MP4 metadata (title, artist, album, duration, embedded cover art) from audio files or URIs without blocking the UI.
 */
object AudioMetadataReader {

    suspend fun extractMetadata(context: Context, file: File): ExtractedAudioMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            readMetadataFromRetriever(context, retriever, file.name)
        } catch (e: Exception) {
            ExtractedAudioMetadata(
                title = file.nameWithoutExtension.ifBlank { "Audio Sin Título" },
                artist = "Artista Desconocido",
                album = "Álbum Desconocido",
                durationMs = 0L,
                coverPath = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun extractMetadata(context: Context, uri: Uri): ExtractedAudioMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            readMetadataFromRetriever(context, retriever, "audio_track")
        } catch (e: Exception) {
            ExtractedAudioMetadata(
                title = "Audio Importado",
                artist = "Artista Desconocido",
                album = "Álbum Desconocido",
                durationMs = 0L,
                coverPath = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun readMetadataFromRetriever(
        context: Context,
        retriever: MediaMetadataRetriever,
        defaultName: String
    ): ExtractedAudioMetadata {
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() } ?: defaultName.substringBeforeLast(".")

        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() } ?: "Artista Desconocido"

        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            ?.takeIf { it.isNotBlank() } ?: "Álbum Desconocido"

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L

        val embeddedPicture = retriever.embeddedPicture
        var coverPath: String? = null
        if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
            try {
                val coverDir = File(context.filesDir, "media/covers")
                if (!coverDir.exists()) coverDir.mkdirs()
                val coverFile = File(coverDir, "cover_${UUID.randomUUID()}.jpg")
                FileOutputStream(coverFile).use { fos ->
                    fos.write(embeddedPicture)
                }
                coverPath = coverFile.absolutePath
            } catch (_: Exception) {}
        }

        return ExtractedAudioMetadata(
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            coverPath = coverPath
        )
    }
}
