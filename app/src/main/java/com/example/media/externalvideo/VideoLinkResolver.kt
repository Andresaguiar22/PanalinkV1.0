package com.example.media.externalvideo

/**
 * P6.7 - Video Link Resolver
 * Analyzes video URLs, extracts thumbnails, video titles, and returns structured ExternalMediaObject.
 */
object VideoLinkResolver {

    fun resolveUrl(url: String): ExternalMediaObject {
        val platform = PlatformDetector.detectPlatform(url)

        val (title, thumbnail) = when (platform) {
            PlatformType.YOUTUBE -> {
                val videoId = extractYouTubeVideoId(url)
                Pair(
                    "Video de YouTube (${videoId.take(6)})",
                    if (videoId.isNotEmpty()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null
                )
            }
            PlatformType.INSTAGRAM -> Pair("Reel / Post de Instagram", "https://picsum.photos/400/600")
            PlatformType.TIKTOK -> Pair("Video de TikTok", "https://picsum.photos/400/600")
            PlatformType.FACEBOOK -> Pair("Video de Facebook", "https://picsum.photos/400/300")
            PlatformType.TWITTER -> Pair("Video de X (Twitter)", "https://picsum.photos/400/300")
            PlatformType.OTHER -> Pair("Video Web Externo", "https://picsum.photos/400/300")
        }

        return ExternalMediaObject(
            url = url,
            platform = platform,
            title = title,
            thumbnail = thumbnail,
            author = "Creador Externo",
            embedSupported = platform == PlatformType.YOUTUBE || platform == PlatformType.OTHER
        )
    }

    private fun extractYouTubeVideoId(url: String): String {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> ""
        }
    }
}
