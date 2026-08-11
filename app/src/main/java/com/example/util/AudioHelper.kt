package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorder(private val context: Context) {
    private val TAG = "AudioRecorder"
    private var mediaRecorder: MediaRecorder? = null
    var currentFile: File? = null
        private set

    private fun cleanOldTempAudioFilesAsync() {
        Thread {
            try {
                val cacheDir = context.cacheDir
                val activePath = currentFile?.absolutePath
                val now = System.currentTimeMillis()
                val cutoff = now - 6 * 60 * 60 * 1000L // 6 hours
                cacheDir.listFiles { file ->
                    file.name.startsWith("voice_note_") && file.name.endsWith(".m4a")
                }?.forEach { file ->
                    if (file.absolutePath != activePath && file.lastModified() < cutoff) {
                        try {
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning old temp audio files", e)
            }
        }.start()
    }

    fun startRecording(): File? {
        if (isRecording || mediaRecorder != null) {
            Log.w(TAG, "AudioRecorder: Recording is already active. Ignoring second startRecording request.")
            return currentFile
        }
        cleanOldTempAudioFilesAsync()
        try {
            val outputDir = context.cacheDir
            val file = File.createTempFile("voice_note_", ".m4a", outputDir)
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000) // 64kbps is plenty for voice
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            Log.d(TAG, "AUDIO_RECORD_START: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "AUDIO_RECORD_ERROR: Error starting recording", e)
            currentFile = null
            mediaRecorder = null
            return null
        }
    }

    fun stopRecording(): File? {
        val recorder = mediaRecorder ?: return null
        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null
            val file = currentFile
            Log.d(TAG, "AUDIO_RECORD_STOP: Recording stopped successfully. Path: ${file?.absolutePath}")
            if (file != null && file.exists()) {
                 Log.d(TAG, "AUDIO_FILE_CREATED: size=${file.length()} bytes")
            }
            file
        } catch (e: Exception) {
            Log.w(TAG, "Recording stopped too soon or failed: ${e.message}")
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaRecorder = null
        currentFile?.delete()
        currentFile = null
        Log.d(TAG, "Audio recording cancelled and cleaned up")
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                Log.d(TAG, "Audio recording paused")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                Log.d(TAG, "Audio recording resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

data class AudioPlayerState(
    val currentUrl: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val playbackSpeed: Float = 1f
)

class AudioPlayer {
    private val TAG = "AudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    var currentUrl: String? = null
        private set

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && (mediaPlayer?.isPlaying == true)) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val dur = mediaPlayer?.duration?.toLong() ?: 0L
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playerState.value.durationMs,
                        isPlaying = true,
                        isPrepared = true
                    )
                    Log.d(TAG, "currentPositionMs=$pos durationMs=${_playerState.value.durationMs} isPlaying=true")
                } catch (e: Exception) {
                    // Ignore transient position reads
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    fun play(
        url: String,
        onPrepared: (Int) -> Unit = {},
        onCompletion: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val currentSpeed = _playerState.value.playbackSpeed
            release()
            currentUrl = url
            _playerState.value = AudioPlayerState(
                currentUrl = url,
                isPrepared = false,
                isPlaying = false,
                playbackSpeed = currentSpeed
            )
            val player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    val dur = mp.duration.toLong()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1f) {
                        try {
                            val params = mp.playbackParams
                            params.speed = currentSpeed
                            mp.playbackParams = params
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying speed on prepared", e)
                        }
                    }
                    _playerState.value = _playerState.value.copy(
                        currentUrl = url,
                        currentPositionMs = 0L,
                        durationMs = dur,
                        isPlaying = true,
                        isPrepared = true
                    )
                    Log.d(TAG, "Prepared for $url: currentPositionMs=0 durationMs=$dur isPlaying=true")
                    onPrepared(mp.duration)
                    mp.start()
                    startProgressLoop()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Completion event for $url")
                    stopProgressLoop()
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = 0L,
                        isPlaying = false
                    )
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra for $url")
                    stopProgressLoop()
                    release()
                    _playerState.value = AudioPlayerState(playbackSpeed = currentSpeed)
                    onError("MediaPlayer error what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio URL: $url", e)
            stopProgressLoop()
            release()
            _playerState.value = AudioPlayerState()
            onError(e.localizedMessage ?: "Error desconocido")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        } finally {
            stopProgressLoop()
            val pos = mediaPlayer?.currentPosition?.toLong() ?: _playerState.value.currentPositionMs
            _playerState.value = _playerState.value.copy(
                currentPositionMs = pos,
                isPlaying = false
            )
            Log.d(TAG, "Paused for ${currentUrl}: currentPositionMs=$pos durationMs=${_playerState.value.durationMs} isPlaying=false")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    if (_playerState.value.currentPositionMs >= _playerState.value.durationMs && _playerState.value.durationMs > 0) {
                        mp.seekTo(0)
                    }
                    mp.start()
                    _playerState.value = _playerState.value.copy(isPlaying = true)
                    startProgressLoop()
                    Log.d(TAG, "Resumed for ${currentUrl}: currentPositionMs=${_playerState.value.currentPositionMs} durationMs=${_playerState.value.durationMs} isPlaying=true")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio", e)
        }
    }

    fun seekTo(msec: Int) {
        try {
            mediaPlayer?.let { mp ->
                mp.seekTo(msec)
                val pos = msec.toLong().coerceIn(0L, if (_playerState.value.durationMs > 0) _playerState.value.durationMs else Long.MAX_VALUE)
                _playerState.value = _playerState.value.copy(currentPositionMs = pos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying || _playerState.value.isPrepared) {
                        val params = mp.playbackParams
                        params.speed = speed
                        mp.playbackParams = params
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting playback speed", e)
        }
    }

    val isPlaying: Boolean
        get() = _playerState.value.isPlaying

    val currentPosition: Int
        get() = _playerState.value.currentPositionMs.toInt()

    val duration: Int
        get() = _playerState.value.durationMs.toInt()

    fun release() {
        stopProgressLoop()
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        currentUrl = null
        _playerState.value = AudioPlayerState()
    }
}
