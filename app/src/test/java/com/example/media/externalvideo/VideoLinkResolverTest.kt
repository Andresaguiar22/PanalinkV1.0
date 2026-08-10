package com.example.media.externalvideo

import org.junit.Assert.*
import org.junit.Test

class VideoLinkResolverTest {

    @Test
    fun testYouTubeResolver() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val mediaObj = VideoLinkResolver.resolveUrl(url)

        assertEquals(PlatformType.YOUTUBE, mediaObj.platform)
        assertTrue(mediaObj.thumbnail?.contains("dQw4w9WgXcQ") == true)
        assertTrue(mediaObj.embedSupported)
    }

    @Test
    fun testInstagramResolver() {
        val url = "https://www.instagram.com/reel/C123456789/"
        val mediaObj = VideoLinkResolver.resolveUrl(url)

        assertEquals(PlatformType.INSTAGRAM, mediaObj.platform)
        assertNotNull(mediaObj.thumbnail)
    }

    @Test
    fun testTikTokResolver() {
        val url = "https://www.tiktok.com/@user/video/7123456789"
        val mediaObj = VideoLinkResolver.resolveUrl(url)

        assertEquals(PlatformType.TIKTOK, mediaObj.platform)
    }
}
