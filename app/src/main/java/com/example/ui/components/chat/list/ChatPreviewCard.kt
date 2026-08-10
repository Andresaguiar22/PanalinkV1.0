package com.example.ui.components.chat.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChatWithDetails
import com.example.data.model.formatIsoDateTime
import com.example.data.supabase.SupabaseClient
import com.example.identity.model.toIdentityUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatPreviewCard(
    chatDetails: ChatWithDetails,
    isTyping: Boolean,
    isSelected: Boolean = false,
    isPinned: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val initialOtherUser = chatDetails.otherMember
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val initialCached = remember(initialOtherUser?.id) { com.example.identity.memory.IdentityMemoryCache.profiles.get(initialOtherUser?.id ?: "") }
    val identityState by identityRepository.observeIdentity(initialOtherUser?.id ?: "").collectAsStateWithLifecycle(initialValue = initialCached?.toIdentityUiState()) // Wait, I need to import it or use top level
    
    val safeAvatarUrl = identityState?.avatarUrl ?: initialOtherUser?.avatarUrl
    val safeDisplayName = identityState?.displayName ?: initialOtherUser?.displayName ?: "Usuario Desconocido"
    val safeUserId = identityState?.userId ?: initialOtherUser?.id
    val safeVerified = identityState?.verified ?: false

    val lastMessage = chatDetails.lastMessage
    val formattedTime = formatIsoDateTime(lastMessage?.createdAt ?: chatDetails.chat.createdAt)

    val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsState()
    val presenceInfo = presenceMap[safeUserId ?: ""]
    val userStatus = presenceInfo?.status?.rawValue ?: "offline"
    val secondaryStatus = if (presenceInfo?.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) presenceInfo?.secondaryStatus?.rawValue else null
    val isOnline = userStatus != "offline"

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2C2C2E) else Color.Transparent,
        label = "bg_color"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 20 }),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Section
            Box(modifier = Modifier.size(56.dp)) {
                com.example.ui.components.PanaAvatar(
                    avatarUrl = safeAvatarUrl,
                    userId = safeUserId,
                    placeholderName = safeDisplayName,
                    size = 56.dp,
                    borderWidth = 0.dp
                )
                
                // Online Indicator
                PresenceIndicator(
                    isOnline = isOnline,
                    status = userStatus,
                    secondaryStatus = secondaryStatus,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    borderColor = if (isSelected) Color(0xFF2C2C2E) else Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content Section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = safeDisplayName ?: "Pana de panalink",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formattedTime,
                        color = if (chatDetails.unreadCount > 0) Color(0xFF00FF85) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (chatDetails.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedContent(
                        targetState = isTyping,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                            fadeOut(animationSpec = tween(90))
                        },
                        label = "typing_content",
                        modifier = Modifier.weight(1f)
                    ) { typing ->
                        if (typing) {
                            TypingIndicator()
                        } else {
                            Text(
                                text = lastMessage?.content ?: "Inicia la conversación...",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chatDetails.chat.isMuted) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Silenciado",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        AnimatedContent(
                            targetState = chatDetails.unreadCount,
                            transitionSpec = {
                                scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith
                                scaleOut()
                            },
                            label = "unread_badge"
                        ) { count ->
                            ChatUnreadBadge(count = count)
                        }
                    }
                }
            }
        }
    }
}
