package com.example.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * File attachment model for Channel posts (`.apk`, `.zip`, `.docx`, `.pdf`, etc.)
 */
data class ChannelFileAttachment(
    val fileName: String,
    val fileSizeFormatted: String, // e.g. "121,3 MB APK"
    val fileExtension: String? = "APK",
    val description: String? = null,
    val fileUrl: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

/**
 * Telegram-style Channel Post Card (`ChannelPostBubble`).
 * Supports forwarded headers, image banners, file/APK attachment cards, formatted rich text,
 * interactive emoji reaction pills, view count, timestamp footer, and a quick share button.
 */
@Composable
fun ChannelPostBubble(
    postText: String,
    authorName: String? = null,
    forwardedFromChannel: String? = null,
    imageUrl: String? = null,
    fileAttachment: ChannelFileAttachment? = null,
    reactions: List<ReactionItem> = emptyList(),
    viewsCountFormatted: String = "168.8K",
    timestampFormatted: String = "10:50 AM",
    commentsEnabled: Boolean = true,
    commentsCountFormatted: String? = "15 comentarios",
    onReactionClick: (String) -> Unit = {},
    onAddReactionClick: (() -> Unit)? = null,
    onFileDownloadClick: ((ChannelFileAttachment) -> Unit)? = null,
    onCommentsClick: (() -> Unit)? = null,
    onQuickShareClick: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleBg = Color(0xFF1E2C3A) // Telegram Post Surface Dark
    val fileCardBg = Color(0xFF17212B) // Inner Card Surface
    val accentBlue = Color(0xFF2AABEE)
    val forwardedTextColor = Color(0xFFB388FF) // Telegram Purple Header
    val secondaryText = Color(0xFF8E959B)
    val primaryText = Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Main Channel Post Card Container
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = bubbleBg,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 1. Forwarded Header (Optional)
                if (!forwardedFromChannel.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reenviado",
                            tint = forwardedTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reenviado de $forwardedFromChannel",
                            color = forwardedTextColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Author Name if present
                if (!authorName.isNullOrEmpty() && forwardedFromChannel.isNullOrEmpty()) {
                    Text(
                        text = authorName,
                        color = accentBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // 2. Image Banner (Optional)
                if (!imageUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .clickable(enabled = onImageClick != null) { onImageClick?.invoke(imageUrl) }
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Post Image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Attached File / APK Card (Optional)
                if (fileAttachment != null) {
                    ChannelFileCard(
                        file = fileAttachment,
                        backgroundColor = fileCardBg,
                        onDownloadClick = { onFileDownloadClick?.invoke(fileAttachment) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. Formatted Body Text (Bold, Links, Hashtags)
                if (postText.isNotEmpty()) {
                    Text(
                        text = formatRichPostText(postText),
                        color = primaryText,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // 5. Emoji Reactions Row
                if (reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    EmojiReactionRow(
                        reactions = reactions,
                        onReactionClick = onReactionClick,
                        onAddReactionClick = onAddReactionClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 6. Comments & Footer (Comments button on left if enabled, Views + Time on right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (commentsEnabled && !commentsCountFormatted.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF17212B))
                                .clickable(enabled = onCommentsClick != null) { onCommentsClick?.invoke() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comentarios",
                                tint = accentBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = commentsCountFormatted,
                                color = accentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "Vistas",
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = viewsCountFormatted,
                            color = secondaryText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = timestampFormatted,
                            color = secondaryText,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 7. Quick Share Button (Positioned right outside the post bubble)
        QuickShareButton(onClick = onQuickShareClick)
    }
}

/**
 * File / APK Attachment Card for Channel Messages.
 */
@Composable
private fun ChannelFileCard(
    file: ChannelFileAttachment,
    backgroundColor: Color,
    onDownloadClick: () -> Unit
) {
    val accentBlue = Color(0xFF2AABEE)
    val secondaryText = Color(0xFF8E959B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round Download Icon / Progress Indicator
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentBlue)
                .clickable { onDownloadClick() },
            contentAlignment = Alignment.Center
        ) {
            if (file.isDownloading) {
                CircularProgressIndicator(
                    progress = { file.downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Descargar archivo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = file.fileSizeFormatted,
                color = secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (!file.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.description,
                    color = Color(0xFFD0D7DE),
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Quick Share Button (`QuickShareButton`) - Small circular button next to post card.
 */
@Composable
fun QuickShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF242F3D))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Reenviar rápidamente",
            tint = Color(0xFF8E959B),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Parses post text to highlight hashtags (`#...`), URLs (`http...`), and bold text.
 */
private fun formatRichPostText(text: String): AnnotatedString {
    val linkColor = Color(0xFF64B5F6)
    val hashtagColor = Color(0xFF2AABEE)

    return buildAnnotatedString {
        val words = text.split(" ")
        for ((index, word) in words.withIndex()) {
            when {
                word.startsWith("#") && word.length > 1 -> {
                    withStyle(SpanStyle(color = hashtagColor, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                word.startsWith("http://") || word.startsWith("https://") || word.startsWith("www.") -> {
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(word)
                    }
                }
                word.startsWith("*") && word.endsWith("*") && word.length > 2 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(word.removeSurrounding("*"))
                    }
                }
                else -> {
                    append(word)
                }
            }
            if (index < words.size - 1) {
                append(" ")
            }
        }
    }
}
