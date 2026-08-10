package com.example.ui.components.chat.list

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00A884)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "escribiendo",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Dot(0)
            Dot(1)
            Dot(2)
        }
    }
}

@Composable
private fun Dot(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.2f at index * 150
                1f at index * 150 + 300
                0.2f at index * 150 + 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .size(3.dp)
            .alpha(alpha)
            .background(Color(0xFF00A884), CircleShape)
    )
}
