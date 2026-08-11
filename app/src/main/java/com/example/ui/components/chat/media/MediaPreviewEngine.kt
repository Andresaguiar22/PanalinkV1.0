package com.example.ui.components.chat.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.Message
import com.example.ui.components.chat.bubble.MediaMessageBubble

@Composable
fun MediaPreviewEngine(
    message: Message,
    bubbleColor: Color,
    senderAvatarUrl: String?,
    onImageClick: (String) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    durationLabel: String = "0:00",
    onSpeedChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mediaUrl = message.mediaUrl ?: return
    val thumbUrl = message.thumbnailUrl ?: ""
    val mediaType = message.messageType?.lowercase() ?: "text"

    val caption = rememberCaptionText(message.textContent)
    val isUploading = message.status == "sending" || message.status == "pending" || message.status == "pending_media"

    val isImage = mediaType == "image" || mediaType == "sticker" || mediaType == "gif" || mediaType.startsWith("image/")
    val isVideo = mediaType == "video" || mediaType.startsWith("video/")
    val isAudio = mediaType == "audio" || mediaType == "voice" || mediaType == "voice_note" || mediaType.startsWith("audio/")
    val isDocument = mediaType == "document" || mediaType.startsWith("application/") || mediaType.startsWith("text/") || mediaType.startsWith("file")

    when {
        isImage -> {
            val urls = if (mediaUrl.contains(",")) mediaUrl.split(",") else listOf(mediaUrl)
            MediaMessageBubble(
                mediaUrls = urls,
                isVideo = false,
                thumbnailUrl = thumbUrl.ifEmpty { urls.firstOrNull() },
                captionText = caption,
                bubbleColor = bubbleColor,
                isUploading = isUploading,
                onMediaClick = { _, selectedUrl -> onImageClick(selectedUrl) },
                modifier = modifier
            )
        }
        isVideo -> {
            MediaMessageBubble(
                mediaUrls = listOf(mediaUrl),
                isVideo = true,
                thumbnailUrl = thumbUrl,
                durationLabel = if (durationLabel != "0:00") durationLabel else formatVideoDuration(message.duration),
                captionText = caption,
                bubbleColor = bubbleColor,
                isUploading = isUploading,
                onMediaClick = { _, url -> onImageClick(url) },
                modifier = modifier
            )
        }
        isAudio -> {
            val isVoiceNoteMsg = mediaType == "voice" || mediaType == "voice_note" ||
                    message.textContent.contains("Voice", ignoreCase = true) ||
                    message.textContent.contains("Nota de voz", ignoreCase = true)

            PremiumVoicePlayer(
                audioUrl = mediaUrl,
                isPlaying = isPlaying,
                progress = progress,
                durationLabel = durationLabel,
                senderAvatarUrl = senderAvatarUrl,
                isSender = message.senderId == com.example.data.supabase.SupabaseClient.currentUser?.id,
                messageStatus = message.status,
                isVoiceNote = isVoiceNoteMsg,
                onPlayPauseClick = onPlayPauseClick,
                onSeek = onSeek,
                onSpeedChange = onSpeedChange,
                modifier = modifier
            )
        }
        isDocument -> {
            val docFileName = rememberDocumentFileName(message)
            DocumentPreviewCard(
                docUrl = mediaUrl,
                fileName = docFileName,
                mediaSize = message.mediaSize,
                bubbleColor = bubbleColor,
                senderAvatarUrl = senderAvatarUrl,
                isSender = message.senderId == com.example.data.supabase.SupabaseClient.currentUser?.id,
                messageStatus = message.status,
                modifier = modifier
            )
        }
    }
}

private fun rememberDocumentFileName(message: com.example.data.model.Message): String? {
    val content = message.content?.trim() ?: ""
    return when {
        content.startsWith("[Document] ") -> content.substringAfter("[Document] ").trim()
        content.startsWith("[Document]") -> content.substringAfter("[Document]").trim()
        content.isNotEmpty() && !content.startsWith("[") -> content
        else -> message.mediaUrl?.split("/")?.lastOrNull()?.substringBefore("?")
    }
}

/**
 * Filtra los marcadores de posición predeterminados como [Image], [Video], [Document] para mostrar solo el pie de foto / enlace real si existe.
 */
private fun rememberCaptionText(textContent: String): String? {
    val trimmed = textContent.trim()
    return if (trimmed.startsWith("[Image]") || trimmed.startsWith("[Video]") || trimmed.startsWith("[Document]")) {
        val remaining = trimmed.substringAfter("]").trim()
        if (remaining.isNotBlank()) remaining else null
    } else if (trimmed.isNotBlank()) {
        trimmed
    } else {
        null
    }
}

private fun formatVideoDuration(durationSec: Long?): String? {
    if (durationSec == null || durationSec <= 0) return null
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return String.format("%d:%02d", minutes, seconds)
}
