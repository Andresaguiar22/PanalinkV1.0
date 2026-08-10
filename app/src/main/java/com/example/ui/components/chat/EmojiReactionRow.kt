package com.example.ui.components.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data item representing an emoji reaction and its count.
 */
data class ReactionItem(
    val emoji: String,
    val count: Int,
    val isUserReacted: Boolean = false
)

/**
 * Interactive row of oval pill reaction buttons (`EmojiReactionRow`).
 * Shows emoji + counter, highlights active user reactions, and triggers tap callbacks.
 */
@Composable
fun EmojiReactionRow(
    reactions: List<ReactionItem>,
    onReactionClick: (String) -> Unit,
    onAddReactionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(reactions, key = { index, item -> "${item.emoji}_$index" }) { _, item ->
            ReactionPillButton(
                item = item,
                onClick = { onReactionClick(item.emoji) }
            )
        }

        if (onAddReactionClick != null) {
            item {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242F3D))
                        .clickable { onAddReactionClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar reacción",
                        tint = Color(0xFF8E959B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionPillButton(
    item: ReactionItem,
    onClick: () -> Unit
) {
    val activeBg = Color(0xFF2B5278) // Telegram Active Pill Blue-Gray
    val inactiveBg = Color(0xFF1E2C3A) // Telegram Inactive Pill Surface
    val activeBorder = Color(0xFF2AABEE)
    val inactiveBorder = Color(0xFF2B3A4A)

    val bgColor by animateColorAsState(
        targetValue = if (item.isUserReacted) activeBg else inactiveBg,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (item.isUserReacted) activeBorder else inactiveBorder,
        label = "borderColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (item.isUserReacted) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.emoji,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = formatReactionCount(item.count),
            color = if (item.isUserReacted) Color.White else Color(0xFF8E959B),
            fontSize = 12.sp,
            fontWeight = if (item.isUserReacted) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun formatReactionCount(count: Int): String {
    return when {
        count < 1000 -> "$count"
        count < 1_000_000 -> String.format("%.1fK", count / 1000.0).replace(".0", "")
        else -> String.format("%.1fM", count / 1_000_000.0).replace(".0", "")
    }
}
