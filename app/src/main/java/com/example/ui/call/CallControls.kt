package com.example.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.bounceClick

/**
 * CallControls displays call interactions (Mute, Speaker, Video, Flip, End) with Material 3.
 */
@Composable
fun CallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    isVideoCall: Boolean = false,
    isCameraOn: Boolean = true,
    isConnected: Boolean = true,
    onCameraToggle: () -> Unit = {},
    onCameraSwitch: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isConnected) {
            // Mute Microphone
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isMuted) Color.White else Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .size(56.dp)
                    .bounceClick(onMuteToggle)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute Microphone",
                    tint = if (isMuted) Color.Black else Color.White
                )
            }
    
            // Toggle Speaker
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isSpeakerOn) Color.White else Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .size(56.dp)
                    .bounceClick(onSpeakerToggle)
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = "Toggle Speaker",
                    tint = if (isSpeakerOn) Color.Black else Color.White
                )
            }
    
            if (isVideoCall) {
                // Toggle Video Camera
                IconButton(
                    onClick = {},
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (!isCameraOn) Color.White else Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .bounceClick(onCameraToggle)
                ) {
                    Icon(
                        imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Video",
                        tint = if (!isCameraOn) Color.Black else Color.White
                    )
                }
    
                // Switch front/back camera
                IconButton(
                    onClick = {},
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .bounceClick(onCameraSwitch)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // End Call (Always Red)
        IconButton(
            onClick = {},
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFFE53935)
            ),
            modifier = Modifier
                .size(64.dp)
                .bounceClick(onEndCall)
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
