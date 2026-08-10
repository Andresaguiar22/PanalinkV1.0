package com.example.media.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.media.audio.AudioTrackEntity

/**
 * P6.7.3 - Player Artwork
 * Renders the album cover with a professional, shadow-enhanced design.
 */
@Composable
fun PlayerArtwork(
    track: AudioTrackEntity?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(24.dp)
            .aspectRatio(1f)
            .shadow(20.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = track?.coverUri ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=1000",
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
