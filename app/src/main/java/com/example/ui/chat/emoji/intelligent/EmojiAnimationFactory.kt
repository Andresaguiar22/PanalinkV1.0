package com.example.ui.chat.emoji.intelligent

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun RenderIntelligentEmojiAnimation(
    emoji: String,
    animation: EmojiPremiumAnimation,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 64.sp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "intelligent_emoji_transition")

    when (animation) {
        EmojiPremiumAnimation.WAVE_GESTURE -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -18f,
                targetValue = 18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveRotation"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    rotationZ = rotation
                }
            )
        }

        EmojiPremiumAnimation.THUMBS_UP_BOUNCE -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thumbsUpOffsetY"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thumbsUpScale"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
            )
        }

        EmojiPremiumAnimation.THUMBS_DOWN_DROP -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thumbsDownOffsetY"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thumbsDownRotation"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    rotationZ = rotation
                }
            )
        }

        EmojiPremiumAnimation.CLAP_RHYTHM -> {
            val scaleX by infiniteTransition.animateFloat(
                initialValue = 0.90f,
                targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "clapScaleX"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    this.scaleX = scaleX
                }
            )
        }

        EmojiPremiumAnimation.PRAY_SWAY -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "prayOffsetY"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "prayScale"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    this.scaleX = scale
                    this.scaleY = scale
                }
            )
        }

        EmojiPremiumAnimation.OK_CONFIRM -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "okScale"
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

        EmojiPremiumAnimation.LAUGH_VIBRATE -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -7f,
                targetValue = 7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 160, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laughRotation"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 320, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laughOffsetY"
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

        EmojiPremiumAnimation.CRY_TEARS_FLOAT -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cryOffsetY"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cryRotation"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    rotationZ = rotation
                }
            )
        }

        EmojiPremiumAnimation.LOVE_HEART_BEAT -> {
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
                label = "loveScale"
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

        EmojiPremiumAnimation.ANGRY_SHAKE -> {
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 90, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "angryOffsetX"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationX = offsetX
                }
            )
        }

        EmojiPremiumAnimation.SLEEP_FLOAT -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sleepScale"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sleepOffsetY"
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

        EmojiPremiumAnimation.DOG_WAG -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dogRotation"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dogOffsetY"
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

        EmojiPremiumAnimation.CAT_PURR -> {
            val scaleX by infiniteTransition.animateFloat(
                initialValue = 0.97f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "catScaleX"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    this.scaleX = scaleX
                }
            )
        }

        EmojiPremiumAnimation.FROG_HOP -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "frogOffsetY"
            )
            val scaleY by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "frogScaleY"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    this.scaleY = scaleY
                }
            )
        }

        EmojiPremiumAnimation.CAR_DRIVE -> {
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "carOffsetX"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationX = offsetX
                }
            )
        }

        EmojiPremiumAnimation.PLANE_FLY -> {
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "planeOffsetX"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "planeOffsetY"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                }
            )
        }

        EmojiPremiumAnimation.ROCKET_LAUNCH -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = -18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rocketOffsetY"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rocketScale"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
            )
        }

        EmojiPremiumAnimation.BALL_BOUNCE -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ballOffsetY"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ballRotation"
            )
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                    rotationZ = rotation
                }
            )
        }

        EmojiPremiumAnimation.PARTY_BURST -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "partyScale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "partyRotation"
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

        EmojiPremiumAnimation.FIRE_ORGANIC -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fireScale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 280, easing = LinearEasing),
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
    }
}
