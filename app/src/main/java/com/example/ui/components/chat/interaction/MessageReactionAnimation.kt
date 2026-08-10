package com.example.ui.components.chat.interaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * MessageReactionAnimation
 * Entrance, exit, and subtle spring vibration spec helpers for ReactionPill and reaction badges.
 */
object MessageReactionAnimation {

    fun enterSpec(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(150))
    }

    fun exitSpec(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + scaleOut(
            targetScale = 0.8f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(animationSpec = tween(120))
    }
}

/**
 * Modifier wrapper that applies a slight spring pulse/vibration when triggered.
 */
@Composable
fun Modifier.reactionPulseEffect(trigger: Boolean): Modifier {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            scale.animateTo(
                targetValue = 1.25f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
