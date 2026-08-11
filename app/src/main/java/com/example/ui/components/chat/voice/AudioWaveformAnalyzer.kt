package com.example.ui.components.chat.voice

import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object AudioWaveformAnalyzer {
    suspend fun analyze(file: File, targetBars: Int = 35): List<Float> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Float>()
        if (!file.exists() || file.length() == 0L) {
            return@withContext List(targetBars) { 0.05f }
        }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) return@withContext List(targetBars) { 0.05f }

            extractor.selectTrack(audioTrackIndex)
            val maxInputSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(maxInputSize)

            val sampleSizes = mutableListOf<Int>()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                sampleSizes.add(sampleSize)
                extractor.advance()
            }

            if (sampleSizes.isEmpty()) return@withContext List(targetBars) { 0.05f }

            // Group into targetBars
            val chunkSize = sampleSizes.size.toFloat() / targetBars
            var maxVal = 1
            val chunkAverages = mutableListOf<Float>()

            for (i in 0 until targetBars) {
                val start = (i * chunkSize).toInt().coerceIn(0, sampleSizes.size - 1)
                val end = ((i + 1) * chunkSize).toInt().coerceIn(start + 1, sampleSizes.size)
                var sum = 0
                var count = 0
                for (j in start until end) {
                    sum += sampleSizes[j]
                    count++
                }
                val avg = if (count > 0) sum / count else sampleSizes[start]
                if (avg > maxVal) maxVal = avg
                chunkAverages.add(avg.toFloat())
            }

            // Normalize
            chunkAverages.forEach {
                val normalized = (it / maxVal.toFloat()).coerceIn(0.05f, 1.0f)
                result.add(normalized)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext List(targetBars) { 0.05f }
        } finally {
            extractor.release()
        }

        if (result.size < targetBars) {
            return@withContext List(targetBars) { 0.05f }
        }
        result
    }
}
