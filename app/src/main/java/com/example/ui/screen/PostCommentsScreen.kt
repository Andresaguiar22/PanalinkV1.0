package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.chat.ChannelCommentItem
import com.example.ui.components.chat.CommentInputBar
import com.example.ui.components.chat.CommentItemBubble
import com.example.ui.components.chat.ReactionItem

/**
 * Original Channel Post Preview model for the pinned top header in PostCommentsScreen.
 */
data class OriginalPostPreview(
    val postId: String,
    val channelName: String,
    val channelAvatarUrl: String? = null,
    val postTextExcerpt: String,
    val imageUrl: String? = null,
    val timestampFormatted: String = "10:50 AM"
)

/**
 * Screen displaying the discussion thread/comments for a specific Channel Post (`PostCommentsScreen`).
 * Features:
 * - Pinned preview card of the original channel post.
 * - Threaded comments list with reply-to previews and interactive reactions.
 * - Membership wall (`CommentInputBar`) that restricts non-subscribers with an "Unirme al canal" CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsScreen(
    postId: String = "post_1",
    originalPost: OriginalPostPreview = OriginalPostPreview(
        postId = postId,
        channelName = "PanaLink Oficial",
        postTextExcerpt = "🚀 ¡Nueva actualización v2.4 lanzada! Descarga la APK y disfruta de los canales tipo Telegram.",
        imageUrl = "https://picsum.photos/400/200"
    ),
    initialIsSubscribed: Boolean = false,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val barBg = Color(0xFF17212B)
    val screenBg = Color(0xFF0E1621) // Telegram Deep Chat Dark
    val pinnedHeaderBg = Color(0xFF1E2C3A)
    val accentBlue = Color(0xFF2AABEE)
    val secondaryText = Color(0xFF8E959B)

    var isSubscribed by remember { mutableStateOf(initialIsSubscribed) }
    var replyingToComment by remember { mutableStateOf<ChannelCommentItem?>(null) }

    // Sample comments state list
    val comments = remember {
        mutableStateListOf(
            ChannelCommentItem(
                id = "c1",
                senderName = "Carlos Mendoza",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=11",
                textContent = "¡Excelente actualización! Las reacciones funcionan super fluido 🔥",
                timestampFormatted = "10:52 AM",
                reactions = listOf(
                    ReactionItem("🔥", 8, true),
                    ReactionItem("👍", 12, false)
                )
            ),
            ChannelCommentItem(
                id = "c2",
                senderName = "Sofía Rodríguez",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=5",
                textContent = "¿Saben si esta versión corregirá el lag en la descarga de APKs grandes?",
                timestampFormatted = "10:55 AM",
                reactions = listOf(
                    ReactionItem("❤️", 3, false)
                )
            ),
            ChannelCommentItem(
                id = "c3",
                senderName = "Dev Pana",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=60",
                textContent = "Sí Sofía, incluye la optimización con WorkManager en segundo plano.",
                timestampFormatted = "11:01 AM",
                replyToAuthor = "Sofía Rodríguez",
                replyToTextSnippet = "¿Saben si esta versión corregirá el lag...",
                reactions = listOf(
                    ReactionItem("🙌", 5, true)
                )
            )
        )
    }

    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Comentarios",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${comments.size} respuestas",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barBg
                )
            )
        },
        bottomBar = {
            CommentInputBar(
                isSubscribed = isSubscribed,
                replyingToComment = replyingToComment,
                onSendMessage = { newText ->
                    val newComment = ChannelCommentItem(
                        id = "c_${System.currentTimeMillis()}",
                        senderName = "Tú",
                        textContent = newText,
                        timestampFormatted = "Ahora",
                        replyToAuthor = replyingToComment?.senderName,
                        replyToTextSnippet = replyingToComment?.textContent
                    )
                    comments.add(newComment)
                    replyingToComment = null
                },
                onJoinChannelClick = {
                    // Instantly subscribe the user and unveil the comment box
                    isSubscribed = true
                },
                onCancelReplyClick = {
                    replyingToComment = null
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Pinned Header (Original Post Preview Card)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = pinnedHeaderBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Publicación fijada",
                        tint = accentBlue,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = originalPost.channelName,
                            color = accentBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = originalPost.postTextExcerpt,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!originalPost.imageUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AsyncImage(
                            model = originalPost.imageUrl,
                            contentDescription = "Post Thumbnail",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 2. Threaded Comments List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(comments, key = { index, item -> "${item.id}_$index" }) { _, item ->
                    CommentItemBubble(
                        comment = item,
                        onReplyClick = { clickedComment ->
                            replyingToComment = clickedComment
                        },
                        onReactionClick = { commentId, clickedEmoji ->
                            val index = comments.indexOfFirst { it.id == commentId }
                            if (index != -1) {
                                val target = comments[index]
                                val updatedReactions = target.reactions.map { r ->
                                    if (r.emoji == clickedEmoji) {
                                        r.copy(
                                            count = if (r.isUserReacted) r.count - 1 else r.count + 1,
                                            isUserReacted = !r.isUserReacted
                                        )
                                    } else r
                                }
                                comments[index] = target.copy(reactions = updatedReactions)
                            }
                        }
                    )
                }
            }
        }
    }
}
