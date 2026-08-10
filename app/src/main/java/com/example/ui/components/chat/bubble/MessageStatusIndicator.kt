package com.example.ui.components.chat.bubble

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.Error

/**
 * MessageStatusIndicator
 * Premium indicator for message timestamp, edit status, star/pin badges, and status ticks with micro-animations.
 */
@Composable
fun MessageStatusIndicator(
    formattedTime: String,
    status: String?,
    isMe: Boolean,
    isEdited: Boolean = false,
    isFavorited: Boolean = false,
    isPinned: Boolean = false,
    textColor: Color = Color(0xFF8596A0),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEdited) {
            Text(
                text = "(editado) ",
                color = textColor,
                fontSize = 9.sp
            )
        }
        if (isFavorited) {
            Text(
                text = "⭐ ",
                fontSize = 9.sp
            )
        }
        if (isPinned) {
            Text(
                text = "📌 ",
                fontSize = 9.sp
            )
        }
        Text(
            text = formattedTime,
            color = textColor,
            fontSize = 10.sp
        )
        if (isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith fadeOut()
                },
                label = "statusTickAnimation"
            ) { targetStatus ->
                when (targetStatus) {
                    "sending", "pending_media" -> {
                        Text(
                            text = "🕒",
                            color = textColor,
                            fontSize = 10.sp
                        )
                    }
                    "failed" -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    "sent" -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Enviado",
                            tint = textColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    "delivered" -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Entregado",
                            tint = textColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    "seen", "read" -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Leído",
                            tint = Color(0xFF53BDEB),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    else -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Enviado",
                            tint = textColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

