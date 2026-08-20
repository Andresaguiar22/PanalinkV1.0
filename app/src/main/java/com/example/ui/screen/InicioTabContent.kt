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
fun TuTabContent(
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profileState by profileViewModel.profileState.collectAsState()
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val email = SupabaseClient.currentUser?.email ?: "pana@panalink.com"
    val colors = com.example.ui.theme.LocalAppColors.current

    val prefs = context.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
    val isMinimalistMode by com.example.ui.theme.ThemeManager.isMinimalistMode.collectAsState()
    var isFloatingPipEnabled by remember { mutableStateOf(prefs.getBoolean("floating_pip_enabled", true)) }

    val contactIdentifier by profileViewModel.contactIdentifierState.collectAsStateWithLifecycle()
    val userPinState = contactIdentifier?.pin ?: (profileState as? ProfileUiState.Success)?.profile?.pin ?: ""
    val qrPayloadState = contactIdentifier?.qrPayload ?: "panalink:contact:$currentUid"

    var displayName by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var pinInputText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var nameInputText by remember { mutableStateOf("") }

    var profileThemeChoice by remember { mutableStateOf(prefs.getString("profile_theme_${currentUid}", "dark_teal") ?: "dark_teal") }

    LaunchedEffect(currentUid) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(profileState) {
        if (profileState is ProfileUiState.Success) {
            val prof = (profileState as ProfileUiState.Success).profile
            displayName = prof.displayName
            avatarUrl = prof.avatarUrl ?: ""
        }
    }

    LaunchedEffect(profileThemeChoice) {
        prefs.edit().putString("profile_theme_${currentUid}", profileThemeChoice).putString("profile_theme_global", profileThemeChoice).apply()
        com.example.ui.theme.ThemeManager.themeKey.value = profileThemeChoice
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. USER PROFILE CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.secondary),
                border = BorderStroke(1.dp, Color(0xFF262629)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    com.example.ui.components.PanaAvatar(
    avatarUrl = avatarUrl.ifEmpty { null },
    size = 100.dp,
    borderColor = colors.accent,
    borderWidth = 3.dp,
    placeholderName = displayName
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                nameInputText = displayName
                                showEditNameDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar nombre",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = email,
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- 2. PIN Y CÓDIGO QR CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.secondary),
                border = BorderStroke(1.dp, Color(0xFF262629)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tu Código QR de Pana 🪪",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Comparte este QR o PIN para que te agreguen al instante",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userPinState.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(2.dp, colors.accent, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("QR Code") }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "PIN: $userPinState",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("PIN de Pana", userPinState)
                                    clipboardManager.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "¡PIN Copiado! 📋", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copiar PIN",
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = colors.accent)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            pinInputText = userPinState
                            pinError = ""
                            showSetPinDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Configurar PIN de Seguridad", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 3. CONFIGURACIONES DE PERFIL ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.secondary),
                border = BorderStroke(1.dp, Color(0xFF262629)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configuración de Aplicación 🇻🇪",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Tema de Pana Selector
                    Column {
                        Text("Tema Visual de Pana", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val themes = listOf(
                                "dark_teal" to "Teal",
                                "royal_purple" to "Púrpura",
                                "neon_orange" to "Naranja",
                                "nordic_ice" to "Ice"
                            )
                            themes.forEach { (key, label) ->
                                val isSelected = profileThemeChoice == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.accent else Color(0xFF121214))
                                        .clickable { profileThemeChoice = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E2E36), thickness = 0.5.dp)

                    // Minimalist Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Minimalista", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Simplifica los menús y acciones", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isMinimalistMode,
                            onCheckedChange = { checked ->
                                com.example.ui.theme.ThemeManager.isMinimalistMode.value = checked
                                prefs.edit().putBoolean("minimalist_mode_global", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E2E36)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E2E36), thickness = 0.5.dp)

                    // Floating PiP Toggle Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ventanas Flotantes (PiP)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Permitir que Pana TV y Reels floten al salir", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isFloatingPipEnabled,
                            onCheckedChange = { checked ->
                                isFloatingPipEnabled = checked
                                prefs.edit().putBoolean("floating_pip_enabled", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E2E36)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E2E36), thickness = 0.5.dp)

                    // Logout Button
                    Button(
                        onClick = {
                            scope.launch {
                                authViewModel.logout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión de Pana", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Editar Nombre de Pana", color = Color.White) },
            containerColor = Color(0xFF121214),
            text = {
                OutlinedTextField(
                    value = nameInputText,
                    onValueChange = { nameInputText = it },
                    label = { Text("Nombre Completo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInputText.isNotBlank()) {
                            profileViewModel.saveProfile(nameInputText, avatarUrl)
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("Guardar", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }


}




@Composable
fun InicioTabContent(
    statesState: StatesUiState,
    statesViewModel: StatesViewModel,
    onNavigateToViewState: (String) -> Unit,
    onNavigateToCreateState: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    feedViewModel: com.example.ui.viewmodel.FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val feedUiState by feedViewModel.uiState.collectAsState()
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<com.example.data.model.PostDto?>(null) }
    var editingPostId by remember { mutableStateOf<String?>(null) }
    var editingPostContent by remember { mutableStateOf("") }
    
    var fullScreenMediaList by remember { mutableStateOf<List<String>?>(null) }
    var fullScreenInitialPage by remember { mutableIntStateOf(0) }
    var fullScreenBackgroundAudio by remember { mutableStateOf<String?>(null) }
    var postToDeleteId by remember { mutableStateOf<String?>(null) }
    var activePlaylistPost by remember { mutableStateOf<com.example.data.model.PostDto?>(null) }

    // Comments Bottom Sheet State
    var showCommentsSheet by remember { mutableStateOf(false) }
    var activeCommentStateId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)


    // Auto-refresh when user session becomes available
    LaunchedEffect(com.example.data.supabase.SupabaseClient.currentUser) {
        if (com.example.data.supabase.SupabaseClient.currentUser != null) {
            if (feedUiState.posts.isEmpty()) feedViewModel.refreshFeed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PanalinkPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    statesViewModel.loadActiveStates()
                    feedViewModel.refreshFeed()
                    kotlinx.coroutines.delay(1200)
                    isRefreshing = false
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Historias 🇻🇪✨",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // "Mi Historia" card
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(150.dp)
                                        .clickable { onNavigateToCreateState() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = SupabaseClient.currentProfile?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                            contentDescription = "Mi Avatar",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(0.7f),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Bottom portion
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(0.3f)
                                                .align(Alignment.BottomCenter)
                                                .background(Color(0xFF161618))
                                        ) {
                                            Text(
                                                "Tu historia",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                                            )
                                        }
                                        
                                        // Add icon overlapping the middle
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = (-20).dp)
                                                .size(28.dp)
                                                .background(Color(0xFFD500F9), CircleShape)
                                                .border(2.dp, Color(0xFF161618), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            // Contacts' stories
                            if (statesState is StatesUiState.Success) {
                                val uniqueUserStories = statesState.states.distinctBy { it.state.userId }
                                items(uniqueUserStories, key = { it.state.userId }) { stateWithUser ->
                                    val firstState = statesState.states.firstOrNull { it.state.userId == stateWithUser.state.userId } ?: stateWithUser
                                    val profile = stateWithUser.profile
                                    val state = firstState.state
                                    val userStories = statesState.states.filter { it.state.userId == stateWithUser.state.userId }
                                    val hasUnread = userStories.any { it.state.viewedByMe != true }
                                    
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
                                    val identityState by identityRepository.observeIdentity(state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(state.userId)?.toIdentityUiState())
                                    val safeAvatarUrl = identityState?.avatarUrl ?: profile.avatarUrl
                                    val safeDisplayName = identityState?.displayName ?: profile.displayName
                                    val safeUserId = identityState?.userId ?: profile.id

                                    Card(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(150.dp)
                                            .clickable { onNavigateToViewState(state.id) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // Background
                                            AsyncImage(
                                                model = state.mediaUrl ?: safeAvatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                                contentDescription = safeDisplayName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            // Gradient overlay for text readability
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                            startY = 100f
                                                        )
                                                    )
                                            )

                                            // Profile picture top left
                                            com.example.ui.components.PanaAvatar(
                                                avatarUrl = safeAvatarUrl,
                                                userId = safeUserId,
                                                size = 32.dp,
                                                borderWidth = 2.dp,
                                                borderColor = if (hasUnread) Color(0xFFB026FF) else Color.Gray.copy(alpha = 0.5f),
                                                placeholderName = safeDisplayName,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                            )

                                            // Username bottom
                                            Text(
                                                text = safeDisplayName?.take(15) ?: "",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (statesState is StatesUiState.Loading) {
                                items(4) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .shimmerEffect()
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF121214), thickness = 1.dp)
                }

                item {
                }

                // Header for Feed Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "El Muro 💬",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showCreatePostSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = "Publicar",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                // Pending Upload Posts
                itemsIndexed(feedUiState.pendingPosts, key = { index, pending -> "pending_${pending.id}_$index" }) { _, pending ->
                    PendingPostCard(pending)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Feed Posts from FeedViewModel (PostDto: TEXT, ALBUM, AUDIO)
                if (feedUiState.posts.isNotEmpty()) {
                    itemsIndexed(feedUiState.posts, key = { index, post -> "feed_post_${post.id ?: post.hashCode()}_$index" }) { index, post ->
                        LaunchedEffect(index) {
                            com.example.media.feed.FeedMediaPreloader.preloadNextPostsMedia(context, feedUiState.posts, index, scope)
                        }
                        FeedPostCard(
                            post = post,
                            onLikeClick = { feedViewModel.toggleLike(post) },
                            onShareClick = { feedViewModel.sharePost(post) },
                            onCommentClick = { selectedPostForComments = post },
                            onProfileClick = { /* Profile click */ },
                            onDeleteClick = { postToDeleteId = post.id },
                            onEditClick = { content ->
                                editingPostId = post.id
                                editingPostContent = content
                            },
                            onMediaClick = { list, page, audio ->
                                fullScreenMediaList = list
                                fullScreenInitialPage = page
                                fullScreenBackgroundAudio = audio
                            },
                            onAudioPlaylistClick = { activePlaylistPost = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Fallback or additional 24h Stories feed posts
                if (feedUiState.posts.isEmpty() && !feedUiState.isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "El muro está vacío por ahora",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "¡Sé el primero en compartir algo!",
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (feedUiState.isLoading && feedUiState.posts.isEmpty()) {
                    items(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Floating Action Button to create a post on El Muro
        FloatingActionButton(
            onClick = { showCreatePostSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp),
            containerColor = Color(0xFF00E5FF),
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Crear Publicación")
        }
    }

    if (showCreatePostSheet) {
        com.example.ui.screen.CreatePostBottomSheet(
            onDismiss = {
                showCreatePostSheet = false
                feedViewModel.refreshFeed()
            }
        )
    }

    if (showCommentsSheet && activeCommentStateId != null) {
        val commentsList by statesViewModel.currentComments.collectAsState()
        val keyboardController = LocalSoftwareKeyboardController.current
        
        LaunchedEffect(activeCommentStateId) {
            statesViewModel.loadComments(activeCommentStateId!!)
        }

        ModalBottomSheet(
            onDismissRequest = {
                showCommentsSheet = false
                activeCommentStateId = null
            },
            sheetState = sheetState,
            containerColor = Color(0xFF161618)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f) 
                    .padding(horizontal = 16.dp)
                    .imePadding() // Ensures content is pushed up by keyboard
            ) {
                Text(
                    "Comentarios",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Comments List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        item {
                            Text("No hay comentarios aún. Sé el primero.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        itemsIndexed(commentsList, key = { index, comment -> "${comment.id}_$index" }) { _, comment ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                com.example.ui.components.PanaAvatar(
                                    avatarUrl = comment.avatarUrl,
                                    userId = comment.userId,
                                    size = 36.dp,
                                    borderWidth = 0.dp,
                                    placeholderName = comment.authorName
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(comment.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(comment.text, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Responder", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable {
                                        // Simple reply implementation
                                    })
                                }
                            }
                        }
                    }
                }

                // Comment Input
                var commentText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Añade un comentario...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF262629),
                            unfocusedContainerColor = Color(0xFF262629),
                            focusedBorderColor = Color(0xFF00FF85),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                statesViewModel.addComment(activeCommentStateId!!, commentText, onError = { err ->
                                    android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                })
                                commentText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(48.dp).background(Color(0xFFB026FF), CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    selectedPostForComments?.let { post ->
        com.example.ui.screen.FeedCommentsBottomSheet(
            postId = post.id ?: "",
            onDismiss = { selectedPostForComments = null },
            viewModel = feedViewModel
        )
    }

    if (editingPostId != null) {
        AlertDialog(
            onDismissRequest = { editingPostId = null },
            title = { Text("Editar publicación", color = Color.White) },
            text = {
                TextField(
                    value = editingPostContent,
                    onValueChange = { editingPostContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editingPostId?.let { feedViewModel.updatePost(it, editingPostContent) }
                    editingPostId = null
                }) {
                    Text("Guardar", color = Color(0xFF00E5FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPostId = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E222B)
        )
    }

    if (postToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { postToDeleteId = null },
            title = { Text("Eliminar publicación", color = Color.White) },
            text = { Text("¿Estás seguro de que quieres eliminar esta publicación? Esta acción no se puede deshacer.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    postToDeleteId?.let { feedViewModel.deletePost(it) }
                    postToDeleteId = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDeleteId = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E222B)
        )
    }

    // --- FULL SCREEN MEDIA VIEWER ---
    if (fullScreenMediaList != null) {
        val mediaList = fullScreenMediaList!!
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = fullScreenInitialPage,
            pageCount = { mediaList.size }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(enabled = false) { /* intercept clicks */ }
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val mediaUrl = mediaList[page]
                val isVideo = com.example.ui.components.isVideoUrl(mediaUrl) || mediaUrl.contains("video")
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        com.example.ui.components.SimpleVideoPreviewPlayer(
                            videoUri = Uri.parse(mediaUrl),
                            isMuted = false,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)
                        )
                    } else {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Pantalla completa",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { fullScreenMediaList = null },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                
                if (mediaList.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${mediaList.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        val currentUrl = mediaList[pagerState.currentPage]
                        try {
                            val uri = Uri.parse(currentUrl)
                            val request = android.app.DownloadManager.Request(uri).apply {
                                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                val fileName = currentUrl.substringAfterLast("/")
                                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                                setTitle("Descargando archivo")
                                setDescription(fileName)
                            }
                            val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            manager.enqueue(request)
                            android.widget.Toast.makeText(context, "Descarga iniciada... 📥", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Descargar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // --- SEQUENTIAL AUDIO PLAYLIST PLAYER MODAL ---
    if (activePlaylistPost != null) {
        val post = activePlaylistPost!!
        val audiosList = remember(post) { 
            (post.mediaUrls ?: emptyList()).filter { it.isNotBlank() && (it.contains("audio") || com.example.ui.components.isAudioUrl(it)) } 
        }
        
        var currentAudioIndex by remember { mutableIntStateOf(0) }
        var isPlaying by remember { mutableStateOf(false) }
        var playbackPosition by remember { mutableLongStateOf(0L) }
        var audioDuration by remember { mutableLongStateOf(0L) }
        
        val exoPlayer = remember(context) { androidx.media3.exoplayer.ExoPlayer.Builder(context).build() }
        
        DisposableEffect(exoPlayer) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_READY) {
                        audioDuration = exoPlayer.duration
                    } else if (state == androidx.media3.common.Player.STATE_ENDED) {
                        if (currentAudioIndex + 1 < audiosList.size) {
                            currentAudioIndex += 1
                        } else {
                            isPlaying = false
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
        
        LaunchedEffect(currentAudioIndex, audiosList) {
            if (audiosList.isNotEmpty()) {
                val url = audiosList[currentAudioIndex]
                val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.parse(url))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = isPlaying
            }
        }
        
        LaunchedEffect(isPlaying, currentAudioIndex) {
            while (isPlaying) {
                playbackPosition = exoPlayer.currentPosition
                kotlinx.coroutines.delay(500)
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { 
                exoPlayer.stop()
                activePlaylistPost = null 
            },
            containerColor = Color(0xFF0F172A),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reproductor de Audios (${audiosList.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        exoPlayer.stop()
                        activePlaylistPost = null 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(audiosList) { index, url ->
                        val isCurrent = index == currentAudioIndex
                        val itemBgColor = if (isCurrent) Color(0xFF1E293B) else Color(0xFF1E1E24)
                        val itemBorderColor = if (isCurrent) Color(0xFF00E5FF) else Color.Transparent
                        
                        Card(
                            onClick = {
                                currentAudioIndex = index
                                isPlaying = true
                                exoPlayer.play()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = itemBgColor),
                            border = BorderStroke(1.dp, itemBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Audio ${index + 1}",
                                            color = Color.White,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Panalink Audio File",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        try {
                                            val uri = Uri.parse(url)
                                            val request = android.app.DownloadManager.Request(uri).apply {
                                                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                val fileName = url.substringAfterLast("/")
                                                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                                                setTitle("Descargando Audio ${index + 1}")
                                                setDescription(fileName)
                                            }
                                            val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                            manager.enqueue(request)
                                            android.widget.Toast.makeText(context, "Descarga iniciada para Audio ${index + 1} 📥", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Descargar",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Reproduciendo: Audio ${currentAudioIndex + 1}",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val progress = if (audioDuration > 0) playbackPosition.toFloat() / audioDuration.toFloat() else 0f
                        Slider(
                            value = progress,
                            onValueChange = { 
                                val pos = (it * audioDuration).toLong()
                                exoPlayer.seekTo(pos)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.Gray
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val curMin = (playbackPosition / 1000) / 60
                            val curSec = (playbackPosition / 1000) % 60
                            val durMin = (audioDuration / 1000) / 60
                            val durSec = (audioDuration / 1000) % 60
                            Text(String.format("%02d:%02d", curMin, curSec), color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%02d:%02d", durMin, durSec), color = Color.Gray, fontSize = 11.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentAudioIndex > 0) {
                                        currentAudioIndex -= 1
                                    }
                                },
                                enabled = currentAudioIndex > 0
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = if (currentAudioIndex > 0) Color.White else Color.Gray, modifier = Modifier.size(36.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(24.dp))
                            
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    isPlaying = !isPlaying
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF00E5FF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(24.dp))
                            
                            IconButton(
                                onClick = {
                                    if (currentAudioIndex + 1 < audiosList.size) {
                                        currentAudioIndex += 1
                                    }
                                },
                                enabled = currentAudioIndex + 1 < audiosList.size
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = if (currentAudioIndex + 1 < audiosList.size) Color.White else Color.Gray, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

