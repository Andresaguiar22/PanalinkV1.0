package com.example.ui.chat.emoji

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedEmojiContent(
    emoji: String,
    animationType: EmojiAnimationType,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 64.sp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "animated_emoji_transition")

    when (animationType) {
        EmojiAnimationType.HEART_BEAT -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        1.0f at 0 with FastOutSlowInEasing
                        1.25f at 200 with FastOutSlowInEasing
                        1.05f at 350 with FastOutSlowInEasing
                        1.25f at 500 with FastOutSlowInEasing
                        1.0f at 800 with FastOutSlowInEasing
                        1.0f at 1000 with FastOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "heartBeatScale"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
        }

        EmojiAnimationType.LAUGH_SHAKE -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laughShakeRotation"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laughShakeOffsetY"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    rotationZ = rotation
                    translationY = offsetY
                }
            )
        }

        EmojiAnimationType.BOUNCE_EFFECT -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounceOffsetY"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                }
            )
        }

        EmojiAnimationType.FIRE_EFFECT -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fireScale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fireRotation"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
            )
        }

        EmojiAnimationType.KISS_THROW -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "kissScale"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "kissOffsetY"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = offsetY
                }
            )
        }
    }
}
