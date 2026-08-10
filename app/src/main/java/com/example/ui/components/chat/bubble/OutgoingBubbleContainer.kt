package com.example.ui.components.chat.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun OutgoingBubbleContainer(
    groupPosition: MessageGroupPosition,
    modifier: Modifier = Modifier,
    shape: Shape = BubbleShapeFactory.createShape(groupPosition, isMe = true),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation: Float = 1f,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation.dp
        ) {
            content()
        }
    }
}

