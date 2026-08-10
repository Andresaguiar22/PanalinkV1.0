package com.example.ui.components.chat.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLog
import com.example.data.model.CallLogStatus
import com.example.data.model.CallLogType

/**
 * CallLogChatBubble renders a call history record inside the chat stream
 * with Telegram / WhatsApp styling.
 */
@Composable
fun CallLogChatBubble(
    callLog: CallLog,
    isMe: Boolean,
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    val isVideo = callLog.type == CallLogType.VIDEO
    val isSuccess = callLog.status == CallLogStatus.COMPLETED

    val iconColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
    val iconBgColor = iconColor.copy(alpha = 0.15f)

    val titleText = when {
        isVideo && isMe -> "Videollamada saliente"
        isVideo -> "Videollamada entrante"
        isMe -> "Llamada de voz saliente"
        else -> "Llamada de voz entrante"
    }

    val subtitleText = when (callLog.status) {
        CallLogStatus.COMPLETED -> {
            val mins = callLog.durationSeconds / 60
            val secs = callLog.durationSeconds % 60
            if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }
        CallLogStatus.MISSED -> "Llamada perdida"
        CallLogStatus.REJECTED -> "Llamada rechazada"
        CallLogStatus.CANCELLED -> "Llamada cancelada"
    }

    val arrowIcon = when {
        callLog.status == CallLogStatus.MISSED || callLog.status == CallLogStatus.REJECTED -> Icons.AutoMirrored.Filled.CallMissed
        isMe -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }

    Surface(
        modifier = modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) Color(0xFF005C4B) else Color(0xFF202C33)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 260.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitleText,
                        color = Color(0xFF8596A0),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedTime,
                color = Color(0xFF8596A0),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}
