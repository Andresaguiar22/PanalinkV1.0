package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Notification
import com.example.data.model.NotificationType
import com.example.ui.viewmodel.NotificationsUiState
import com.example.ui.viewmodel.NotificationsViewModel
import com.example.identity.model.toIdentityUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToState: (String) -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToReel: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (uiState is NotificationsUiState.Success && (uiState as NotificationsUiState.Success).notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllNotifications() }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Limpiar todas")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161618),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF161618)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    NotificationLoading()
                }
                is NotificationsUiState.Error -> {
                    LaunchedEffect(state.message) {
                        snackbarHostState.showSnackbar(state.message)
                    }
                    EmptyNotificationView("Ha ocurrido un error al cargar las notificaciones.")
                }
                is NotificationsUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyNotificationView("Aún no tienes notificaciones")
                    } else {
                        val grouped = groupNotificationsByDate(state.notifications)
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            grouped.forEach { (dateGroup, notifs) ->
                                stickyHeader {
                                    NotificationGroup(dateGroup)
                                }
                                itemsIndexed(notifs, key = { index, notification -> "${notification.id}_$index" }) { _, notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onClick = {
                                            viewModel.clearNotification(notification.id)
                                            when (notification.type) {
                                                NotificationType.LIKE, NotificationType.COMMENT, NotificationType.FAVORITE, NotificationType.SHARE, NotificationType.VIEW, NotificationType.POST -> {
                                                    onNavigateToPostDetail(notification.sourceId)
                                                }
                                                NotificationType.REEL -> {
                                                    onNavigateToReel(notification.sourceId)
                                                }
                                                NotificationType.MESSAGE, NotificationType.CALL -> {
                                                    onNavigateToChat(notification.sourceId, notification.profile?.id ?: "")
                                                }
                                                NotificationType.FOLLOWER -> {
                                                    onNavigateToProfile(notification.profile?.id ?: "")
                                                }
                                                else -> {}
                                            }
                                        }
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

@Composable
fun NotificationGroup(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161618).copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) Color.Transparent else Color(0xFFB026FF).copy(alpha = 0.1f)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(notification.profile?.id ?: "").collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(notification.profile?.id ?: "")?.toIdentityUiState())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            com.example.ui.components.PanaAvatar(
                avatarUrl = identityState?.avatarUrl ?: notification.profile?.avatarUrl,
                userId = identityState?.userId ?: notification.profile?.id,
                size = 56.dp,
                borderWidth = 0.dp,
                placeholderName = identityState?.displayName ?: notification.profile?.displayName
            )
            
            // Icon badge
            val (icon, color) = getNotificationIcon(notification.type)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF161618), CircleShape)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val name = identityState?.displayName ?: notification.profile?.displayName ?: ""
            val timeStr = formatNotificationTime(notification.timestamp)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeStr,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = notification.actionText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!notification.previewText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${notification.previewText}\"",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFB026FF), CircleShape)
            )
        }
    }
}

@Composable
fun EmptyNotificationView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NotificationLoading() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF262629), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .background(Color(0xFF262629), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .background(Color(0xFF262629), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

// Helpers
fun getNotificationIcon(type: NotificationType): Pair<ImageVector, Color> {
    return when (type) {
        NotificationType.LIKE -> Pair(Icons.Default.Favorite, Color.Red)
        NotificationType.COMMENT -> Pair(Icons.Default.ChatBubble, Color(0xFF00C853))
        NotificationType.FOLLOWER -> Pair(Icons.Default.Person, Color(0xFF2962FF))
        NotificationType.MESSAGE -> Pair(Icons.Default.Email, Color(0xFFFF9100))
        NotificationType.CALL -> Pair(Icons.Default.Call, Color(0xFFD50000))
        NotificationType.FAVORITE -> Pair(Icons.Default.Star, Color(0xFFFFD600))
        NotificationType.SHARE -> Pair(Icons.Default.Share, Color(0xFF00B0FF))
        NotificationType.VIEW -> Pair(Icons.Default.Visibility, Color(0xFF00E676))
        else -> Pair(Icons.Default.Notifications, Color.Gray)
    }
}

fun formatNotificationTime(timestamp: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp) ?: return ""
        val diff = System.currentTimeMillis() - date.time
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            minutes < 1 -> "Ahora"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    } catch (e: Exception) {
        return ""
    }
}

fun groupNotificationsByDate(notifications: List<Notification>): Map<String, List<Notification>> {
    val groups = mutableMapOf<String, MutableList<Notification>>()
    val now = Calendar.getInstance()
    
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
    parser.timeZone = TimeZone.getTimeZone("UTC")
    
    notifications.forEach { notif ->
        val notifCal = Calendar.getInstance()
        try {
            val date = parser.parse(notif.timestamp)
            if (date != null) notifCal.time = date
        } catch (e: Exception) {}
        
        val diffDays = (now.timeInMillis - notifCal.timeInMillis) / (1000 * 60 * 60 * 24)
        
        val group = when {
            diffDays < 1 && now.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR) -> "Hoy"
            diffDays < 2 -> "Ayer"
            diffDays < 7 -> "Esta semana"
            else -> "Anteriormente"
        }
        
        groups.getOrPut(group) { mutableListOf() }.add(notif)
    }
    
    return groups
}

@Composable
fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count > 0) {
        Box(
            modifier = modifier
                .background(Color.Red, CircleShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "+99" else count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
