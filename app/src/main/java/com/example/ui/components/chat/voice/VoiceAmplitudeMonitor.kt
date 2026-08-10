package com.example.ui.components.chat.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceAmplitudeMonitor(private val barCount: Int = 10) {

    private val _amplitudes = MutableStateFlow(List(barCount) { 0.1f })
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val amplitudeHistory = mutableListOf<Float>()

    private var monitorJob: Job? = null
    private var isPaused: Boolean = false

    fun start(scope: CoroutineScope, getRawAmplitude: () -> Int) {
        stop()
        isPaused = false
        monitorJob = scope.launch(Dispatchers.Default) {
            val currentList = MutableList(barCount) { 0.1f }
            while (monitorJob?.isActive == true) {
                if (!isPaused) {
                    val raw = getRawAmplitude()
                    // Normalize raw amplitude (0..32767) to a factor (0.1..1.0)
                    val normalized = ((raw - 200) / 10000f).coerceIn(0.1f, 1.0f)
                    
                    synchronized(amplitudeHistory) {
                        amplitudeHistory.add(normalized)
                    }

                    // Shift values to create a scrolling/live waveform effect
                    currentList.removeAt(0)
                    currentList.add(normalized)
                    _amplitudes.value = currentList.toList()
                }
                delay(80L) // Sample every 80ms for smooth 12.5 FPS update
            }
        }
    }

    fun getSampledWaveform(targetBars: Int = 40): List<Float> {
        val historySnapshot = synchronized(amplitudeHistory) { amplitudeHistory.toList() }
        if (historySnapshot.isEmpty()) {
            return List(targetBars) { 0.1f }
        }
        if (historySnapshot.size <= targetBars) {
            val result = ArrayList<Float>(targetBars)
            for (i in 0 until targetBars) {
                val index = (i * historySnapshot.size) / targetBars
                result.add(historySnapshot[index.coerceIn(0, historySnapshot.size - 1)])
            }
            return result
        }
        val chunkSize = historySnapshot.size.toFloat() / targetBars
        val result = ArrayList<Float>(targetBars)
        for (i in 0 until targetBars) {
            val start = (i * chunkSize).toInt().coerceIn(0, historySnapshot.size - 1)
            val end = ((i + 1) * chunkSize).toInt().coerceIn(start + 1, historySnapshot.size)
            var sum = 0f
            var count = 0
            for (j in start until end) {
                sum += historySnapshot[j]
                count++
            }
            val avg = if (count > 0) sum / count else historySnapshot[start]
            result.add(avg.coerceIn(0.1f, 1.0f))
        }
        return result
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        isPaused = false
        _amplitudes.value = List(barCount) { 0.1f }
        synchronized(amplitudeHistory) {
            amplitudeHistory.clear()
        }
    }
}
