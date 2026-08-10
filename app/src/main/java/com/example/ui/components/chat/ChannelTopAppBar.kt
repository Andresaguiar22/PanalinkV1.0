package com.example.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Telegram-style Top App Bar for Channel view (`ChannelTopAppBar`).
 * Features channel avatar, name, verified checkmark, formatted subscriber count,
 * back button, search/mute quick actions, and options overflow menu.
 */
@Composable
fun ChannelTopAppBar(
    channelName: String,
    subscriberCount: Int,
    avatarUrl: String? = null,
    isVerified: Boolean = true,
    isMuted: Boolean = false,
    onBackClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMuteToggleClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHeaderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF17212B) // Telegram Dark TopBar
    val contentColor = Color.White
    val secondaryTextColor = Color(0xFF8E959B)
    val verifiedBadgeColor = Color(0xFF2AABEE) // Telegram Cyan

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = contentColor
            )
        }

        // Channel Info Header (Clickable for info screen)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable { onHeaderClick() }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar
            AsyncImage(
                model = avatarUrl ?: "https://ui-avatars.com/api/?name=${channelName}&background=2AABEE&color=fff",
                contentDescription = "Channel Avatar",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channelName,
                        color = contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = verifiedBadgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatSubscriberCount(subscriberCount),
                    color = secondaryTextColor,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Icons (Search, Mute toggle, Options)
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = contentColor
            )
        }

        IconButton(onClick = onMuteToggleClick) {
            Icon(
                imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                contentDescription = if (isMuted) "Activar notificaciones" else "Silenciar",
                tint = if (isMuted) Color(0xFFE53935) else contentColor
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Más opciones",
                tint = contentColor
            )
        }
    }
}

/**
 * Formats subscriber counts into human readable strings like "2.5K suscriptores" or "2M suscriptores".
 */
private fun formatSubscriberCount(count: Int): String {
    return when {
        count < 1000 -> "$count suscriptores"
        count < 1_000_000 -> String.format("%.1fK suscriptores", count / 1000.0).replace(".0", "")
        else -> String.format("%.1fM suscriptores", count / 1_000_000.0).replace(".0", "")
    }
}
