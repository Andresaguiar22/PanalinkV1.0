package com.example.ui.components.chat.bubble

import android.content.Context
import com.example.ui.components.chat.emoji.AnimatedEmojiEngine
import com.example.ui.components.chat.emoji.EmojiHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import com.example.ui.components.chat.interaction.MessageInteractionState
import com.example.ui.components.chat.interaction.rememberSwipeReplyState
import com.example.ui.components.chat.interaction.swipeReplyGesture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseClient
import com.example.data.model.Message
import com.example.data.model.CallLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.repository.CdnManager
import com.example.data.model.formatIsoDateTime
import com.example.util.AudioPlayer
import com.example.ui.theme.LocalAppColors
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleEngine(
    message: Message,
    isMe: Boolean,
    groupPosition: MessageGroupPosition,
    myAvatarUrl: String?,
    otherAvatarUrl: String?,
    textSizeSp: Float = 15f,
    allMessages: List<Message>,
    onReply: (Message) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
    onForward: (Message) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onImageClick: (String) -> Unit,
    playingAudioUrl: String?,
    isAudioPlaying: Boolean,
    audioProgress: Float,
    audioDurationMs: Int,
    audioCurrentPositionMs: Int,
    audioPlayer: AudioPlayer,
    onAudioPlayStateChange: (String?, Boolean) -> Unit,
    reactions: Map<String, String>?,
    onReact: (String) -> Unit,
    onToggleFavorite: (Message) -> Unit,
    onSaveSticker: ((String) -> Unit)? = null,
    onToggleStickerFavorite: ((String) -> Unit)? = null,
    isEdited: Boolean = false,
    onEdit: ((Message) -> Unit)? = null,
    isHighlighted: Boolean = false,
    isChannel: Boolean = false,
    onOpenComments: ((String) -> Unit)? = null,
    onGhostOpen: (Message) -> Unit = {},
    onPlaylistAction: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (message.isGhost) {
        GhostMessageBubble(
            message = message,
            isMe = isMe,
            onOpen = { onGhostOpen(message) },
            modifier = modifier
        )
        return
    }

    if (message.messageType == "playlist") {
        val payload = remember(message.textContent) {
            try {
                Json.decodeFromString<com.example.media.playlist.PlaylistSharePayload>(message.textContent)
            } catch (e: Exception) {
                null
            }
        }
        if (payload != null) {
            val formattedTime = formatIsoDateTime(message.createdAt)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                PlaylistChatBubble(
                    payload = payload,
                    isMe = isMe,
                    formattedTime = formattedTime,
                    onOpen = { onPlaylistAction(payload.playlistId, "OPEN") },
                    onPlay = { onPlaylistAction(payload.playlistId, "PLAY") },
                    onSave = { onPlaylistAction(payload.playlistId, "SAVE") }
                )
            }
            return
        }
    }

    if (isChannel) {
        // Detect if content has forwarded source
        val forwardedFrom = if (message.textContent.contains("Reenviado de", ignoreCase = true) || message.textContent.contains("Forwarded from", ignoreCase = true)) {
            val lines = message.textContent.lines()
            lines.firstOrNull { it.contains("Reenviado", ignoreCase = true) || it.contains("Forwarded", ignoreCase = true) }?.replace("Reenviado de ", "")?.replace("Forwarded from ", "")
        } else null

        // Detect if message has an APK or downloadable file
        val fileInfo = if (message.messageType == "document" || message.messageType?.startsWith("application/") == true || message.messageType?.startsWith("text/") == true) {
            com.example.ui.components.chat.ChannelFileAttachment(
                fileName = message.mediaUrl?.substringAfterLast('/') ?: "Archivo adjunto",
                fileSizeFormatted = (message.mediaSize?.div(1024)?.toString() ?: "0") + " KB",
                fileExtension = message.mediaUrl?.substringAfterLast('.')?.uppercase() ?: "FILE",
                description = message.textContent,
                fileUrl = message.mediaUrl
            )
        } else null

        // Format timestamp
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        val timeStr = try {
            val parseFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = parseFormat.parse(message.createdAt.substringBefore("."))
            if (date != null) timeFormat.format(date) else ""
        } catch (e: Exception) { "" }

        // Format reactions
        val reactionItems = reactions?.map { (emoji, _) ->
            com.example.ui.components.chat.ReactionItem(
                emoji = emoji,
                count = 1,
                isUserReacted = true
            )
        } ?: emptyList()

        com.example.ui.components.chat.ChannelPostBubble(
            postText = if (fileInfo != null) "" else message.textContent,
            authorName = if (forwardedFrom == null) (if (isMe) null else otherAvatarUrl?.let { "Admin" }) else null,
            forwardedFromChannel = forwardedFrom,
            imageUrl = if (message.messageType == "image" || message.messageType?.startsWith("image/") == true) message.mediaUrl else null,
            fileAttachment = fileInfo,
            reactions = reactionItems,
            viewsCountFormatted = "1.2K", // Simulated for now
            timestampFormatted = timeStr,
            commentsEnabled = true,
            commentsCountFormatted = null, // Can add if we have count
            onReactionClick = onReact,
            onAddReactionClick = { /* No-op for now */ },
            onFileDownloadClick = { /* Can implement actual download via utility */ },
            onCommentsClick = { onOpenComments?.invoke(message.id) },
            onQuickShareClick = { onForward(message) },
            onImageClick = onImageClick,
            modifier = modifier
        )
        return
    }

    val colors = LocalAppColors.current
    val isSticker = message.textContent.startsWith("[Sticker] ") || 
                    message.messageType?.lowercase() == "sticker" || 
                    message.messageType?.lowercase() == "gif" || 
                    message.textContent.startsWith("[GIF]")
    val bubbleShape = if (isSticker) {
        RoundedCornerShape(0.dp)
    } else {
        BubbleShapeFactory.createShape(groupPosition, isMe)
    }
    val isOnlyEmoji = remember(message.textContent) {
        val trimmed = message.textContent.trim()
        trimmed.isNotEmpty() && trimmed.all { it.isHighSurrogate() || it.isLowSurrogate() || it.isSurrogate() || it.code <= 0x7F || it.isWhitespace() } && 
        trimmed.count { it.isHighSurrogate() } in 1..3 && trimmed.length <= 10
    }
    // Better emoji check (simplified for this context)
    val isBigEmoji = remember(message.textContent) {
        EmojiHelper.isEmojiOnly(message.textContent)
    }

    val bubbleColor = if (isSticker || isBigEmoji) Color.Transparent 
                      else if (isMe) Color(0xFFD9FDD3) 
                      else Color(0xFFFFFFFF)
    val contentTextColor = Color(0xFF111B21)
    val elevation = if (isSticker || isBigEmoji) 0f else 1f

    var showMenu by remember { mutableStateOf(false) }

    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected || showMenu) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubbleSelectionScale"
    )

    val selectionElevation by animateFloatAsState(
        targetValue = if (isSelected || showMenu) 6f else elevation,
        animationSpec = spring(),
        label = "bubbleSelectionElevation"
    )

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val formattedTime = formatIsoDateTime(message.createdAt)
    
    val messageType = message.messageType?.lowercase() ?: ""
    val mediaType = when {
        messageType == "image" || messageType.startsWith("image/") -> "CHAT_IMAGE"
        messageType == "video" || messageType.startsWith("video/") -> "CHAT_VIDEO"
        messageType == "audio" || messageType.startsWith("audio/") -> "VOICE_NOTE"
        else -> "DOCUMENT"
    }
    
    val mediaUrl = com.example.media.MediaEngineAdapter.rememberResolvedMediaUrl(
        remoteUrl = message.mediaUrl, 
        type = mediaType, 
        ownerId = message.senderId
    )
    
    val content = message.textContent.trim()
    val isMedia = messageType in listOf("audio", "image", "video", "document") ||
            (mediaUrl != null && (mediaUrl.contains(".") || mediaUrl.startsWith("http"))) ||
            content.startsWith("[")

    if (message.messageType == "call") {
        val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        val callLog = remember(message.textContent) {
            try {
                moshi.adapter(CallLog::class.java).fromJson(message.textContent)
            } catch (e: Exception) {
                null
            }
        }
        if (callLog != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                CallLogChatBubble(
                    callLog = callLog,
                    isMe = isMe,
                    formattedTime = formattedTime
                )
            }
            return
        }
    }

    val swipeState = rememberSwipeReplyState(thresholdDp = 80.dp) {
        onReply(message)
    }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE) }
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val isFavorited = message.isFavorited
    var isPinned by remember { mutableStateOf(prefs.getBoolean("pinned_${message.id}", false)) }

    val interactionState = remember(isSelected, showMenu, swipeState.isSwiping) {
        MessageInteractionState(
            isSelected = isSelected,
            isMenuOpen = showMenu,
            isSwiping = swipeState.isSwiping
        )
    }

    // Look up the message being replied to
    val repliedMsg = remember(message.replyToMessageId, allMessages) {
        allMessages.find { it.id == message.replyToMessageId }
    }

    val highlightColor = if (isSelected) colors.primary.copy(alpha = 0.15f) else if (isHighlighted) Color(0xFF00A884).copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor)
            .padding(vertical = if (isHighlighted) 4.dp else 2.dp)
            .animateContentSize()
    ) {
        // Reply sliding indicator icon behind the bubble when dragging
        if (swipeState.offset.value > 10f) {
            Text(
                text = "💬",
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.2f))
                    .padding(6.dp)
            )
        }

        val bubbleRowContent: @Composable () -> Unit = {
            // Message Bubble Content wrapper with interactive Context Menu
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = selectionScale
                    scaleY = selectionScale
                }
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        )
                        .combinedClickable(
                            onClick = { onSelect() },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        )
                        .padding(
                            horizontal = if (isSticker) 0.dp else 12.dp,
                            vertical = if (isSticker) 0.dp else 8.dp
                        )
                ) {
                    // Show Replied Message Preview inside bubble
                    if (repliedMsg != null) {
                        val replySenderName = if (repliedMsg.senderId == (SupabaseClient.currentUser?.id ?: "")) "Tú" else "Pana"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x15FFFFFF))
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(colors.primary)
                            )
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = replySenderName,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                val replyPreviewText = remember(repliedMsg) {
                                    val content = repliedMsg.textContent
                                    when {
                                        content.startsWith("[Image] ") -> "Foto 🖼️"
                                        content.startsWith("[Video] ") -> "Video 🎥"
                                        content.startsWith("[Audio] ") -> "Nota de voz 🎤"
                                        content.startsWith("[Document] ") -> "Documento 📄"
                                        content.startsWith("[Sticker] ") -> "Sticker 🏷️"
                                        else -> content
                                    }
                                }
                                Text(
                                    text = replyPreviewText,
                                    color = Color(0xFFD1D7DB),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Multimedia Bubble content rendering switcher (Phase 3.3-A)
                    val isGhostConsumed = message.isGhost && message.ghostOpenedAt != null

                    if (isGhostConsumed) {
                        ConsumedGhostMessageContent(
                            isMe = isMe,
                            formattedTime = formattedTime,
                            onToggleFavorite = { onToggleFavorite(message) },
                            isFavorited = isFavorited
                        )
                    } else {
                        if (isMedia && !isSticker) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                val rawAudioUrl = mediaUrl ?: (
                                        if (content.startsWith("[Audio] ")) content.substringAfter("[Audio] ").trim()
                                        else if (content.startsWith("[Voice] ")) content.substringAfter("[Voice] ").trim()
                                        else if (content == "[Nota de voz]") ""
                                        else content
                                        )
                                val cleanAudioUrl = if (rawAudioUrl.contains("?duration=")) {
                                    rawAudioUrl.substringBefore("?duration=")
                                } else {
                                    rawAudioUrl
                                }
                                val resolvedAudioUrl = CdnManager.resolveMediaUrlSync(cleanAudioUrl)
                                val isPlaying = playingAudioUrl == resolvedAudioUrl && isAudioPlaying && resolvedAudioUrl.isNotBlank()
                                val currentProgress = if (playingAudioUrl == resolvedAudioUrl) audioProgress else 0f
                                val durationParam = message.duration?.toInt() ?: if (rawAudioUrl.contains("?duration=")) {
                                    rawAudioUrl.substringAfter("?duration=").substringBefore("&").toIntOrNull() ?: 0
                                } else 0

                                val isCurrentlySelected = playingAudioUrl == resolvedAudioUrl
                                val durationLabel = if (isCurrentlySelected && audioCurrentPositionMs > 0) {
                                    String.format("%d:%02d", (audioCurrentPositionMs / 1000) / 60, (audioCurrentPositionMs / 1000) % 60)
                                } else if (durationParam > 0) {
                                    String.format("%d:%02d", durationParam / 60, durationParam % 60)
                                } else {
                                    "0:00"
                                }

                                com.example.ui.components.chat.media.MediaPreviewEngine(
                                    message = message,
                                    bubbleColor = bubbleColor,
                                    senderAvatarUrl = if (isMe) myAvatarUrl else otherAvatarUrl,
                                    onImageClick = onImageClick,
                                    onPlayPauseClick = {
                                        val audioUrl = resolvedAudioUrl
                                        if (audioUrl.isNotBlank()) {
                                            if (playingAudioUrl == audioUrl) {
                                                if (isAudioPlaying) {
                                                    audioPlayer.pause()
                                                } else {
                                                    audioPlayer.resume()
                                                }
                                            } else {
                                                audioPlayer.play(
                                                    url = audioUrl,
                                                    onPrepared = { },
                                                    onCompletion = { },
                                                    onError = { }
                                                )
                                            }
                                        }
                                    },
                                    onSeek = { fraction ->
                                        if (playingAudioUrl == resolvedAudioUrl) {
                                            audioPlayer.seekTo((fraction * audioPlayer.duration).toInt())
                                        }
                                    },
                                    isPlaying = isPlaying,
                                    progress = currentProgress,
                                    durationLabel = durationLabel,
                                    onSpeedChange = { speed ->
                                        if (playingAudioUrl == resolvedAudioUrl) {
                                            audioPlayer.setSpeed(speed)
                                        }
                                    }
                                )

                                // Overlay status for images/videos
                                if (messageType in listOf("image", "video")) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 8.dp, end = 8.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        MessageStatusIndicator(
                                            formattedTime = formattedTime,
                                            status = message.status,
                                            isMe = isMe,
                                            isEdited = isEdited,
                                            isFavorited = isFavorited,
                                            isPinned = isPinned,
                                            textColor = Color.White
                                        )
                                    }
                                }
                            }
                        } else if (isBigEmoji) {
                            AnimatedEmojiEngine(
                                text = message.textContent.trim(),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            MessageStatusIndicator(
                                formattedTime = formattedTime,
                                status = message.status,
                                isMe = isMe,
                                isEdited = isEdited,
                                isFavorited = isFavorited,
                                isPinned = isPinned,
                                textColor = Color(0xFF8596A0)
                            )
                        } else if (isSticker) {
                            val stickerUrl = mediaUrl ?: (if (content.startsWith("[Sticker] ")) content.substringAfter("[Sticker] ").trim() else if (content.startsWith("[GIF] ")) content.substringAfter("[GIF] ").trim() else content)
                            StickerBubbleContent(stickerUrl = stickerUrl)
                        } else {
                            val statusIndicatorComposable: @Composable () -> Unit = {
                                MessageStatusIndicator(
                                    formattedTime = formattedTime,
                                    status = message.status,
                                    isMe = isMe,
                                    isEdited = isEdited,
                                    isFavorited = isFavorited,
                                    isPinned = isPinned
                                )
                            }

                            if (message.isGhost) {
                                GhostMessageContent(
                                    isMe = isMe,
                                    formattedTime = formattedTime,
                                    statusIndicator = statusIndicatorComposable,
                                    onClick = { onGhostOpen(message) }
                                )
                            } else {
                                TextBubbleContent(
                                    text = message.textContent,
                                    textSizeSp = textSizeSp,
                                    textColor = contentTextColor,
                                    statusIndicator = statusIndicatorComposable
                                )
                            }
                        }
                    }

                // Reactions Display
                    if (!reactions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val reactionCounts = reactions.values.groupingBy { it }.eachCount()
                            reactionCounts.forEach { (emoji, count) ->
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                                    color = Color(0xFF3B4A54),
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(text = emoji, fontSize = 12.sp)
                                        if (count > 1) {
                                            Text(text = count.toString(), color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Status Indicator under content if NOT media (images/videos)
                    if (isMedia && messageType !in listOf("image", "video")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        MessageStatusIndicator(
                            formattedTime = formattedTime,
                            status = message.status,
                            isMe = isMe,
                            isEdited = isEdited,
                            isFavorited = isFavorited,
                            isPinned = isPinned,
                            textColor = Color(0xFF667781),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // Channel Comments Link
                    if (isChannel) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color(0xFF374248).copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Open comments */ }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF0088CC),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "20 comentarios", // Mock count
                                    color = Color(0xFF0088CC),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Overlapping reactions pill
                if (!reactions.isNullOrEmpty()) {
                    val uniqueEmojis = reactions.values.distinct().take(3).joinToString("")
                    val count = reactions.size
                    Box(
                        modifier = Modifier
                            .align(if (isMe) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(
                                x = if (isMe) 14.dp else (-14).dp,
                                y = 8.dp
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F2C34))
                            .border(1.dp, Color(0xFF2A3942), RoundedCornerShape(10.dp))
                            .clickable {
                                val myReaction = reactions[SupabaseClient.currentUser?.id ?: ""]
                                if (myReaction != null) {
                                    onReact(myReaction)
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = uniqueEmojis, fontSize = 11.sp)
                            if (count > 1) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$count",
                                    color = Color(0xFF8596A0),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Context Menu Items Definition
                val menuItems = remember(isMe, isFavorited, isPinned, message) {
                    val isStickerMsg = message.messageType == "sticker" || 
                                       message.messageType == "gif" || 
                                       message.messageType == "image/webp" || 
                                       message.textContent.startsWith("[Sticker] ") || 
                                       message.textContent.startsWith("[GIF]")
                    val stickerUrl = if (isStickerMsg) (message.mediaUrl ?: (if (message.textContent.startsWith("[Sticker] ")) message.textContent.substringAfter("[Sticker] ").trim() else if (message.textContent.startsWith("[GIF] ")) message.textContent.substringAfter("[GIF] ").trim() else message.textContent)) else null
                    buildList {
                        if (isStickerMsg && stickerUrl != null) {
                            add(PremiumMenuItemData(id = "save_sticker", title = "Guardar sticker", iconEmoji = "💾", onClick = { onSaveSticker?.invoke(stickerUrl) }))
                            add(PremiumMenuItemData(id = "star_sticker", title = "Favorito (Sticker)", iconEmoji = "⭐", onClick = { onToggleStickerFavorite?.invoke(stickerUrl) }))
                        }
                        add(
                            PremiumMenuItemData(
                                id = "reply",
                                title = "Responder",
                                iconEmoji = "💬",
                                onClick = { onReply(message) }
                            )
                        )
                        if (isMe && !message.textContent.startsWith("[")) {
                            add(
                                PremiumMenuItemData(
                                    id = "edit",
                                    title = "Editar mensaje",
                                    iconEmoji = "✏️",
                                    onClick = { onEdit?.invoke(message) }
                                )
                            )
                        }
                        add(
                            PremiumMenuItemData(
                                id = "copy",
                                title = "Copiar texto",
                                iconEmoji = "📋",
                                onClick = { clipboardManager.setText(AnnotatedString(message.textContent)) }
                            )
                        )
                        add(
                            PremiumMenuItemData(
                                id = "forward",
                                title = "Reenviar",
                                iconEmoji = "↗️",
                                onClick = { onForward(message) }
                            )
                        )
                        add(
                            PremiumMenuItemData(
                                id = "star",
                                title = if (isFavorited) "Quitar de favoritos" else "Agregar a favoritos",
                                iconEmoji = if (isFavorited) "🌟" else "⭐",
                                onClick = { onToggleFavorite(message) }
                            )
                        )
                        add(
                            PremiumMenuItemData(
                                id = "pin",
                                title = if (isPinned) "Desfijar de chat" else "Fijar en chat",
                                iconEmoji = "📌",
                                onClick = {
                                    val newState = !isPinned
                                    isPinned = newState
                                    prefs.edit().putBoolean("pinned_${message.id}", newState).apply()
                                }
                            )
                        )
                        add(
                            PremiumMenuItemData(
                                id = "delete_me",
                                title = "Eliminar para mí",
                                iconEmoji = "🗑️",
                                isDestructive = true,
                                onClick = { onDeleteForMe(message.id) }
                            )
                        )
                        if (isMe) {
                            add(
                                PremiumMenuItemData(
                                    id = "delete_everyone",
                                    title = "Eliminar para todos",
                                    iconEmoji = "⚠️",
                                    isDestructive = true,
                                    onClick = { onDeleteForEveryone(message.id) }
                                )
                            )
                        }
                    }
                }

                // Premium Context Menu Replacement
                PremiumMessageMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    onSelectReaction = { emoji ->
                        onReact(emoji)
                    },
                    menuItems = menuItems,
                    isMe = isMe
                )
            }
        }

        val swipeModifier = Modifier
            .offset { IntOffset(swipeState.offset.value.toInt(), 0) }
            .swipeReplyGesture(swipeState)

        if (isMe) {
            OutgoingBubbleContainer(
                groupPosition = groupPosition,
                shape = bubbleShape,
                containerColor = bubbleColor,
                tonalElevation = selectionElevation,
                modifier = swipeModifier
            ) {
                bubbleRowContent()
            }
        } else {
            IncomingBubbleContainer(
                groupPosition = groupPosition,
                avatarUrl = otherAvatarUrl,
                shape = bubbleShape,
                containerColor = bubbleColor,
                tonalElevation = selectionElevation,
                modifier = swipeModifier
            ) {
                bubbleRowContent()
            }
        }
    }
}

@Composable
private fun GhostMessageContent(
    isMe: Boolean,
    formattedTime: String,
    statusIndicator: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF00A884)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mensaje de ver una vez",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111B21)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        statusIndicator()
    }
}

@Composable
private fun ConsumedGhostMessageContent(
    isMe: Boolean,
    formattedTime: String,
    onToggleFavorite: () -> Unit,
    isFavorited: Boolean
) {
    Row(
        modifier = Modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mensaje consumido",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontStyle = FontStyle.Italic
        )
    }
}
