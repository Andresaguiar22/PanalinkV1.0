package com.example.media.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.media.audio.RepeatMode

/**
 * P6.7.3 - Player Controls
 * Professional playback controls (Shuffle, Prev, Play/Pause, Next, Repeat).
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) Color(0xFF38BDF8) else Color.White
            )
        }

        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play/Pause",
                modifier = Modifier.size(40.dp)
            )
        }

        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = onToggleRepeat) {
            val icon = when (repeatMode) {
                RepeatMode.NONE -> Icons.Rounded.Repeat
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                RepeatMode.ALL -> Icons.Rounded.Repeat
            }
            Icon(
                icon,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.NONE) Color(0xFF38BDF8) else Color.White
            )
        }
    }
}
