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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Error
import com.example.util.DeliveryState

@Composable
fun MessageStatusIndicator(
    formattedTime: String,
    deliveryState: DeliveryState,
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
            color = textColor.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )

        if (isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedContent(
                targetState = deliveryState,
                transitionSpec = {
                    (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith fadeOut()
                },
                label = "statusTickAnimation"
            ) { targetStatus ->
                when (targetStatus) {
                    DeliveryState.SENDING -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Enviando",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    DeliveryState.OFFLINE_PENDING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Pendiente (Sin conexión)",
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    DeliveryState.FAILED -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DeliveryState.SENT -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Enviado",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    DeliveryState.DELIVERED -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Entregado",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    DeliveryState.READ -> {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Leído",
                            tint = Color(0xFF34B7F1),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    else -> {
                        // UNKNOWN -> Do not show tick
                    }
                }
            }
        }
    }
}
