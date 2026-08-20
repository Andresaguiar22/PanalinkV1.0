@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screen

import com.example.ui.components.*
import com.example.util.*

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.FeedPostCard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import com.example.ui.viewmodel.StatesViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.identity.model.toIdentityUiState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.model.*
import com.example.data.supabase.SupabaseClient
import com.example.ui.viewmodel.*
import com.example.ui.theme.shimmerEffect
import com.example.ui.theme.getAvatarGradient
import com.example.ui.components.PanalinkPullToRefreshBox
import com.example.ui.theme.bounceClick
import com.example.ui.components.chat.list.ChatPreviewCard
import com.example.util.ChatListScrollManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun ChatsTabContent(
    chatsState: ChatsUiState,
    typingChats: Map<String, Boolean>,
    contactsState: ContactsUiState,
    statesState: StatesUiState,
    chatsViewModel: ChatsViewModel,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToViewState: (String) -> Unit,
    onNavigateToCreateState: () -> Unit,
    onRefresh: () -> Unit,
    selectedChatIds: Set<String> = emptySet(),
    onToggleChatSelection: (String) -> Unit = {},
    onStartChatSelection: (String) -> Unit = {},
    deletedChatIds: Set<String> = emptySet(),
    pinnedChatIds: Set<String> = emptySet(),
    mutedChatIds: Set<String> = emptySet(),
    customUnreadCounts: Map<String, Int> = emptyMap()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = com.example.ui.theme.LocalAppColors.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Restore scroll position
    LaunchedEffect(Unit) {
        val pos = ChatListScrollManager.getPosition(context)
        if (pos != null) {
            listState.scrollToItem(pos.first, pos.second)
        }
    }

    // Save scroll position
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            ChatListScrollManager.savePosition(
                context,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High-fidelity Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar panas o mensajes...", color = Color.Gray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Icono de búsqueda",
                    tint = Color.White
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp)
        )

        PanalinkPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    kotlinx.coroutines.delay(1200)
                    isRefreshing = false
                }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Chats Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (searchQuery.isEmpty()) {
                    // Active Chats List
                    when (chatsState) {
                        is ChatsUiState.Loading -> {
                            items(5) {
                                ShimmerChatItemRow()
                            }
                        }
                        is ChatsUiState.Success -> {
                            var chats = chatsState.chats
                            // Filter deleted and archived
                            chats = chats.filterNot { deletedChatIds.contains(it.chat.id) || it.chat.isArchived }
                            // Sort pinned to the top
                            chats = chats.sortedWith(
                                compareByDescending<ChatWithDetails> { it.chat.isPinned }
                                    .thenByDescending { it.chat.pinnedAt ?: "" }
                                    .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
                            )

                            if (chats.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(72.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No tienes chats activos", color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Presiona el botón de abajo para buscar panas.", color = Color(0xFF607D8B), fontSize = 13.sp)
                                    }
                                }
                            } else {
                                itemsIndexed(chats, key = { index, chatDetails -> "${chatDetails.chat.id}_$index" }) { _, chatDetails ->
                                    ChatPreviewCard(
                                        chatDetails = if (customUnreadCounts.containsKey(chatDetails.chat.id)) {
                                            chatDetails.copy(unreadCount = customUnreadCounts[chatDetails.chat.id]!!)
                                        } else {
                                            chatDetails
                                        },
                                        isTyping = typingChats[chatDetails.chat.id] == true,
                                        isSelected = selectedChatIds.contains(chatDetails.chat.id),
                                        isPinned = chatDetails.chat.isPinned,
                                        onLongClick = {
                                            if (selectedChatIds.isEmpty()) {
                                                onStartChatSelection(chatDetails.chat.id)
                                            }
                                        },
                                        onClick = {
                                            if (selectedChatIds.isNotEmpty()) {
                                                onToggleChatSelection(chatDetails.chat.id)
                                            } else {
                                                onNavigateToChat(chatDetails.chat.id, chatDetails.otherMember?.id ?: "")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        is ChatsUiState.Error -> {
                            item {
                                Text(
                                    text = chatsState.message,
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Filtered results
                    when (chatsState) {
                        is ChatsUiState.Loading -> {
                            items(3) {
                                ShimmerChatItemRow()
                            }
                        }
                        is ChatsUiState.Success -> {
                            var chats = chatsState.chats
                            // Filter deleted and archived
                            chats = chats.filterNot { deletedChatIds.contains(it.chat.id) || it.chat.isArchived }
                            // Sort pinned to the top
                            chats = chats.sortedWith(
                                compareByDescending<ChatWithDetails> { it.chat.isPinned }
                                    .thenByDescending { it.chat.pinnedAt ?: "" }
                                    .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
                            )

                            val filteredChats = chats.filter { chatDetails ->
                                val otherUser = chatDetails.otherMember
                                val nameMatches = otherUser?.displayName?.contains(searchQuery, ignoreCase = true) == true
                                val msgMatches = chatDetails.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true
                                nameMatches || msgMatches
                            }

                            val filteredContacts = if (contactsState is ContactsUiState.Success) {
                                contactsState.contacts.filter { contact ->
                                    contact.displayName.contains(searchQuery, ignoreCase = true)
                                }.filter { contact ->
                                    filteredChats.none { chatDetails -> chatDetails.otherMember?.id == contact.id }
                                }
                            } else {
                                emptyList()
                            }

                            if (filteredChats.isEmpty() && filteredContacts.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(72.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Sin resultados para \"$searchQuery\"", color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Prueba con otro nombre o palabra clave.", color = Color(0xFF607D8B), fontSize = 13.sp)
                                    }
                                }
                            } else {
                                if (filteredChats.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "CONVERSACIONES ACTIVAS",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    itemsIndexed(filteredChats, key = { index, chatDetails -> "${chatDetails.chat.id}_$index" }) { _, chatDetails ->
                                        ChatPreviewCard(
                                            chatDetails = if (customUnreadCounts.containsKey(chatDetails.chat.id)) {
                                                chatDetails.copy(unreadCount = customUnreadCounts[chatDetails.chat.id]!!)
                                            } else {
                                                chatDetails
                                            },
                                            isTyping = typingChats[chatDetails.chat.id] == true,
                                            isSelected = selectedChatIds.contains(chatDetails.chat.id),
                                            isPinned = chatDetails.chat.isPinned,
                                            onLongClick = {
                                                if (selectedChatIds.isEmpty()) {
                                                    onStartChatSelection(chatDetails.chat.id)
                                                }
                                            },
                                            onClick = {
                                                if (selectedChatIds.isNotEmpty()) {
                                                    onToggleChatSelection(chatDetails.chat.id)
                                                } else {
                                                    onNavigateToChat(chatDetails.chat.id, chatDetails.otherMember?.id ?: "")
                                                }
                                            }
                                        )
                                    }
                                }

                                if (filteredContacts.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "PANAS / CONTACTOS",
                                            color = colors.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    items(filteredContacts) { contact ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    chatsViewModel.createChat(contact) { chat ->
                                                        onNavigateToChat(chat.id, contact.id)
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsStateWithLifecycle()
                                            val presenceInfo = presenceMap[contact.id]
                                            val statusStr = presenceInfo?.status?.rawValue ?: "offline"
                                            val secondaryStr = if (presenceInfo?.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) presenceInfo?.secondaryStatus?.rawValue else null
                                            ChatAvatar(
                                                name = contact.displayName,
                                                avatarUrl = contact.avatarUrl,
                                                status = statusStr,
                                                secondaryStatus = secondaryStr,
                                                size = 54.dp
                                            )

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = contact.displayName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Conectado por panalink",
                                                    color = Color(0xFF90A4AE),
                                                    fontSize = 13.sp
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    chatsViewModel.createChat(contact) { chat ->
                                                        onNavigateToChat(chat.id, contact.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = "Enviar mensaje",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is ChatsUiState.Error -> {
                            item {
                                Text(
                                    text = chatsState.message,
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ChatItemRow(
    chatDetails: ChatWithDetails,
    chatsViewModel: ChatsViewModel,
    onNavigateToChat: (String, String) -> Unit,
    isSelected: Boolean = false,
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    customUnreadCount: Int? = null,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    val otherUser = chatDetails.otherMember
    val lastMessage = chatDetails.lastMessage
    
    // Format timestamp nicely
    val formattedTime = com.example.data.model.formatIsoDateTime(lastMessage?.createdAt)

    val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsStateWithLifecycle()
    val isOnline = presenceMap[otherUser?.id ?: ""]?.status != com.example.data.repository.UserPresenceStatus.OFFLINE

    val rowBackground = if (isSelected) Color(0x1F25D366) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // User Profile Pic with dynamic gradient and status badges
        ChatAvatar(
            name = otherUser?.displayName ?: "Pana de panalink",
            avatarUrl = otherUser?.avatarUrl,
            status = presenceMap[otherUser?.id ?: ""]?.status?.rawValue ?: "offline", secondaryStatus = if (presenceMap[otherUser?.id ?: ""]?.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) presenceMap[otherUser?.id ?: ""]?.secondaryStatus?.rawValue else null,
            hasUnread = (customUnreadCount ?: chatDetails.unreadCount) > 0,
            size = 54.dp,
            isSelected = isSelected
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text Info (Name + Last Message)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = otherUser?.displayName ?: "Pana de panalink",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Anclado",
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = formattedTime,
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastMessage?.content ?: "Inicia la conversación chamo...",
                    color = Color(0xFF90A4AE),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Silenciado",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    
                    val finalUnreadCount = customUnreadCount ?: chatDetails.unreadCount
                    if (finalUnreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(colors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = finalUnreadCount.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ShimmerChatItemRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Shimmer Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shimmer Name
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                // Shimmer Time
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Shimmer Message Snippet
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}


@Composable
fun ChatAvatar(
    name: String,
    avatarUrl: String?,
    status: String = "offline",
    secondaryStatus: String? = null,
    hasUnread: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 54.dp,
    isSelected: Boolean = false
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    Box(
        modifier = Modifier
            .size(size)
            .bounceClick()
    ) {
        val borderModifier = if (hasUnread) {
            Modifier
                .fillMaxSize()
                .border(2.5.dp, com.example.ui.theme.getPremiumActiveIconGradient(), CircleShape)
                .padding(3.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = borderModifier
                .clip(CircleShape)
                .background(getAvatarGradient(name))
        ) {
            val resolvedUrl = remember(avatarUrl) {
                com.example.data.repository.CdnManager.resolveAvatarUrl(avatarUrl)
            }
            if (resolvedUrl != null) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = "Avatar de $name",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                val initials = if (name.isNotEmpty()) name.take(1).uppercase() else "?"
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.38f).sp
                    )
                }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF00A884), // WhatsApp primary green
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            val isOnline = status != "offline"
            if (isOnline || secondaryStatus != null) {
                com.example.ui.components.chat.list.PresenceIndicator(
                    isOnline = isOnline,
                    status = status,
                    secondaryStatus = secondaryStatus,
                    size = 13.dp,
                    showText = false,
                    showOffline = false,
                    borderColor = colors.background,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}


