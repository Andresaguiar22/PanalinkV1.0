package com.example.creative.post

import android.content.Context
import com.example.creative.persistence.AutoSaveManager
import com.example.creative.persistence.CreativeProjectEntity
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * P6.6.2 - Post Studio Repository
 * Manages Room database persistence & disk draft serialization for PostStudioProjects.
 */
class PostStudioRepository(private val context: Context) {

    private val draftsDir = File(context.filesDir, "post_drafts").apply { if (!exists()) mkdirs() }

    suspend fun saveDraft(project: PostStudioProject) = withContext(Dispatchers.IO) {
        val creativeProj = project.toCreativeProject()
        AutoSaveManager.saveDraft(context, creativeProj)

        // Also save detailed JSON with multi-page structure
        val file = File(draftsDir, "post_${project.id}.json")
        val json = serializePostProject(project)
        file.writeText(json.toString())

        // Save active pointer
        val db = PanalinkDatabase.getDatabase(context)
        val activeEntity = CreativeProjectEntity(
            id = "active_post_draft",
            sourceMedia = creativeProj.sourceMedia,
            type = "POST",
            layersJson = json.toString(),
            createdAt = project.createdAtMs,
            updatedAt = System.currentTimeMillis()
        )
        db.creativeProjectDao().insertOrUpdateProject(activeEntity)
    }

    suspend fun loadDraft(projectId: String): PostStudioProject? = withContext(Dispatchers.IO) {
        val file = File(draftsDir, "post_$projectId.json")
        if (file.exists()) {
            val jsonStr = file.readText()
            return@withContext deserializePostProject(JSONObject(jsonStr))
        }

        // Try DB fallback
        val db = PanalinkDatabase.getDatabase(context)
        val entity = db.creativeProjectDao().getProjectById(projectId) ?: db.creativeProjectDao().getProjectById("active_post_draft")
        if (entity != null && entity.layersJson.isNotEmpty()) {
            return@withContext deserializePostProject(JSONObject(entity.layersJson))
        }
        null
    }

    suspend fun deleteDraft(projectId: String) = withContext(Dispatchers.IO) {
        val file = File(draftsDir, "post_$projectId.json")
        if (file.exists()) file.delete()
        AutoSaveManager.clearDraft(context, projectId)
        val db = PanalinkDatabase.getDatabase(context)
        db.creativeProjectDao().deleteProjectById("active_post_draft")
    }

    private fun serializePostProject(project: PostStudioProject): JSONObject {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("caption", project.caption)
        root.put("hashtags", JSONArray(project.hashtags))
        root.put("location", project.location ?: "")
        root.put("status", project.status.name)
        root.put("createdAtMs", project.createdAtMs)
        root.put("updatedAtMs", project.updatedAtMs)

        val pagesArr = JSONArray()
        project.pages.forEach { page ->
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            pageObj.put("pageIndex", page.pageIndex)
            pageObj.put("aspectRatio", page.aspectRatio)
            pageObj.put("backgroundColorHex", page.backgroundColorHex)

            val layersArr = JSONArray()
            page.layers.forEach { layer ->
                val layerObj = JSONObject()
                layerObj.put("id", layer.id)
                layerObj.put("xFraction", layer.xFraction.toDouble())
                layerObj.put("yFraction", layer.yFraction.toDouble())
                layerObj.put("scale", layer.scale.toDouble())
                layerObj.put("rotation", layer.rotation.toDouble())
                layerObj.put("isVisible", layer.isVisible)
                layerObj.put("isLocked", layer.isLocked)
                layerObj.put("type", layer.javaClass.simpleName)

                when (layer) {
                    is com.example.creative.core.CreativeLayer.Image -> {
                        layerObj.put("imageUriOrPath", layer.imageUriOrPath)
                        layerObj.put("filterName", layer.filterName)
                    }
                    is com.example.creative.core.CreativeLayer.Video -> {
                        layerObj.put("videoUriOrPath", layer.videoUriOrPath)
                        layerObj.put("filterName", layer.filterName)
                    }
                    is com.example.creative.core.CreativeLayer.Text -> {
                        layerObj.put("text", layer.text)
                        layerObj.put("colorHex", layer.colorHex)
                        layerObj.put("fontFamily", layer.fontFamily)
                    }
                    is com.example.creative.core.CreativeLayer.Sticker -> {
                        layerObj.put("stickerUrlOrPath", layer.stickerUrlOrPath)
                    }
                    else -> {}
                }
                layersArr.put(layerObj)
            }
            pageObj.put("layers", layersArr)
            pagesArr.put(pageObj)
        }
        root.put("pages", pagesArr)
        return root
    }

