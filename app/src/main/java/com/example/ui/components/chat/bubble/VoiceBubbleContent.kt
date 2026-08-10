package com.example.ui.components.chat.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.VoiceMessageBubble

@Composable
fun VoiceBubbleContent(
    audioUrl: String,
    isPlaying: Boolean,
    progress: Float,
    durationLabel: String,
    timestamp: String,
    isSender: Boolean,
    senderAvatarUrl: String?,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.chat.media.PremiumVoicePlayer(
        audioUrl = audioUrl,
        isPlaying = isPlaying,
        progress = progress,
        durationLabel = durationLabel,
        senderAvatarUrl = senderAvatarUrl,
        isSender = isSender,
        onPlayPauseClick = onPlayPauseClick,
        onSeek = onSeek,
        modifier = modifier
    )
}
