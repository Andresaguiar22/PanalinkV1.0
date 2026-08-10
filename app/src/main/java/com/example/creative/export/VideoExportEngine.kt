package com.example.creative.export

import android.content.Context
import android.util.Log
import com.example.creative.core.CreativeProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object VideoExportEngine {
    private const val TAG = "VideoExportEngine"

    suspend fun exportProjectToVideo(
        context: Context,
        project: CreativeProject
    ): ExportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val sourceFile = File(project.sourceMedia)
            if (!sourceFile.exists()) {
                return@withContext ExportResult.Error("Source video file not found: ${project.sourceMedia}")
            }

            // Export video output file
            val targetDir = File(context.filesDir, "media/reels")
            if (!targetDir.exists()) targetDir.mkdirs()

            val outputFile = File(targetDir, "exported_reel_${UUID.randomUUID()}.mp4")

            // Copy source video or perform Android MediaCodec / Transformer pass
            sourceFile.copyTo(outputFile, overwrite = true)

            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Video export completed in ${duration}ms: ${outputFile.absolutePath}")
            ExportResult.Success(outputFile, duration)
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            ExportResult.Error("Video export failed: ${e.message}", e)
        }
    }
}
