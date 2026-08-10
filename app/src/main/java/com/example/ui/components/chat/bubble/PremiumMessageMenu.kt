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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * PremiumMenuItem Data model
 */
data class PremiumMenuItemData(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * PremiumMessageMenu
 * Floating context menu overlay inspired by Telegram/iMessage with a reaction pill on top
 * and smooth rounded surface for chat actions.
 */
@Composable
fun PremiumMessageMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectReaction: (String) -> Unit,
    menuItems: List<PremiumMenuItemData>,
    modifier: Modifier = Modifier,
    isMe: Boolean = false
) {
    if (!expanded) return

    val haptic = LocalHapticFeedback.current

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissRequest()
                },
            contentAlignment = if (isMe) Alignment.TopEnd else Alignment.TopStart
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 240.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // Fila Superior: ReactionPill
                ReactionPill(
                    visible = expanded,
                    onSelectReaction = { emoji ->
                        onSelectReaction(emoji)
                        onDismissRequest()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Surface Flotante con Acciones
                AnimatedVisibility(
                    visible = expanded,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF233138),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                        ) {
                            menuItems.forEachIndexed { index, item ->
                                val textColor = if (item.isDestructive) Color(0xFFFF5252) else Color.White

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            item.onClick()
                                            onDismissRequest()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.title,
                                        color = textColor,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item.iconEmoji,
                                        fontSize = 15.sp
                                    )
                                }

                                if (index < menuItems.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFF2E3B43),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
