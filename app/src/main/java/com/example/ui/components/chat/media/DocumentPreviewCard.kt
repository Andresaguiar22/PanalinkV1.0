package com.example.ui.components.chat.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.chat.bubble.DocumentMessageBubble

@Composable
fun DocumentPreviewCard(
    docUrl: String,
    fileName: String? = null,
    mediaSize: Long?,
    bubbleColor: Color = Color(0xFFE7FFDB),
    senderAvatarUrl: String? = null,
    isSender: Boolean = true,
    messageStatus: String? = "sent",
    modifier: Modifier = Modifier
) {
    DocumentMessageBubble(
        docUrl = docUrl,
        fileName = fileName,
        mediaSize = mediaSize,
        bubbleColor = bubbleColor,
        senderAvatarUrl = senderAvatarUrl,
        isSender = isSender,
        messageStatus = messageStatus,
        modifier = modifier
    )
}
