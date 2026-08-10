package com.example.creative.post

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import com.example.creative.export.ExportResult
import com.example.creative.export.ImageExportEngine
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingPostEntity
import com.example.data.supabase.SupabaseClient
import com.example.media.dedup.MediaDeduplicationEngine
import com.example.worker.PostUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID

/**
 * P6.6.2 - Post Export Coordinator
 * Coordinates rendering PostStudioProject pages -> MediaStorageManager -> PendingPostEntity -> PostUploadWorker -> Supabase.
 */
object PostExportCoordinator {

    suspend fun exportAndQueuePost(
        context: Context,
        project: PostStudioProject,
        privacy: String = "public"
    ): String = withContext(Dispatchers.IO) {
        val db = PanalinkDatabase.getDatabase(context)
        val pendingPostDao = db.pendingPostDao()

        val renderedMediaUris = mutableListOf<String>()

        project.pages.forEachIndexed { index, page ->
            val mainMediaLayer = page.getMainMediaLayer()

            val renderedFile: File? = if (page.layers.size == 1 && mainMediaLayer is CreativeLayer.Image) {
                File(mainMediaLayer.imageUriOrPath)
            } else if (page.layers.size == 1 && mainMediaLayer is CreativeLayer.Video) {
                File(mainMediaLayer.videoUriOrPath)
            } else {
                val width = 1080
                val height = when (page.aspectRatio) {
                    "1:1" -> 1080
                    "16:9" -> 607
                    else -> 1350 // 4:5
                }
                val pageProject = CreativeProject(
                    id = "${project.id}_page_$index",
                    sourceMedia = (mainMediaLayer as? CreativeLayer.Image)?.imageUriOrPath ?: "",
                    layers = page.layers,
                    type = CreativeType.POST
                )
                val exportResult = ImageExportEngine.exportProjectToImage(
                    context = context,
                    project = pageProject,
                    outputWidth = width,
                    outputHeight = height
                )
                if (exportResult is ExportResult.Success) {
                    exportResult.exportedFile
                } else null
            }

            if (renderedFile != null && renderedFile.exists()) {
                val deduplicatedFile = MediaDeduplicationEngine.deduplicateFile(
                    renderedFile,
                    renderedFile.parentFile ?: context.filesDir
                )
                renderedMediaUris.add(deduplicatedFile.absolutePath)
            } else if (mainMediaLayer is CreativeLayer.Image) {
                renderedMediaUris.add(mainMediaLayer.imageUriOrPath)
            } else if (mainMediaLayer is CreativeLayer.Video) {
                renderedMediaUris.add(mainMediaLayer.videoUriOrPath)
            }
        }

        val pendingPostId = UUID.randomUUID().toString()
        val currentUserId = SupabaseClient.currentUser?.id ?: ""

        val postType = if (renderedMediaUris.size > 1) "ALBUM" else if (renderedMediaUris.isNotEmpty()) "IMAGE" else "TEXT"

        val mediaUrisJson = JSONArray(renderedMediaUris).toString()

        val fullCaption = buildString {
            append(project.caption)
            if (project.hashtags.isNotEmpty()) {
                append("\n\n")
                append(project.hashtags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" })
            }
            if (!project.location.isNullOrBlank()) {
                append("\n📍 ${project.location}")
            }
        }.trim()

        val pendingEntity = PendingPostEntity(
            id = pendingPostId,
            userId = currentUserId,
            content = fullCaption,
            type = postType,
            mediaUrisJson = mediaUrisJson,
            privacy = privacy,
            status = "pending",
            createdAt = System.currentTimeMillis(),
            progress = 0f
        )

        pendingPostDao.insertPost(pendingEntity)

        // Queue WorkManager job
        val workRequest = OneTimeWorkRequestBuilder<PostUploadWorker>()
            .setInputData(
                workDataOf(
                    "pendingPostId" to pendingPostId,
                    "serverPostId" to project.id
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        pendingPostId
    }
}
