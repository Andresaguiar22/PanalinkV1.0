package com.example.data.repository

import com.example.data.model.MediaPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

interface LinkPreviewRepository {
    suspend fun fetchPreview(url: String): MediaPreview?
}

class YouTubeLinkPreviewRepository : LinkPreviewRepository {
    override suspend fun fetchPreview(url: String): MediaPreview? = withContext(Dispatchers.IO) {
        try {
            // Simple OEmbed implementation for YouTube
            val oEmbedUrl = "https://www.youtube.com/oembed?url=$url&format=json"
            val response = URL(oEmbedUrl).readText()
            val json = JSONObject(response)

            val videoId = extractVideoId(url) ?: return@withContext null

            MediaPreview(
                provider = "youtube",
                videoId = videoId,
                title = json.getString("title"),
                thumbnailUrl = json.getString("thumbnail_url"),
                embedUrl = "https://www.youtube.com/embed/$videoId"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractVideoId(url: String): String? {
        // Simple regex-based extraction as a fallback
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/shorts/|/live/|/v/|/e/|watch\\?feature=player_embedded&v=|watch%3Fv%3D|watch&v=)([^#&?\\n]+)"
        val compiledPattern = pattern.toRegex()
        return compiledPattern.find(url)?.value
    }
}
