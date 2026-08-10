package com.example.ui.components.chat.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ReactionPill
 * Capsule surface displaying quick reaction emojis with smooth spring animations and haptic feedback.
 */
@Composable
fun ReactionPill(
    visible: Boolean,
    onSelectReaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String> = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = com.example.ui.components.chat.interaction.MessageReactionAnimation.enterSpec(),
        exit = com.example.ui.components.chat.interaction.MessageReactionAnimation.exitSpec()
    ) {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = Color(0xFF1F2C34),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                emojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectReaction(emoji)
                            }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
