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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Data model representing a comment in a channel post thread.
 */
data class ChannelCommentItem(
    val id: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val textContent: String,
    val timestampFormatted: String,
    val replyToAuthor: String? = null,
    val replyToTextSnippet: String? = null,
    val reactions: List<ReactionItem> = emptyList(),
    val isVerified: Boolean = false
)

/**
 * Individual comment bubble component (`CommentItemBubble`) for Telegram-style channel comment threads.
 * Displays user avatar, name, reply preview (if replying to another comment), message body,
 * reaction pills, timestamp, and reply action.
 */
@Composable
fun CommentItemBubble(
    comment: ChannelCommentItem,
    onReplyClick: (ChannelCommentItem) -> Unit = {},
    onReactionClick: (commentId: String, emoji: String) -> Unit = { _, _ -> },
    onMoreClick: (ChannelCommentItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleBg = Color(0xFF1E2C3A) // Telegram Dark Bubble
    val replyBoxBg = Color(0xFF17212B) // Inner Reply Preview Box
    val accentBlue = Color(0xFF2AABEE)
    val replyBorderColor = Color(0xFF64B5F6)
    val secondaryText = Color(0xFF8E959B)
    val primaryText = Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        // User Avatar
        AsyncImage(
            model = comment.senderAvatarUrl ?: "https://ui-avatars.com/api/?name=${comment.senderName}&background=2AABEE&color=fff",
            contentDescription = comment.senderName,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Comment Card Surface
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = bubbleBg,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Header: Author Name + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.senderName,
                        color = accentBlue,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = comment.timestampFormatted,
                        color = secondaryText,
                        fontSize = 11.sp
                    )
                }

                // Reply Preview Box (If replying to another comment)
                if (!comment.replyToAuthor.isNullOrEmpty() && !comment.replyToTextSnippet.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(replyBoxBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(replyBorderColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = comment.replyToAuthor,
                                color = replyBorderColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = comment.replyToTextSnippet,
                                color = secondaryText,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Comment Content
                Text(
                    text = comment.textContent,
                    color = primaryText,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                // Reaction Pills Row if present
                if (comment.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    EmojiReactionRow(
                        reactions = comment.reactions,
                        onReactionClick = { emoji -> onReactionClick(comment.id, emoji) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Actions: Reply shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReplyClick(comment) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Responder",
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Responder",
                            color = secondaryText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
