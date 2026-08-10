package com.example.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.webrtc.SurfaceViewRenderer

/**
 * VideoCallScreen hosts connected video-enhanced calling.
 * Highly polished layout optimized for high frame rate, low latency WebRTC streams.
 */
@Composable
fun VideoCallScreen(
    opponentId: String? = null,
    opponentName: String,
    formattedDuration: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraOn: Boolean,
    isConnected: Boolean = true,
    viewModel: com.example.call.VideoCallViewModel,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eglContext = remember { viewModel.getEglContext() }
    
    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    // Sync renderers safely with the master session
    LaunchedEffect(localRenderer, remoteRenderer) {
        viewModel.setVideoViews(local = localRenderer, remote = remoteRenderer)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Fullscreen Remote Video View
        Box(modifier = Modifier.fillMaxSize()) {
            RemoteVideoView(
                eglContext = eglContext,
                onViewReady = { renderer ->
                    remoteRenderer = renderer
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Subtle vignette/dark overlay for visual depth and overlay readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
            )
        }

        // 2. Overlay Headers (Opponent details and call length)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = opponentName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Duration Badge
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formattedDuration,
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
                ) {
                    Text(
                        text = "Reconectando... 📡",
                        color = Color(0xFFFBBF24),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 3. Floating local PIP Preview (Top Right corner)
        if (isCameraOn) {
            Card(
                modifier = Modifier
                    .size(110.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LocalVideoView(
                        eglContext = eglContext,
                        onViewReady = { renderer ->
                            localRenderer = renderer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 4. Floating Overlay Call Controls (Bottom Center)
        ActiveCallControls(
            isMuted = isMuted,
            isSpeakerOn = isSpeakerOn,
            onMuteToggle = onMuteToggle,
            onSpeakerToggle = onSpeakerToggle,
            onEndCall = onEndCall,
            isVideoCall = true,
            isCameraOn = isCameraOn,
            onCameraToggle = onCameraToggle,
            onCameraSwitch = onCameraSwitch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}
