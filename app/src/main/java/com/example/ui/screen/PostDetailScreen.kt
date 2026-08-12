package com.example.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: FeedViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val postState by viewModel.selectedPostDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.selectedPostLoading.collectAsStateWithLifecycle()
    val commentsMap by viewModel.postComments.collectAsStateWithLifecycle()
    val comments = commentsMap[postId] ?: emptyList()
    
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        viewModel.getPostDetail(postId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0E1621), // Telegram Deep Chat Dark
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Publicación",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF17212B)
                )
            )
        },
        bottomBar = {
            if (postState != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    color = Color(0xFF17212B)
                ) {
                    Column {
                        // Quick Emoji Selector Bar
                        AnimatedVisibility(visible = showEmojiPicker) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E222B))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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

                        // Bottom Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text("Escribe un comentario...", color = Color.Gray, fontSize = 14.sp)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E222B),
                                    unfocusedContainerColor = Color(0xFF1E222B),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showEmojiPicker = !showEmojiPicker },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("😃", fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF2AABEE))
            } else if (postState == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "La publicación no existe o fue eliminada.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                    ) {
                        Text("Regresar", color = Color.White)
                    }
                }
            } else {
                val post = postState!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        com.example.ui.components.FeedPostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post) },
                            onCommentClick = {
                                // Already on detail screen
                            },
                            onDeleteClick = {
                                viewModel.deletePost(post.id!!)
                                onBackClick()
                            },
                            onEditClick = { content ->
                                viewModel.updatePost(post.id!!, content)
                            }
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Comentarios (${comments.size})",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }

                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aún no hay comentarios. ¡Sé el primero! 💬",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            comments,
                            key = { index, comment -> "${comment.id ?: comment.hashCode()}_$index" }
                        ) { _, comment ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                TikTokCommentRow(
                                    comment = comment,
                                    onReplyClick = { authorName ->
                                        commentText = "@$authorName "
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
