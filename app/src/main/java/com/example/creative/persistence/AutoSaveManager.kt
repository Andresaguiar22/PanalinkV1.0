package com.example.creative.persistence

import android.content.Context
import com.example.creative.core.CreativeProject
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UnfinishedDraftInfo(
    val projectId: String,
    val projectType: String,
    val lastModifiedMs: Long
)

object AutoSaveManager {

    suspend fun saveDraft(context: Context, project: CreativeProject) = withContext(Dispatchers.IO) {
        val db = PanalinkDatabase.getDatabase(context)
        val entity = CreativeProjectEntity(
            id = project.id,
            sourceMedia = project.sourceMedia,
            type = project.type.name,
            layersJson = "", // Serialized layers
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            tempExportPath = null
        )
        db.creativeProjectDao().insertOrUpdateProject(entity)
    }

    suspend fun checkForUnfinishedDraft(context: Context): UnfinishedDraftInfo? = withContext(Dispatchers.IO) {
        val db = PanalinkDatabase.getDatabase(context)
        val latestEntity = db.creativeProjectDao().getProjectById("current_active_draft")
        if (latestEntity != null) {
            UnfinishedDraftInfo(
                projectId = latestEntity.id,
                projectType = latestEntity.type,
                lastModifiedMs = latestEntity.updatedAt
            )
        } else {
            null
        }
    }

    suspend fun clearDraft(context: Context, projectId: String) = withContext(Dispatchers.IO) {
        val db = PanalinkDatabase.getDatabase(context)
        db.creativeProjectDao().deleteProjectById(projectId)
    }
}

