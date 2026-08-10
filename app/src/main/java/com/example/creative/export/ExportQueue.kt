package com.example.creative.export

import android.content.Context
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * P6.5A - Background Export Queue Engine
 * Handles rendering queue without blocking UI, preserving Offline First and Room persistence.
 */

data class ExportJob(
    val id: String = java.util.UUID.randomUUID().toString(),
    val project: CreativeProject,
    val resolution: String = "1080p",
    val fps: Int = 30,
    val quality: String = "High",
    val hdrEnabled: Boolean = false,
    val status: ExportStatus = ExportStatus.PENDING,
    val progress: Float = 0f,
    val outputFile: File? = null,
    val errorMessage: String? = null
)

enum class ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

object ExportQueueManager {

    private val queue = mutableListOf<ExportJob>()

    fun enqueueJob(job: ExportJob): String {
        queue.add(job)
        return job.id
    }

    fun getJobStatus(jobId: String): ExportJob? {
        return queue.firstOrNull { it.id == jobId }
    }

    suspend fun processNextJob(context: Context): ExportJob? = withContext(Dispatchers.IO) {
        val pendingJob = queue.firstOrNull { it.status == ExportStatus.PENDING } ?: return@withContext null
        val processingIndex = queue.indexOf(pendingJob)
        val processingJob = pendingJob.copy(status = ExportStatus.PROCESSING, progress = 0.1f)
        if (processingIndex >= 0) queue[processingIndex] = processingJob

        try {
            val outputDir = File(context.filesDir, "exports")
            if (!outputDir.exists()) outputDir.mkdirs()

            val targetFile = File(outputDir, "export_${processingJob.project.id}_${System.currentTimeMillis()}.mp4")
            
            val composition = com.example.creative.video.VideoComposition(
                clips = listOf(
                    com.example.creative.timeline.TimelineClip(
                        id = java.util.UUID.randomUUID().toString(),
                        mediaUriOrPath = processingJob.project.sourceMedia,
                        durationMs = 15000L
                    )
                ),
                textLayers = processingJob.project.layers.filterIsInstance<CreativeLayer.Text>(),
                stickerLayers = processingJob.project.layers.filterIsInstance<CreativeLayer.Sticker>()
            )

            var renderResult: com.example.creative.video.VideoRenderState? = null
            VideoProcessor.processVideoComposition(context, composition).collect { state ->
                renderResult = state
            }

            val isSuccess = renderResult is com.example.creative.video.VideoRenderState.Success
            val successFile = (renderResult as? com.example.creative.video.VideoRenderState.Success)?.exportedFile

            val completedJob = processingJob.copy(
                status = if (isSuccess) ExportStatus.COMPLETED else ExportStatus.FAILED,
                progress = if (isSuccess) 1.0f else 0f,
                outputFile = successFile,
                errorMessage = if (isSuccess) null else "Error procesando render de video"
            )

            if (processingIndex >= 0) queue[processingIndex] = completedJob
            return@withContext completedJob
        } catch (e: Exception) {
            val failedJob = processingJob.copy(
                status = ExportStatus.FAILED,
                progress = 0f,
                errorMessage = e.localizedMessage ?: "Excepción desconocida en render"
            )
            if (processingIndex >= 0) queue[processingIndex] = failedJob
            return@withContext failedJob
        }
    }
}
