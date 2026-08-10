package com.example.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.bounceClick

/**
 * IncomingCallScreen handles incoming voice or video calls by offering accept/reject flows,
 * styled with beautiful Material 3 components and glowing pulser waves.
 */
@Composable
fun IncomingCallScreen(
    opponentId: String? = null,
    opponentName: String,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Info using CallStatusText
            CallStatusText(
                statusText = if (isVideo) "Videollamada entrante" else "Llamada de voz entrante",
                opponentName = opponentName,
                statusColor = Color(0xFF38BDF8),
                modifier = Modifier.padding(top = 64.dp)
            )

            // Central Pulsing Avatar (Green for Audio, Cyan/Blue for Video)
            CallAvatarPulse(
                name = opponentName,
                userId = opponentId,
                pulseColor = if (isVideo) Color(0xFF0EA5E9) else Color(0xFF22C55E),
                avatarSize = 130.dp
            )

            // Action controls (Quick reply message, Accept, Reject)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Optional: Responder con mensaje rápido
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .bounceClick {
                            // Quick reply placeholder
                        }
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Quick reply",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Responder con mensaje",
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Reject Call Button (Red)
                    CallActionButton(
                        onClick = onReject,
                        icon = Icons.Default.CallEnd,
                        contentDescription = "Rechazar Llamada",
                        containerColor = Color(0xFFEF4444), // Red 500
                        contentColor = Color.White,
                        size = 70.dp,
                        iconSize = 32.dp,
                        label = "Rechazar",
                        testTag = "reject_button"
                    )

                    Spacer(modifier = Modifier.width(48.dp))

                    // Accept Call Button (Green)
                    CallActionButton(
                        onClick = onAccept,
                        icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Aceptar Llamada",
                        containerColor = Color(0xFF22C55E), // Emerald 500
                        contentColor = Color.White,
                        size = 70.dp,
                        iconSize = 32.dp,
                        label = "Aceptar",
                        testTag = "accept_button"
                    )
                }
            }
        }
    }
}
