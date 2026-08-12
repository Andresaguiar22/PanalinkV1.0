package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PostDto
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "local_posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val type: String?,
    val content: String?,
    val mediaUrlsJson: String?, // JSON array for all mediaUrls
    val audioUrl: String?,
    val privacy: String?,
    val likesCount: Int,
    val commentsCount: Int,
    val shareCount: Int = 0,
    val currentUserLiked: Boolean,
    val visibility: String? = "PUBLIC",
    val deletedAt: String? = null,
    val createdAt: String?,
    val updatedAt: String? = null,
    val syncedAt: Long = System.currentTimeMillis(),
    val previewMetadataJson: String? = null,
    val customMediaIdsJson: String? = null
) {
    fun toPostDto(): PostDto {
        val mediaUrls = mediaUrlsJson?.let {
            try {
                val array = JSONArray(it)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val customMediaIds = customMediaIdsJson?.let {
            try {
                val array = JSONArray(it)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (e: Exception) {
                null
            }
        }

        val previewMetadata = previewMetadataJson?.let {
            try {
                val obj = JSONObject(it)
                val map = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.getString(key)
                }
                map
            } catch (e: Exception) {
                null
            }
        }

        return PostDto(
            id = id,
            userId = authorId,
            type = type ?: "TEXT",
            content = content,
            mediaUrls = mediaUrls,
            audioUrl = audioUrl,
            privacy = privacy ?: "PUBLIC",
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = shareCount,
            createdAt = createdAt,
            previewMetadata = previewMetadata,
            isLikedByMe = currentUserLiked,
            customMediaIds = customMediaIds
        )
    }

    companion object {
        fun fromPostDto(dto: PostDto): PostEntity {
            val mediaUrlsJson = dto.mediaUrls?.let { list ->
                JSONArray().apply {
                    list.forEach { put(it) }
                }.toString()
            }

            val customMediaIdsJson = dto.customMediaIds?.let { list ->
                JSONArray().apply {
                    list.forEach { put(it) }
                }.toString()
            }

            val previewMetadataJson = dto.previewMetadata?.let { map ->
                JSONObject().apply {
                    map.forEach { (k, v) -> put(k, v) }
                }.toString()
            }

            return PostEntity(
                id = dto.id ?: java.util.UUID.randomUUID().toString(),
                authorId = dto.userId ?: "",
                type = dto.type,
                content = dto.content,
                mediaUrlsJson = mediaUrlsJson,
                audioUrl = dto.audioUrl,
                privacy = dto.privacy,
                likesCount = dto.likesCount,
                commentsCount = dto.commentsCount,
                shareCount = dto.sharesCount,
                currentUserLiked = dto.isLikedByMe,
                createdAt = dto.createdAt,
                previewMetadataJson = previewMetadataJson,
                customMediaIdsJson = customMediaIdsJson
            )
        }
    }
}
