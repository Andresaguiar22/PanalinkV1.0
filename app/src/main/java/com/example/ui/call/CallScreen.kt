package com.example.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.call.CallState
import com.example.call.CallType

/**
 * CallScreen is the master component that dynamically renders the entire VoIP call interface.
 * It manages transitions between different call states seamlessly.
 */
@Composable
fun CallScreen(
    opponentId: String? = null,
    opponentName: String,
    callState: CallState,
    callType: CallType,
    formattedDuration: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraOn: Boolean,
    videoViewModel: com.example.call.VideoCallViewModel?,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Premium slate 900
    ) {
        AnimatedContent(
            targetState = callState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "callScreenStateTransition",
            modifier = Modifier.fillMaxSize()
        ) { targetState ->
            when (targetState) {
                is CallState.IDLE -> {
                    // Placeholder for idle state (usually handled by routing out)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF22C55E))
                    }
                }

                is CallState.OUTGOING -> {
                    // Outgoing call screen (Calling state)
                    OutgoingCallScreen(
                        opponentId = opponentId,
                        opponentName = opponentName,
                        isVideo = callType == CallType.VIDEO,
                        onCancel = onEndCall
                    )
                }

                is CallState.RINGING -> {
                    // Incoming call screen
                    IncomingCallScreen(
                        opponentId = opponentId,
                        opponentName = opponentName,
                        isVideo = callType == CallType.VIDEO,
                        onAccept = onAcceptCall,
                        onReject = onRejectCall
                    )
                }

                is CallState.CONNECTING -> {
                    // Connecting phase
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CallAvatarPulse(
                                userId = opponentId,
                                name = opponentName,
                                pulseColor = Color(0xFF38BDF8),
                                isAnimating = true
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Conectando...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Estableciendo conexión segura",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                CallState.CONNECTED, CallState.RECONNECTING -> {
                    // Active call screen based on audio/video
                    val isConnected = targetState == CallState.CONNECTED
                    if (callType == CallType.VIDEO && videoViewModel != null) {
                        VideoCallScreen(
                        opponentId = opponentId,
                        opponentName = opponentName,
                            formattedDuration = formattedDuration,
                            isMuted = isMuted,
                            isSpeakerOn = isSpeakerOn,
                            isCameraOn = isCameraOn,
                            isConnected = isConnected,
                            viewModel = videoViewModel,
                            onMuteToggle = onMuteToggle,
                            onSpeakerToggle = onSpeakerToggle,
                            onCameraToggle = onCameraToggle,
                            onCameraSwitch = onSwitchCamera,
                            onEndCall = onEndCall
                        )
                    } else {
                        AudioCallScreen(
                        opponentId = opponentId,
                        opponentName = opponentName,
                            formattedDuration = formattedDuration,
                            isMuted = isMuted,
                            isSpeakerOn = isSpeakerOn,
                            isConnected = isConnected,
                            onMuteToggle = onMuteToggle,
                            onSpeakerToggle = onSpeakerToggle,
                            onEndCall = onEndCall
                        )
                    }
                }

                CallState.BUSY -> {
                    CallErrorScreen(
                        title = "Usuario Ocupado",
                        subtitle = "está en otra llamada.",
                        opponentId = opponentId,
                        opponentName = opponentName,
                        onDismiss = onDismissError,
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFF59E0B) // Amber
                    )
                }

                CallState.REJECTED -> {
                    CallErrorScreen(
                        title = "Llamada Rechazada",
                        subtitle = "rechazó tu llamada.",
                        opponentId = opponentId,
                        opponentName = opponentName,
                        onDismiss = onDismissError,
                        icon = Icons.Default.CallEnd,
                        iconColor = Color(0xFFEF4444) // Red
                    )
                }

                CallState.FAILED, CallState.DISCONNECTED -> {
                    CallErrorScreen(
                        title = "Error de Conexión",
                        subtitle = "no se pudo conectar. Comprueba la red.",
                        opponentId = opponentId,
                        opponentName = opponentName,
                        onDismiss = onDismissError,
                        icon = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                        iconColor = Color(0xFFEF4444)
                    )
                }

                CallState.ENDED, CallState.CANCELLED, CallState.MISSED -> {
                    CallErrorScreen(
                        title = "Llamada Finalizada",
                        subtitle = "ha colgado.",
                        opponentId = opponentId,
                        opponentName = opponentName,
                        onDismiss = onDismissError,
                        icon = Icons.Default.CallEnd,
                        iconColor = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
