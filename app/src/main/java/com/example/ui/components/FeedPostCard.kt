package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.identity.model.toIdentityUiState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UserStateWithUser
import com.example.ui.viewmodel.StatesViewModel

fun isVideoUrl(url: String): Boolean {
    val l = url.lowercase()
    return l.endsWith(".mp4") || l.endsWith(".mov") || l.endsWith(".mkv") || l.endsWith(".webm") || l.contains("/video/") || l.contains("video_")
}

fun isAudioUrl(url: String): Boolean {
    val l = url.lowercase()
    return l.endsWith(".mp3") || l.endsWith(".m4a") || l.endsWith(".wav") || l.endsWith(".aac") || l.contains("/audio/") || l.contains("audio_")
}

@Composable
fun FeedPostCard(
    stateWithUser: UserStateWithUser,
    statesViewModel: StatesViewModel,
    onNavigateToViewState: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    val state = stateWithUser.state
    val initialProfile = stateWithUser.profile
    val context = LocalContext.current

    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(state.userId)?.toIdentityUiState())
    
    val safeAvatarUrl = identityState?.avatarUrl ?: initialProfile?.avatarUrl
    val safeDisplayName = identityState?.displayName ?: initialProfile?.displayName ?: ""
    val safeUserId = identityState?.userId ?: initialProfile?.id ?: state.userId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 6.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanaAvatar(
                    avatarUrl = safeAvatarUrl,
                    userId = safeUserId,
                    size = 40.dp,
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFFB026FF),
                    contentDescription = safeDisplayName,
                    placeholderName = safeDisplayName
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = safeDisplayName ?: "Pana de la Comunidad",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val timeStr = remember(state.createdAt) {
                        try {
                            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val date = parser.parse(state.createdAt)
                            val diff = System.currentTimeMillis() - (date?.time ?: System.currentTimeMillis())
                            val minutes = (diff / 60000).toInt()
                            when {
                                minutes < 1 -> "hace un momento"
                                minutes < 60 -> "hace ${minutes}m"
                                minutes < 1440 -> "hace ${minutes / 60}h"
                                else -> "hace ${minutes / 1440}d"
                            }
                        } catch (e: Exception) {
                            "hace poco"
                        }
                    }
                    Text(text = timeStr, color = Color.Gray, fontSize = 11.sp)
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Compartir", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                            onClick = { 
                                showMenu = false
                                onShareClick(state.id) 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copiar enlace", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                            onClick = { 
                                showMenu = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Post Link", "https://panalink.app/status/${state.id}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar publicación", color = Color(0xFFFF4D4D)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) },
                            onClick = { 
                                showMenu = false
                                statesViewModel.deleteState(state.id) {
                                    Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reportar", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray) },
                            onClick = { 
                                showMenu = false
                                Toast.makeText(context, "Publicación reportada", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Media & Content
            val youtubeVideoId = remember(state.type, state.previewMetadata, state.caption) {
                var extractedId: String? = null
                state.previewMetadata?.let { meta ->
                    try {
                        when (meta) {
                            is Map<*, *> -> extractedId = meta["video_id"]?.toString()
                            is org.json.JSONObject -> extractedId = meta.optString("video_id", null)
                            is String -> {
                                val json = org.json.JSONObject(meta)
                                extractedId = json.optString("video_id", null)
                            }
                        }
                    } catch (e: Exception) {
                        extractedId = null
                    }
                }
                if (extractedId.isNullOrBlank()) {
                    extractedId = com.example.util.YouTubeUrlParser.extractYouTubeVideoId(state.caption ?: "")
                }
                extractedId
            }

            if (!youtubeVideoId.isNullOrBlank()) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    YouTubePostCard(
                        videoId = youtubeVideoId,
                        originalText = state.caption ?: ""
                    )
                }
            } else {
                val allMediaList = (state.mediaUrls ?: listOfNotNull(state.mediaUrl)).filter { it.isNotBlank() }
                val mediaImagesAndVideos = allMediaList.filter { !isAudioUrl(it) }
                val audioUrls = allMediaList.filter { isAudioUrl(it) }.toMutableList()
                if (state.audioUrl?.isNotBlank() == true && !audioUrls.contains(state.audioUrl)) {
                    audioUrls.add(0, state.audioUrl)
                }

                if (mediaImagesAndVideos.isNotEmpty()) {
                    // ... (resto de lógica original)
                    val pagerState = rememberPagerState(pageCount = { mediaImagesAndVideos.size })
                    var isMuted by remember { mutableStateOf(true) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .background(Color(0xFF161618))
                            .clickable { onNavigateToViewState(state.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val url = mediaImagesAndVideos[page]
                            val isVideo = state.mediaType == "video" || isVideoUrl(url)

                            val resolvedResources = com.example.media.feed.PostMediaResolver.rememberResolvedMediaResources(
                                mediaUrls = listOf(url),
                                ownerId = state.userId
                            )
                            val mediaResource = resolvedResources.firstOrNull() ?: com.example.media.model.MediaResource.Remote(url)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onNavigateToViewState(state.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVideo) {
                                    val videoUri = when (mediaResource) {
                                        is com.example.media.model.MediaResource.Local -> Uri.fromFile(java.io.File(mediaResource.path))
                                        is com.example.media.model.MediaResource.Remote -> Uri.parse(mediaResource.url)
                                        else -> Uri.parse(url)
                                    }
                                    SimpleVideoPreviewPlayer(
                                        videoUri = videoUri,
                                        isMuted = isMuted,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    com.example.media.ui.MediaRenderer(
                                        resource = mediaResource,
                                        contentDescription = "Imagen",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Mute / Unmute Button for videos
                        val currentUrl = mediaImagesAndVideos.getOrNull(pagerState.currentPage) ?: ""
                        if (state.mediaType == "video" || isVideoUrl(currentUrl)) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Sonido",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Page Indicator Badges
                        if (mediaImagesAndVideos.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${mediaImagesAndVideos.size}",
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(mediaImagesAndVideos.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color(0xFF00FF85) else Color.White.copy(alpha = 0.5f)
                                    val width = if (pagerState.currentPage == iteration) 16.dp else 6.dp
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .height(6.dp)
                                            .width(width)
                                    )
                                }
                            }
                        }

                        // Background Audio indicator badge if photos have background audio
                        if (audioUrls.isNotEmpty() && state.mediaType != "video") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎵 Audio de fondo", color = Color(0xFF00FF85), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (audioUrls.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        PlaylistAudioPlayer(audioUrls = audioUrls)
                    }
                } else if (state.mediaType == "text") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF6A1B9A), Color(0xFF311B92))
                                )
                            )
                            .clickable { onNavigateToViewState(state.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.caption ?: "",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }

            // Post Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLiked = state.likedByMe ?: false
                IconButton(
                    onClick = {
                        statesViewModel.toggleLike(state.id, isLiked, onError = { err ->
                            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                        })
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (isLiked) Color(0xFFFF1744) else Color.White,
                        contentDescription = "Me gusta"
                    )
                }

                IconButton(
                    onClick = { onCommentClick(state.id) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        tint = Color.White,
                        contentDescription = "Comentarios"
                    )
                }

                IconButton(
                    onClick = { onShareClick(state.id) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        tint = Color.White,
                        contentDescription = "Compartir"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val isFavorited = state.favoritedByMe ?: false
                IconButton(
                    onClick = { statesViewModel.toggleFavorite(state.id, isFavorited) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.BookmarkBorder else Icons.Outlined.BookmarkBorder,
                        tint = if (isFavorited) Color(0xFFD500F9) else Color.White,
                        contentDescription = "Guardar"
                    )
                }
            }

            // Likes Count
            val likesCount = state.likesCount ?: 0
            if (likesCount > 0) {
                Text(
                    text = "$likesCount Me gusta",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Caption for Images/Videos
            if (state.mediaType != "text" && !state.caption.isNullOrBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = safeDisplayName ?: "",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.caption,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // View all comments
            val commentsCount = state.commentsCount ?: 0
            if (commentsCount > 0) {
                Text(
                    text = "Ver los $commentsCount comentarios",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onCommentClick(state.id) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