    private fun deserializePostProject(json: JSONObject): PostStudioProject {
        val pagesList = mutableListOf<PostPage>()
        val pagesArr = json.optJSONArray("pages")
        if (pagesArr != null) {
            for (i in 0 until pagesArr.length()) {
                val pageObj = pagesArr.getJSONObject(i)
                val layersList = mutableListOf<com.example.creative.core.CreativeLayer>()
                val layersArr = pageObj.optJSONArray("layers")
                if (layersArr != null) {
                    for (j in 0 until layersArr.length()) {
                        val lObj = layersArr.getJSONObject(j)
                        val id = lObj.optString("id", java.util.UUID.randomUUID().toString())
                        val x = lObj.optDouble("xFraction", 0.5).toFloat()
                        val y = lObj.optDouble("yFraction", 0.5).toFloat()
                        val scale = lObj.optDouble("scale", 1.0).toFloat()
                        val rotation = lObj.optDouble("rotation", 0.0).toFloat()
                        val type = lObj.optString("type", "")

                        when (type) {
                            "Image" -> {
                                layersList.add(
                                    com.example.creative.core.CreativeLayer.Image(
                                        id = id, xFraction = x, yFraction = y, scale = scale, rotation = rotation,
                                        imageUriOrPath = lObj.optString("imageUriOrPath"),
                                        filterName = lObj.optString("filterName", "Normal")
                                    )
                                )
                            }
                            "Video" -> {
                                layersList.add(
                                    com.example.creative.core.CreativeLayer.Video(
                                        id = id, xFraction = x, yFraction = y, scale = scale, rotation = rotation,
                                        videoUriOrPath = lObj.optString("videoUriOrPath"),
                                        filterName = lObj.optString("filterName", "Normal")
                                    )
                                )
                            }
                            "Text" -> {
                                layersList.add(
                                    com.example.creative.core.CreativeLayer.Text(
                                        id = id, xFraction = x, yFraction = y, scale = scale, rotation = rotation,
                                        text = lObj.optString("text"),
                                        colorHex = lObj.optString("colorHex", "#FFFFFF"),
                                        fontFamily = lObj.optString("fontFamily", "SansSerif")
                                    )
                                )
                            }
                            "Sticker" -> {
                                layersList.add(
                                    com.example.creative.core.CreativeLayer.Sticker(
                                        id = id, xFraction = x, yFraction = y, scale = scale, rotation = rotation,
                                        stickerUrlOrPath = lObj.optString("stickerUrlOrPath")
                                    )
                                )
                            }
                        }
                    }
                }
                pagesList.add(
                    PostPage(
                        id = pageObj.optString("id", java.util.UUID.randomUUID().toString()),
                        pageIndex = pageObj.optInt("pageIndex", i),
                        layers = layersList,
                        aspectRatio = pageObj.optString("aspectRatio", "4:5"),
                        backgroundColorHex = pageObj.optString("backgroundColorHex", "#000000")
                    )
                )
            }
        }

        val hashtagsList = mutableListOf<String>()
        val tagsArr = json.optJSONArray("hashtags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) hashtagsList.add(tagsArr.getString(i))
        }

        return PostStudioProject(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            title = json.optString("title", "Borrador de Publicación"),
            pages = if (pagesList.isNotEmpty()) pagesList else listOf(PostPage()),
            caption = json.optString("caption", ""),
            hashtags = hashtagsList,
            location = json.optString("location").ifEmpty { null },
            createdAtMs = json.optLong("createdAtMs", System.currentTimeMillis()),
            updatedAtMs = json.optLong("updatedAtMs", System.currentTimeMillis())
        )
    }
}
