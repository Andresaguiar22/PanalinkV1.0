package com.example.ui.components.chat.voice

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object AudioWaveformAnalyzer {

    /**
     * Extrae físicamente las muestras PCM del archivo .m4a mediante MediaExtractor y MediaCodec
     * y las reduce a [targetBars] barras basadas en la amplitud real.
     */
    suspend fun analyze(file: File, targetBars: Int = 35): List<Float> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext List(targetBars) { 0.05f }
        }

        val extractor = MediaExtractor()
        val resultAmplitudes = mutableListOf<Float>()

        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext List(targetBars) { 0.05f }
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime == null) {
                extractor.release()
                return@withContext List(targetBars) { 0.05f }
            }

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            val pcmAmplitudes = mutableListOf<Float>()
            val timeoutUs = 10000L

            while (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    if (buffer != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && info.size > 0) {
                        // Analizar amplitudes PCM 16-bit
                        outBuffer.position(info.offset)
                        outBuffer.limit(info.offset + info.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        val shortBuffer = outBuffer.asShortBuffer()
                        
                        var maxAmpInFrame = 0f
                        while (shortBuffer.hasRemaining()) {
                            val sample = shortBuffer.get()
                            val absSample = abs(sample.toInt()).toFloat()
                            if (absSample > maxAmpInFrame) {
                                maxAmpInFrame = absSample
                            }
                        }
                        if (maxAmpInFrame > 0) {
                            pcmAmplitudes.add(maxAmpInFrame)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                }
            }

            codec.stop()
            codec.release()

            if (pcmAmplitudes.isEmpty()) {
                extractor.release()
                return@withContext List(targetBars) { 0.05f }
            }

            // Downsample a targetBars
            val chunkSize = Math.max(1, pcmAmplitudes.size / targetBars)
            val maxGlobalAmp = pcmAmplitudes.maxOrNull() ?: 1f
            
            for (i in 0 until targetBars) {
                val start = i * chunkSize
                val end = minOf(start + chunkSize, pcmAmplitudes.size)
                var maxChunkAmp = 0f
                for (j in start until end) {
                    if (pcmAmplitudes[j] > maxChunkAmp) {
                        maxChunkAmp = pcmAmplitudes[j]
                    }
                }
                // Normalizar entre 0.05 y 1.0
                val normalized = (maxChunkAmp / maxGlobalAmp).coerceIn(0.05f, 1.0f)
                resultAmplitudes.add(normalized)
            }
            
            while (resultAmplitudes.size < targetBars) {
                resultAmplitudes.add(0.05f)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            extractor.release()
            return@withContext List(targetBars) { 0.05f }
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                // Ignore
            }
        }

        resultAmplitudes.take(targetBars)
    }
}
