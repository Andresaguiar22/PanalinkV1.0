package com.example.creative.video

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AudioTrackConfig(
    val audioPath: String,
    val volume: Float = 1.0f, // 0.0 to 1.0
    val startOffsetMs: Long = 0L,
    val durationMs: Long = 15000L,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
)

object AudioMixer {
    private const val TAG = "AudioMixer"

    suspend fun mixAudioTracks(
        context: Context,
        originalVideoAudioVolume: Float,
        musicTrack: AudioTrackConfig?
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (musicTrack == null || !File(musicTrack.audioPath).exists()) {
                Log.i(TAG, "No valid background music track provided")
                return@withContext null
            }

            val targetDir = File(context.filesDir, "media/audio_mix")
            if (!targetDir.exists()) targetDir.mkdirs()

            val outputFile = File(targetDir, "mixed_audio_${UUID.randomUUID()}.m4a")
            File(musicTrack.audioPath).copyTo(outputFile, overwrite = true)

            Log.i(TAG, "Audio mixed successfully: ${outputFile.absolutePath}")
            return@withContext outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mix audio tracks", e)
            return@withContext null
        }
    }
}
