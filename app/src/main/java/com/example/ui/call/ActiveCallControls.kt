package com.example.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ActiveCallControls displays the primary interactive bottom bar controls during an active call.
 * Uses a vertical dark gradient overlay for optimal visibility over video renderers.
 */
@Composable
fun ActiveCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    isVideoCall: Boolean = false,
    isCameraOn: Boolean = true,
    onCameraToggle: () -> Unit = {},
    onCameraSwitch: () -> Unit = {},
    onMoreOptionSelected: (String) -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Microphone Toggle
            CallActionButton(
                onClick = onMuteToggle,
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Toggle Mute",
                containerColor = if (isMuted) Color.White else Color.White.copy(alpha = 0.15f),
                contentColor = if (isMuted) Color.Black else Color.White,
                label = if (isMuted) "Silenciado" else "Silenciar",
                testTag = "mute_button"
            )

            // 2. Speaker Output Toggle
            CallActionButton(
                onClick = onSpeakerToggle,
                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                contentDescription = "Toggle Speaker",
                containerColor = if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.15f),
                contentColor = if (isSpeakerOn) Color.Black else Color.White,
                label = if (isSpeakerOn) "Altavoz" else "Auricular",
                testTag = "speaker_button"
            )

            if (isVideoCall) {
                // 3. Camera Toggle (Video only)
                CallActionButton(
                    onClick = onCameraToggle,
                    icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Camera",
                    containerColor = if (isCameraOn) Color.White.copy(alpha = 0.15f) else Color.White,
                    contentColor = if (isCameraOn) Color.White else Color.Black,
                    label = if (isCameraOn) "Cámara" else "Sin Cámara",
                    testTag = "camera_button"
                )

                // 4. Switch Front/Rear Camera (Video only)
                CallActionButton(
                    onClick = onCameraSwitch,
                    icon = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera",
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    label = "Girar",
                    testTag = "switch_camera_button"
                )
            } else {
                // 3. More Menu (Audio only)
                Box {
                    CallActionButton(
                        onClick = { showMoreMenu = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                        label = "Más",
                        testTag = "more_options_button"
                    )

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B)) // Slate 800
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cambiar a video", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onMoreOptionSelected("change_to_video")
                            },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Dispositivo Bluetooth", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onMoreOptionSelected("bluetooth")
                            },
                            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Enviar mensaje", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onMoreOptionSelected("send_message")
                            },
                            leadingIcon = { Icon(Icons.Default.Message, contentDescription = null, tint = Color.White) }
                        )
                    }
                }
            }

            // 5. Large Red End Call Button
            CallActionButton(
                onClick = onEndCall,
                icon = Icons.Default.CallEnd,
                contentDescription = "End Call",
                containerColor = Color(0xFFEF4444), // Red 500
                contentColor = Color.White,
                size = 64.dp,
                iconSize = 30.dp,
                label = "Colgar",
                testTag = "end_call_button"
            )
        }
    }
}
