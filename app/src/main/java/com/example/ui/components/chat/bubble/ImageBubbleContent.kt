package com.example.ui.components.chat.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ImageBubbleContent(
    imageUrl: String,
    thumbUrl: String,
    bubbleColor: Color,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val urls = if (imageUrl.contains(",")) imageUrl.split(",") else listOf(imageUrl)
    MediaMessageBubble(
        mediaUrls = urls,
        isVideo = false,
        thumbnailUrl = thumbUrl.ifEmpty { urls.firstOrNull() },
        bubbleColor = bubbleColor,
        onMediaClick = { _, selectedUrl -> onImageClick(selectedUrl) },
        modifier = modifier
    )
}
