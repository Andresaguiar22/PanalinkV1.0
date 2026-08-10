package com.example.ui.components.chat.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun IncomingBubbleContainer(
    groupPosition: MessageGroupPosition,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = BubbleShapeFactory.createShape(groupPosition, isMe = false),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation: Float = 1f,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        com.example.ui.components.PanaAvatar(
            avatarUrl = avatarUrl,
            size = 28.dp,
            borderWidth = 0.dp,
            contentDescription = "Avatar de contacto"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation.dp
        ) {
            content()
        }
    }
}

