package com.example.ui.screen

import androidx.compose.runtime.saveable.rememberSaveable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PostCommentDto
import com.example.ui.viewmodel.FeedViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCommentsBottomSheet(
    postId: String,
    onDismiss: () -> Unit,
    viewModel: FeedViewModel
) {
    val context = LocalContext.current
    val commentsMap by viewModel.postComments.collectAsState()
    val comments = commentsMap[postId] ?: emptyList()
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12141A),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header TikTok / Instagram style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${comments.size} comentarios",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "≡", color = Color.Gray, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Traducción automática activada", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("文A", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Comments List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no hay comentarios. ¡Sé el primero! 💬",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                itemsIndexed(comments, key = { index, comment -> "${comment.id ?: comment.hashCode()}_$index" }) { _, comment ->
                    TikTokCommentRow(
                        comment = comment,
                        onReplyClick = { authorName ->
                            commentText = "@$authorName "
                        }
                    )
                }
            }

            // Quick Emoji Selector Bar
            AnimatedVisibility(visible = showEmojiPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color(0xFF1E222B), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf("❤️", "🔥", "😂", "🥰", "👏", "😮", "🙏", "🇻🇪")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clickable {
                                    commentText += emoji
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Bottom Input Bar TikTok Style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E222B), RoundedCornerShape(28.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Agregar comentario...", color = Color.Gray, fontSize = 14.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        maxLines = 4
                    )

                    // Action Icons inside input bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                commentText += "@"
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("@", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("😃", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank() && !isSending) {
                                    isSending = true
                                    viewModel.addComment(postId, commentText) {
                                        commentText = ""
                                        isSending = false
                                        showEmojiPicker = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (commentText.isNotBlank()) Color(0xFF00E5FF) else Color.Transparent),
                            enabled = commentText.isNotBlank() && !isSending
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = if (commentText.isNotBlank()) Color.Black else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TikTokCommentRow(
    comment: PostCommentDto,
    onReplyClick: (String) -> Unit
) {
    var isLiked by rememberSaveable { mutableStateOf(false) }
    var likeCount by rememberSaveable { mutableStateOf((2..35).random()) }
    var isDisliked by rememberSaveable { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(comment.userId ?: "").collectAsStateWithLifecycle(initialValue = null)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        com.example.ui.components.PanaAvatar(
            avatarUrl = identityState?.avatarUrl ?: comment.profile?.avatarUrl,
            userId = identityState?.userId ?: comment.profile?.id,
            size = 38.dp,
            borderWidth = 0.dp,
            placeholderName = identityState?.displayName ?: comment.profile?.displayName
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author Name
            Text(
                text = identityState?.displayName ?: comment.profile?.displayName ?: "Usuario",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Comment text
            Text(
                text = comment.content ?: "",
                color = Color.White,
                fontSize = 14.sp
            )

            if (!comment.mediaUrl.isNullOrBlank()) {
                val commentResources = com.example.media.feed.PostMediaResolver.rememberResolvedMediaResources(
                    mediaUrls = listOf(comment.mediaUrl),
                    ownerId = comment.userId
                )
                commentResources.firstOrNull()?.let { res ->
                    Spacer(modifier = Modifier.height(6.dp))
                    com.example.media.ui.MediaRenderer(
                        resource = res,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date & Reply Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = comment.createdAt?.let { formatFeedCommentDate(it) } ?: "Ahora",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Text(
                    text = "Responder",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        onReplyClick(identityState?.displayName ?: comment.profile?.displayName ?: "Usuario")
                    }
                )
            }
        }

        // Right side: Like & Dislike
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            IconButton(
                onClick = {
                    if (isLiked) {
                        isLiked = false
                        likeCount--
                    } else {
                        isLiked = true
                        likeCount++
                        if (isDisliked) isDisliked = false
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Me gusta",
                    tint = if (isLiked) Color(0xFFFF2D55) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (likeCount > 0) {
                Text(
                    text = "$likeCount",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = {
                    isDisliked = !isDisliked
                    if (isDisliked && isLiked) {
                        isLiked = false
                        likeCount--
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ThumbDown,
                    contentDescription = "No me gusta",
                    tint = if (isDisliked) Color(0xFFFF9500) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatFeedCommentDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(dateStr)
        val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
        date?.let { formatter.format(it) } ?: dateStr
    } catch (e: Exception) {
        "07-28"
    }
}
