package com.example.ui.components.chat.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message

@Composable
fun GhostMessageBubble(
    message: Message,
    isMe: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpened = message.ghostOpenedAt != null
    val bubbleColor = if (isMe) Color(0xFFD9FDD3) else Color(0xFFFFFFFF)
    val ghostColor = Color(0xFFBB86FC)
    val textColor = Color(0xFF111B21)

    Surface(
        modifier = modifier
            .padding(vertical = 2.dp)
            .widthIn(max = 280.dp),
        shape = RoundedCornerShape(16.dp),
        color = bubbleColor,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (!isOpened) Modifier.clickable { onOpen() } else Modifier)
                    .background(if (isOpened) Color.LightGray.copy(alpha = 0.2f) else ghostColor.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (isOpened) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isOpened) Color.Gray else ghostColor,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = if (isOpened) "Secreto consumido" else "Mensaje Fantasma",
                        color = if (isOpened) Color.Gray else ghostColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isOpened) "Ya no puedes ver este contenido" else "Toca para revelar el secreto",
                        color = if (isOpened) Color.Gray else textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            if (!isOpened && !isMe) {
                Text(
                    text = "⚠️ Este mensaje se auto-destruirá al cerrarlo.",
                    color = Color.Red.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }
}
