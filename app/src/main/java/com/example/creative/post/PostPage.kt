package com.example.creative.post

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType

/**
 * P6.6.2 - Post Studio Engine Core
 * Represents a single page in a multi-page carousel or single post.
 */
data class PostPage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageIndex: Int = 0,
    val layers: List<CreativeLayer> = emptyList(),
    val aspectRatio: String = "4:5", // 1:1, 4:5, 16:9
    val backgroundColorHex: String = "#000000"
) {
    fun getMainMediaLayer(): CreativeLayer? {
        return layers.firstOrNull { it is CreativeLayer.Image || it is CreativeLayer.Video }
    }
}

/**
 * Represents a complete post project with multi-page support wrapping CreativeProject.
 */
data class PostStudioProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Borrador de Publicación",
    val pages: List<PostPage> = listOf(PostPage()),
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val location: String? = null,
    val taggedUsers: List<String> = emptyList(),
    val scheduledAtMs: Long? = null,
    val status: PostProjectStatus = PostProjectStatus.DRAFT,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun toCreativeProject(): CreativeProject {
        val allLayers = pages.flatMap { it.layers }
        val mainSource = pages.firstOrNull()?.getMainMediaLayer()?.let {
            when (it) {
                is CreativeLayer.Image -> it.imageUriOrPath
                is CreativeLayer.Video -> it.videoUriOrPath
                else -> ""
            }
        } ?: ""
        return CreativeProject(
            id = id,
            sourceMedia = mainSource,
            layers = allLayers,
            createdAt = createdAtMs,
            type = CreativeType.POST
        )
    }
}

enum class PostProjectStatus {
    DRAFT,
    EDITING,
    READY_EXPORT,
    EXPORTING,
    READY_UPLOAD,
    FAILED
}
