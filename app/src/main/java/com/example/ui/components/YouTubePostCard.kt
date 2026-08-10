package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.data.repository.YouTubeRepository
import com.example.data.repository.OEmbedResponse
import kotlinx.coroutines.launch

@Composable
fun YouTubePostCard(
    videoId: String,
    originalText: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf<OEmbedResponse?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(videoId) {
        scope.launch {
            val data = YouTubeRepository.getVideoMetadata(videoId)
            metadata = data
        }
    }

    val cleanText = remember(originalText, videoId) {
        val parser = com.example.util.YouTubeUrlParser
        val url = parser.extractYouTubeUrl(originalText)
        if (url != null) originalText.replace(url, "").trim() else originalText
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (cleanText.isNotBlank()) {
            Text(
                text = cleanText,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (isPlaying) {
                YouTubePlayerComposable(
                    videoId = videoId,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val thumb = metadata?.thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                AsyncImage(
                    model = thumb,
                    contentDescription = metadata?.title ?: "Video de YouTube",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isPlaying = true }
                )
                
                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .clickable { isPlaying = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Title Overlay if metadata is available
                if (metadata != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = metadata?.title ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = metadata?.authorName ?: "YouTube",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
