package com.example.ui.components.chat.media

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PanaAvatar

@Composable
fun PremiumVoicePlayer(
    audioUrl: String,
    isPlaying: Boolean,
    progress: Float,
    durationLabel: String,
    senderAvatarUrl: String?,
    isSender: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit = {},
    isLoading: Boolean = false,
    isError: Boolean = false,
    messageStatus: String? = "sent",
    isSending: Boolean = false,
    isVoiceNote: Boolean = true,
    modifier: Modifier = Modifier
) {
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    
    val barCount = 35
    val amplitudes = remember(audioUrl) {
        val seed = audioUrl.hashCode().toLong()
        val random = java.util.Random(seed)
        var last = 0.5f
        List(barCount) { 
            val change = (random.nextFloat() - 0.5f) * 0.4f
            last = (last + change).coerceIn(0.15f, 0.95f)
            last
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val currentProgress = if (isDragging) dragProgress else progress

    val pulseTransition = rememberInfiniteTransition(label = "WavePulse")
    val pulseAlpha by if (isPlaying) {
        pulseTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val effectiveIsSending = isSending || messageStatus == "sending" || messageStatus == "pending" || messageStatus == "pending_media"
    val isFailed = messageStatus == "failed"
    val bubbleBgColor = if (isSender) Color(0xFFE7FFDB) else Color(0xFFFFFFFF)
    val playedColor = if (isSender) Color(0xFF1EBE71) else Color(0xFF00A3DA)
    val unplayedColor = Color(0xFF8696A0).copy(alpha = 0.2f)
    val secondaryText = Color(0xFF667781)

    val waveTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val waveOffset by if (isPlaying) {
        waveTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WaveOffset"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .widthIn(max = 300.dp),
        color = bubbleBgColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PanaAvatar - Now functional and elegant
            Box(modifier = Modifier.padding(end = 4.dp)) {
                PanaAvatar(
                    avatarUrl = senderAvatarUrl,
                    modifier = Modifier.size(38.dp),
                    size = 38.dp,
                    borderWidth = 1.5.dp,
                    borderColor = playedColor.copy(alpha = 0.3f)
                )
                
                // Mic icon to distinguish Voice Note from regular Audio
                if (isVoiceNote) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(playedColor, CircleShape)
                            .border(1.2.dp, bubbleBgColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = bubbleBgColor,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            // Play/Pause button with refined glass look
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading && !effectiveIsSending) { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                if (effectiveIsSending || isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = playedColor,
                        strokeWidth = 2.dp,
                        trackColor = Color.Transparent
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(playedColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isFailed -> Icons.Default.Error
                                isError -> Icons.Default.Refresh
                                isPlaying -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (isFailed) Color.Red else playedColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Waveform rendering
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .pointerInput(audioUrl) {
                            detectTapGestures { offset ->
                                val w = size.width
                                if (w > 0) onSeek((offset.x / w).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(audioUrl) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, _ ->
                                    val w = size.width
                                    if (w > 0) {
                                        dragProgress = (change.position.x / w).coerceIn(0f, 1f)
                                        onSeek(dragProgress)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = 2.dp.toPx()
                        val spacing = 1.8.dp.toPx()
                        val totalW = barWidth + spacing
                        val count = (size.width / totalW).toInt().coerceAtMost(barCount)
                        val midY = size.height / 2
                        
                        for (i in 0 until count) {
                            val baseAmp = amplitudes.getOrElse(i) { 0.3f }
                            val phase = if (isPlaying) (waveOffset * 2 * Math.PI).toFloat() else 0f
                            val dynAmp = if (isPlaying) {
                                (baseAmp * (0.75f + 0.25f * Math.sin((i * 0.6 + phase).toDouble()).toFloat())).coerceIn(0.15f, 1.2f)
                            } else baseAmp
                            
                            val h = (size.height * 0.85f) * dynAmp
                            val x = i * totalW + (size.width - (count * totalW)) / 2
                            val barProgress = i.toFloat() / count.toFloat()
                            val color = if (barProgress <= currentProgress) playedColor.copy(alpha = pulseAlpha) else unplayedColor
                            
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, midY - h / 2),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                }
                
                // Small progress bar for uploading - "Minucioso"
                if (effectiveIsSending) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .clip(CircleShape),
                        color = playedColor,
                        trackColor = playedColor.copy(alpha = 0.1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isFailed -> "Error de envío"
                            effectiveIsSending -> "Subiendo..."
                            isError -> "Error de descarga"
                            else -> durationLabel
                        },
                        color = if (isFailed) Color.Red else secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Speed Selector
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                playbackSpeed = when (playbackSpeed) {
                                    1f -> 1.5f
                                    1.5f -> 2f
                                    else -> 1f
                                }
                                onSpeedChange(playbackSpeed)
                            },
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt() else playbackSpeed}x",
                            color = secondaryText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
