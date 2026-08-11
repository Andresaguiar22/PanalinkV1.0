package com.example.ui.components.chat.voice

import android.content.Context
import com.example.util.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

sealed interface VoiceRecordingResult {
    data class Success(
        val file: File,
        val durationSeconds: Int,
        val durationMillis: Long,
        val waveform: List<Float> = emptyList()
    ) : VoiceRecordingResult

    object TooShort : VoiceRecordingResult
    object Error : VoiceRecordingResult
    object Cancelled : VoiceRecordingResult
}

class VoiceRecordingController(private val context: Context) {
    private val audioRecorder = AudioRecorder(context)
    private val amplitudeMonitor = VoiceAmplitudeMonitor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val amplitudes: StateFlow<List<Float>> = amplitudeMonitor.amplitudes
    
    var recordingState: RecordingState = RecordingState.IDLE
        private set

    private var accumulatedTimeMillis: Long = 0L
    private var lastResumeTimeMillis: Long = 0L

    fun start(): File? {
        if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
            android.util.Log.w("VoiceRecordingController", "Already recording or paused. Ignoring start.")
            return audioRecorder.currentFile
        }

        val file = audioRecorder.startRecording()
        if (file != null) {
            accumulatedTimeMillis = 0L
            lastResumeTimeMillis = System.currentTimeMillis()
            recordingState = RecordingState.RECORDING
            amplitudeMonitor.start(scope) { audioRecorder.getMaxAmplitude() }
        } else {
            recordingState = RecordingState.IDLE
        }
        return file
    }

    fun stopAndValidate(minDurationMillis: Long = 300L, fallbackDurationSeconds: Int = 0): VoiceRecordingResult {
        if (recordingState != RecordingState.RECORDING && recordingState != RecordingState.PAUSED) {
            return VoiceRecordingResult.Error
        }
        val durationMillis = getElapsedMillis()
        val file = audioRecorder.stopRecording()
        recordingState = RecordingState.STOPPED
        val waveform = amplitudeMonitor.getSampledWaveform(35)
        amplitudeMonitor.stop()

        if (file == null || !file.exists() || file.length() == 0L) {
            return VoiceRecordingResult.Error
        }

        if (durationMillis < minDurationMillis) {
            try {
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            return VoiceRecordingResult.TooShort
        }

        val calculatedSeconds = (durationMillis / 1000).toInt()
        val finalSeconds = maxOf(1, maxOf(calculatedSeconds, fallbackDurationSeconds))

        return VoiceRecordingResult.Success(
            file = file,
            durationSeconds = finalSeconds,
            durationMillis = durationMillis,
            waveform = waveform
        )
    }

    fun cancel() {
        if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
            audioRecorder.cancelRecording()
            recordingState = RecordingState.IDLE
            accumulatedTimeMillis = 0L
            amplitudeMonitor.stop()
        }
    }

    fun pause() {
        if (recordingState == RecordingState.RECORDING) {
            accumulatedTimeMillis += (System.currentTimeMillis() - lastResumeTimeMillis)
            audioRecorder.pauseRecording()
            recordingState = RecordingState.PAUSED
            amplitudeMonitor.pause()
        }
    }

    fun resume() {
        if (recordingState == RecordingState.PAUSED) {
            lastResumeTimeMillis = System.currentTimeMillis()
            audioRecorder.resumeRecording()
            recordingState = RecordingState.RECORDING
            amplitudeMonitor.resume()
        }
    }

    fun isCurrentlyRecording(): Boolean = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED

    fun release() {
        try {
            cancel()
        } catch (_: Exception) {}
        try {
            amplitudeMonitor.stop()
        } catch (_: Exception) {}
        try {
            scope.cancel()
        } catch (_: Exception) {}
    }

    fun getElapsedMillis(): Long {
        return when (recordingState) {
            RecordingState.IDLE, RecordingState.STOPPED -> 0L
            RecordingState.PAUSED -> accumulatedTimeMillis
            RecordingState.RECORDING -> accumulatedTimeMillis + (System.currentTimeMillis() - lastResumeTimeMillis)
        }
    }
}
