package com.example.ui.components.chat.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun VideoBubbleContent(
    videoUrl: String,
    thumbUrl: String,
    durationLabel: String? = null,
    bubbleColor: Color = Color(0xFF1F2C34),
    onVideoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    MediaMessageBubble(
        mediaUrls = listOf(videoUrl),
        isVideo = true,
        thumbnailUrl = thumbUrl,
        durationLabel = durationLabel,
        bubbleColor = bubbleColor,
        onMediaClick = { _, url -> onVideoClick(url) },
        modifier = modifier
    )
}
