package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VideoCompressorHelper {
    private const val TAG = "VideoCompressorHelper"
    private const val TIMEOUT_USEC = 10000L

    suspend fun compressVideo(
        context: Context,
        uri: Uri?,
        fallbackFile: File?,
        progressCallback: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        Log.i(TAG, "Starting video compression pipeline...")
        Log.i(TAG, "[Target Specs] Resolution: Max 720p (1280x720 / 720x1280)")
        Log.i(TAG, "[Target Specs] Video Bitrate: 2.0 Mbps (Target: 1.5 - 2.5 Mbps)")
        Log.i(TAG, "[Target Specs] Audio Track: Copied directly (Zero quality loss, low CPU)")

        var tempInputFile: File? = null
        var tempOutputFile: File? = null

        try {
            // 1. Create temporary files
            tempInputFile = File.createTempFile("compress_input", ".mp4", context.cacheDir)
            tempOutputFile = File.createTempFile("compress_output", ".mp4", context.cacheDir)

            // 2. Stream input video directly to the temporary input file without loading it in RAM
            if (uri != null) {
                Log.d(TAG, "Streaming video data from URI directly to temp file to avoid RAM overhead: $uri")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempInputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else if (fallbackFile != null && fallbackFile.exists()) {
                Log.d(TAG, "Copying provided fallback file to temp input file")
                fallbackFile.inputStream().use { inputStream ->
                    tempInputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                throw Exception("No hay datos de video ni URI disponibles para comprimir.")
            }

            val inputSize = tempInputFile.length()
            if (inputSize <= 0) {
                throw Exception("El archivo de video de entrada está vacío (0 bytes).")
            }

            val originalSizeMB = inputSize.toDouble() / (1024 * 1024)
            Log.i(TAG, "=== AUDITORÍA DE COMPRESIÓN DE VIDEO: ANTES ===")
            Log.i(TAG, "- Tamaño de video original en disco: $inputSize bytes (${String.format("%.2f", originalSizeMB)} MB)")
            Log.i(TAG, "==============================================")

            // 3. Run actual MediaCodec transcoding
            val success = transcodeVideo(tempInputFile, tempOutputFile) { progress ->
                progressCallback(progress)
            }

            if (success && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                val compressedSize = tempOutputFile.length()
                Log.d(TAG, "Transcoding successful! File saved at: ${tempOutputFile.absolutePath}")
                
                val finalSizeMB = compressedSize.toDouble() / (1024 * 1024)
                Log.i(TAG, "=== AUDITORÍA DE COMPRESIÓN DE VIDEO: DESPUÉS ===")
                Log.i(TAG, "- Tamaño de video comprimido: $compressedSize bytes (${String.format("%.2f", finalSizeMB)} MB)")
                Log.i(TAG, "- Relación de compresión: ${String.format("%.1f", (compressedSize.toFloat() / inputSize.toFloat()) * 100)}%")
                Log.i(TAG, "================================================")
                
                // Return the output file (caller is responsible for deleting it if it's outside cache, 
                // but here it is in cacheDir, so it's relatively safe). 
                // We'll rename it to avoid being deleted in 'finally' block.
                val resultFile = File.createTempFile("panalink_compressed", ".mp4", context.cacheDir)
                if (tempOutputFile.renameTo(resultFile)) {
                    return@withContext resultFile
                } else {
                    // If rename fails, copy it
                    tempOutputFile.inputStream().use { input ->
                        resultFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext resultFile
                }
            } else {
                throw Exception("El proceso de transcodificación falló o generó un archivo vacío.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error grave durante la compresión del video: ${e.message}", e)
            try {
                if (tempInputFile != null && tempInputFile.exists() && tempInputFile.length() > 0) {
                    val originalSize = tempInputFile.length()
                    Log.i(TAG, "FALLBACK: Retornando video original de disco como recurso seguro (${originalSize} bytes)...")
                    val fallbackResult = File.createTempFile("panalink_fallback", ".mp4", context.cacheDir)
                    tempInputFile.inputStream().use { input ->
                        fallbackResult.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext fallbackResult
                }
            } catch (fallbackError: Throwable) {
                Log.e(TAG, "Fallo al intentar leer el video original de fallback", fallbackError)
            }
            throw Exception("El proceso de compresión falló y no se pudo recuperar el video original: ${e.localizedMessage}", e)
        } finally {
            try {
                tempInputFile?.delete()
                tempOutputFile?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean up temporary video files", e)
            }
        }
    }

    /*
     * Legacy version that returns ByteArray. Disabled in Phase 2.1 to prevent memory issues.
     * 
    suspend fun compressVideoToBytes(
        context: Context,
        uri: Uri?,
        fallbackBytes: ByteArray?,
        progressCallback: (Float) -> Unit
    ): ByteArray = withContext(Dispatchers.Default) {
        var fallbackFile: File? = null
        if (fallbackBytes != null) {
            fallbackFile = File.createTempFile("fallback_wrap_", ".mp4", context.cacheDir)
            fallbackFile.writeBytes(fallbackBytes)
        }
        
        try {
            val resultFile = compressVideo(context, uri, fallbackFile, progressCallback)
            val bytes = resultFile.readBytes()
            resultFile.delete()
            bytes
        } finally {
            fallbackFile?.delete()
        }
    }
    */

    /**
     * Highly optimized, robust MediaCodec-based video transcoder.
     * Transcodes H.264 video streams down to max 720p & 2.0 Mbps, while copying audio directly.
     */
    private suspend fun transcodeVideo(
        inputFile: File,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var muxerStarted = false

        try {
            extractor.setDataSource(inputFile.absolutePath)
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                }
            }

            if (videoTrackIndex == -1) {
                Log.w(TAG, "No video track found in input file.")
                return false
            }

            // 1. Calculate Target Dimensions (Max 720p keeping aspect ratio)
            val origWidth = videoFormat!!.getInteger(MediaFormat.KEY_WIDTH)
            val origHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val rotation = if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                videoFormat.getInteger(MediaFormat.KEY_ROTATION)
            } else 0

            Log.d(TAG, "Input Dimensions: ${origWidth}x${origHeight}, Rotation: $rotation")

            val maxDim = 1280
            var targetWidth = origWidth
            var targetHeight = origHeight

            if (origWidth > origHeight) {
                if (origWidth > maxDim) {
                    targetHeight = (origHeight * maxDim) / origWidth
                    targetWidth = maxDim
                }
            } else {
                if (origHeight > maxDim) {
                    targetWidth = (origWidth * maxDim) / origHeight
                    targetHeight = maxDim
                }
            }

            // MediaCodec requires dimensions to be even numbers
            targetWidth = (targetWidth / 2) * 2
            targetHeight = (targetHeight / 2) * 2

            Log.i(TAG, "Target Dimensions: ${targetWidth}x${targetHeight}")

            // 2. Setup MediaMuxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 3. Setup Video Encoder Format
            val encoderFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            // 4. Setup Video Decoder
            decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC)
            decoder.configure(videoFormat, inputSurface, null, 0)
            decoder.start()

            // 5. Setup Audio Track
            var muxerAudioTrackIndex = -1
            if (audioTrackIndex != -1 && audioFormat != null) {
                muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            }

            extractor.selectTrack(videoTrackIndex)

            // 6. Transcoding Loop Setup
            val durationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                val d = videoFormat.getLong(MediaFormat.KEY_DURATION)
                if (d > 0) d else 1_000_000L
            } else 1_000_000L

            var muxerVideoTrackIndex = -1

            var allInputRead = false
            var allDecoderOutputDone = false
            var allEncoderOutputDone = false

            val bufferInfo = MediaCodec.BufferInfo()
            var lastProgressReport = 0f

            val startTime = System.currentTimeMillis()
            val maxAllowedTimeMs = (durationUs / 1000) * 4 + 30000 // 4x duration + 30 seconds buffer
            var consecutiveNoProgressCount = 0

            while (!allEncoderOutputDone) {
                var progressMade = false

                // Feed Input to Decoder
                if (!allInputRead) {
                    val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            inputBuf.clear()
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                allInputRead = true
                                Log.d(TAG, "Video Extractor: Reached EOF")
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                            progressMade = true
                        }
                    }
                }

                // Dequeue from Decoder and render to Encoder input surface
                if (!allDecoderOutputDone) {
                    val decStatus = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (decStatus >= 0) {
                        val doRender = bufferInfo.size != 0
                        decoder.releaseOutputBuffer(decStatus, doRender)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoder.signalEndOfInputStream()
                            allDecoderOutputDone = true
                            Log.d(TAG, "Video Decoder: Signaled EOS to Encoder")
                        }
                        progressMade = true
                    } else if (decStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.d(TAG, "Decoder output format changed")
                        progressMade = true
                    }
                }

                // Dequeue from Encoder and write to Muxer
                val encStatus = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (encStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encStatus)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                val newFormat = encoder.outputFormat
                                muxerVideoTrackIndex = muxer.addTrack(newFormat)
                                muxer.start()
                                muxerStarted = true
                                Log.d(TAG, "Muxer started with video track.")
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerVideoTrackIndex, encodedData, bufferInfo)

                            val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                            if (progress - lastProgressReport >= 0.05f) {
                                lastProgressReport = progress
                                // Map progress to 0.0 - 0.95 (leave remaining for audio copy and finalization)
                                onProgress(progress * 0.9f)
                            }
                        }
                    }

                    encoder.releaseOutputBuffer(encStatus, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        allEncoderOutputDone = true
                        Log.d(TAG, "Video Encoder: Reached EOS")
                    }
                    progressMade = true
                } else if (encStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw RuntimeException("Encoder format changed twice!")
                    }
                    val newFormat = encoder.outputFormat
                    muxerVideoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                    Log.d(TAG, "Muxer started on format changed.")
                    progressMade = true
                }

                // Safety checks to prevent infinite loops and 100% CPU lock
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > maxAllowedTimeMs) {
                    Log.w(TAG, "Transcoding loop exceeded maximum allowed time ($maxAllowedTimeMs ms). Forcing exit.")
                    break
                }

                if (!progressMade) {
                    consecutiveNoProgressCount++
                    if (consecutiveNoProgressCount > 1000) { // ~10 seconds of pure silence
                        Log.w(TAG, "No progress made for 1000 iterations. Forcing exit from loop.")
                        break
                    }
                    kotlinx.coroutines.delay(5) // Prevent busy loop / CPU throttling
                } else {
                    consecutiveNoProgressCount = 0
                    kotlinx.coroutines.yield() // Cooperate with other coroutines
                }
            }

            // 7. Copy Audio Track directly if present to save CPU and maintain perfect audio quality
            if (audioTrackIndex != -1 && muxerAudioTrackIndex != -1) {
                Log.d(TAG, "Copying original audio track stream directly...")
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val audioBuffer = ByteBuffer.allocateDirect(128 * 1024)
                val audioBufferInfo = MediaCodec.BufferInfo()

                var consecutiveNoProgress = 0
                while (true) {
                    audioBuffer.clear()
                    val sampleSize = extractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) {
                        Log.d(TAG, "Audio Copy: Reached EOF")
                        break
                    }
                    if (sampleSize == 0) {
                        consecutiveNoProgress++
                        if (consecutiveNoProgress > 500) {
                            Log.w(TAG, "Audio Copy: Breaking loop due to consecutive empty reads")
                            break
                        }
                    } else {
                        consecutiveNoProgress = 0
                    }
                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = sampleSize
                    audioBufferInfo.presentationTimeUs = extractor.sampleTime
                    @Suppress("WrongConstant")
                    audioBufferInfo.flags = extractor.sampleFlags
                    
                    if (muxerStarted) {
                        muxer.writeSampleData(muxerAudioTrackIndex, audioBuffer, audioBufferInfo)
                    }
                    if (!extractor.advance()) {
                        Log.d(TAG, "Audio Copy: Reached EOF (advance returned false)")
                        break
                    }
                }
            }

            onProgress(1.0f)
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed during transcoding pipeline", e)
            return false
        } finally {
            try { extractor.release() } catch (e: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (e: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (e: Exception) {}
            try { inputSurface?.release() } catch (e: Exception) {}
            try { if (muxerStarted) muxer?.stop(); muxer?.release() } catch (e: Exception) {}
        }
    }

    private fun processFastStart(inputBytes: ByteArray): ByteArray {
        val size = inputBytes.size
        if (size < 8) return inputBytes

        var offset = 0
        var ftypBytes: ByteArray? = null
        var moovBytes: ByteArray? = null
        var mdatOffset = -1
        var moovOffset = -1
        var moovSize = -1L
        var moovHeaderSize = 8

        val buffer = ByteBuffer.wrap(inputBytes).order(ByteOrder.BIG_ENDIAN)

        while (offset < size - 8) {
            buffer.position(offset)
            var atomSize = buffer.getInt().toLong() and 0xFFFFFFFFL
            val atomTypeBytes = ByteArray(4)
            buffer.get(atomTypeBytes)
            val atomType = String(atomTypeBytes, Charsets.US_ASCII)

            var headerSize = 8
            if (atomSize == 1L) {
                if (offset + 16 > size) break
                atomSize = buffer.getLong()
                headerSize = 16
            } else if (atomSize == 0L) {
                atomSize = (size - offset).toLong()
            }

            if (atomSize <= 0 || offset + atomSize > size) {
                break
            }

            when (atomType) {
                "ftyp" -> {
                    ftypBytes = inputBytes.copyOfRange(offset, (offset + atomSize).toInt())
                }
                "moov" -> {
                    moovOffset = offset
                    moovSize = atomSize
                    moovBytes = inputBytes.copyOfRange(offset, (offset + atomSize).toInt())
                    moovHeaderSize = headerSize
                }
                "mdat" -> {
                    mdatOffset = offset
                }
            }

            offset += atomSize.toInt()
        }

        if (ftypBytes == null || moovBytes == null || mdatOffset == -1 || moovOffset == -1) {
            Log.w(TAG, "Faststart bypassed: ftyp, moov, or mdat not found")
            return inputBytes
        }

        if (moovOffset < mdatOffset) {
            Log.i(TAG, "Faststart: moov is already before mdat. Perfect!")
            return inputBytes
        }

        Log.i(TAG, "Faststart: moov is after mdat. Moving moov to the beginning and shifting offsets...")

        val shiftAmount = moovSize
        findAndModifyOffsets(moovBytes, moovHeaderSize, moovBytes.size - moovHeaderSize, shiftAmount)

        val outBytes = ByteArray(size)
        var writePos = 0

        // 1. Write ftyp
        System.arraycopy(ftypBytes, 0, outBytes, writePos, ftypBytes.size)
        writePos += ftypBytes.size

        // 2. Write moov
        System.arraycopy(moovBytes, 0, outBytes, writePos, moovBytes.size)
        writePos += moovBytes.size

        // 3. Write everything else in order EXCEPT ftyp and moov
        offset = 0
        while (offset < size - 8) {
            buffer.position(offset)
            var atomSize = buffer.getInt().toLong() and 0xFFFFFFFFL
            val atomTypeBytes = ByteArray(4)
            buffer.get(atomTypeBytes)
            val atomType = String(atomTypeBytes, Charsets.US_ASCII)

            if (atomSize == 1L) {
                atomSize = buffer.getLong()
            } else if (atomSize == 0L) {
                atomSize = (size - offset).toLong()
            }

            if (atomSize <= 0 || offset + atomSize > size) {
                break
            }

            if (atomType != "ftyp" && atomType != "moov") {
                val len = atomSize.toInt()
                if (writePos + len <= size) {
                    System.arraycopy(inputBytes, offset, outBytes, writePos, len)
                    writePos += len
                }
            }

            offset += atomSize.toInt()
        }

        Log.i(TAG, "Faststart successful! Reorganized file size: $writePos bytes (Expected: $size)")
        return if (writePos == size) outBytes else outBytes.copyOf(writePos)
    }

    private fun findAndModifyOffsets(bytes: ByteArray, offset: Int, length: Int, shift: Long) {
        var pos = offset
        val end = offset + length
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        while (pos < end - 8) {
            buffer.position(pos)
            val size = buffer.getInt().toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (size < 8 || pos + size > end) {
                break
            }

            val contentOffset = pos + 8
            val contentLength = (size - 8).toInt()

            when (type) {
                "moov", "trak", "mdia", "minf", "stbl" -> {
                    findAndModifyOffsets(bytes, contentOffset, contentLength, shift)
                }
                "stco" -> {
                    if (contentLength >= 8) {
                        buffer.position(contentOffset)
                        val versionFlags = buffer.getInt()
                        val entryCount = buffer.getInt()
                        if (contentLength >= 8 + entryCount * 4) {
                            Log.d(TAG, "Faststart: Found stco container with $entryCount entries at pos $pos")
                            for (i in 0 until entryCount) {
                                val currentPos = contentOffset + 8 + i * 4
                                buffer.position(currentPos)
                                val oldOffset = buffer.getInt().toLong() and 0xFFFFFFFFL
                                val newOffset = oldOffset + shift
                                buffer.position(currentPos)
                                buffer.putInt(newOffset.toInt())
                            }
                        }
                    }
                }
                "co64" -> {
                    if (contentLength >= 8) {
                        buffer.position(contentOffset)
                        val versionFlags = buffer.getInt()
                        val entryCount = buffer.getInt()
                        if (contentLength >= 8 + entryCount * 8) {
                            Log.d(TAG, "Faststart: Found co64 container with $entryCount entries at pos $pos")
                            for (i in 0 until entryCount) {
                                val currentPos = contentOffset + 8 + i * 8
                                buffer.position(currentPos)
                                val oldOffset = buffer.getLong()
                                val newOffset = oldOffset + shift
                                buffer.position(currentPos)
                                buffer.putLong(newOffset)
                            }
                        }
                    }
                }
            }
            pos += size.toInt()
        }
    }
}
