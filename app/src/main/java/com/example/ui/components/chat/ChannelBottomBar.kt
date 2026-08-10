package com.example.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom action bar for Telegram-style Channels (`ChannelBottomBar`).
 * Contains floating internal search button, central Mute/Unmute pill button,
 * and floating scroll-to-bottom button with unread count badge.
 */
@Composable
fun ChannelBottomBar(
    isMuted: Boolean = false,
    unreadCount: Int = 0,
    showScrollToBottom: Boolean = true,
    onMuteToggleClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onScrollToBottomClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val barBg = Color(0xFF17212B) // Telegram Bottom Bar Surface
    val buttonBg = Color(0xFF242F3D) // Telegram Button Surface
    val primaryText = Color.White
    val secondaryText = Color(0xFF8E959B)
    val accentBlue = Color(0xFF2AABEE)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Floating Search Button (Left)
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(buttonBg)
                .clickable { onSearchClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar en canal",
                tint = primaryText,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Main Central Pill Button (Mute / Unmute / Activate Notifications)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .shadow(2.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(if (isMuted) buttonBg else Color(0xFF2B5278))
                .clickable { onMuteToggleClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (isMuted) accentBlue else secondaryText,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isMuted) "Activar notificaciones" else "Silenciar",
                    color = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Scroll to Bottom Button with Unread Badge (Right)
        AnimatedVisibility(
            visible = showScrollToBottom || unreadCount > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = accentBlue,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (unreadCount > 999) "999+" else unreadCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .clickable { onScrollToBottomClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar al final",
                        tint = primaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
