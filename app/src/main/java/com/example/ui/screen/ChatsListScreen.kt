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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun SelectionTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onPinClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onMuteClicked: () -> Unit,
    onArchiveClicked: () -> Unit,
    onMarkReadClicked: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onRestrictClicked: () -> Unit,
    onAddToFavoritesClicked: () -> Unit,
    onAddToListClicked: () -> Unit,
    onClearChatsClicked: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF005E54)) // Darker elegant WhatsApp teal for selection mode
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Cancelar selección",
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = selectedCount.toString(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(onClick = onPinClicked) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Fijar chat",
                tint = Color.White
            )
        }
        
        IconButton(onClick = onDeleteClicked) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar chat",
                tint = Color.White
            )
        }
        
        IconButton(onClick = onMuteClicked) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = "Silenciar chat",
                tint = Color.White
            )
        }
        
        IconButton(onClick = onArchiveClicked) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = "Archivar chat",
                tint = Color.White
            )
        }
        
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones avanzadas",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1F2C34))
            ) {
                DropdownMenuItem(
                    text = { Text("Marcar como no leído / leído", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onMarkReadClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Seleccionar todos", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onSelectAllClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Restringir chats", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onRestrictClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Añadir a Favoritos", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onAddToFavoritesClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Añadir a lista", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onAddToListClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Vaciar chats", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onClearChatsClicked()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

@Composable
fun ChatsListScreen(
    chatsViewModel: ChatsViewModel,
    statesViewModel: StatesViewModel,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    notificationsViewModel: NotificationsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    pendingUploadsViewModel: com.example.ui.viewmodel.PendingUploadsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToChat: (String, String) -> Unit, // chatId, otherUserId
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateState: () -> Unit = {},
    onNavigateToCreateStory: () -> Unit = {},
    onNavigateToCreateReel: () -> Unit = {},
    onNavigateToViewState: (String) -> Unit, // stateId
    onNavigateToTikTok: (String) -> Unit, // stateId
    onNavigateToProfile: () -> Unit,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var deletedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var pinnedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var mutedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var archivedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var customUnreadCounts by remember { mutableStateOf(emptyMap<String, Int>()) }

    androidx.activity.compose.BackHandler(enabled = selectedChatIds.isNotEmpty()) {
        selectedChatIds = emptySet()
    }

    val tabNavController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val chatsState by chatsViewModel.chatsState.collectAsStateWithLifecycle()
    val typingChats by chatsViewModel.typingChats.collectAsStateWithLifecycle()
    val favoriteChatIds by chatsViewModel.favoriteChatIds.collectAsStateWithLifecycle()
    val restrictedChatIds by chatsViewModel.restrictedChatIds.collectAsStateWithLifecycle()
    val chatListsMap by chatsViewModel.chatListsMap.collectAsStateWithLifecycle()
    val contactsState by chatsViewModel.contactsState.collectAsStateWithLifecycle()
    val statesState by statesViewModel.statesState.collectAsStateWithLifecycle()
    val storiesState by statesViewModel.storiesState.collectAsStateWithLifecycle()
    val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsStateWithLifecycle()
    
    val profileUiState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val currentProfile = (profileUiState as? com.example.ui.viewmodel.ProfileUiState.Success)?.profile ?: SupabaseClient.currentProfile
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddToListDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Profile?>(null) }
    var showRealQrScanner by remember { mutableStateOf(false) }
    val colors = com.example.ui.theme.LocalAppColors.current
    val addContactState by chatsViewModel.addContactState.collectAsStateWithLifecycle()

    if (showAddToListDialog) {
        var selectedOption by remember { mutableStateOf("🏢 Trabajo") }
        var isCustomSelected by remember { mutableStateOf(false) }
        var customName by remember { mutableStateOf("") }
        val presetLists = listOf("🏢 Trabajo", "👨‍👩‍👧‍👦 Familia", "🤝 Panas", "📌 Personal", "⭐ Importante")

        AlertDialog(
            onDismissRequest = { showAddToListDialog = false },
            title = { Text("Añadir a lista de chats 📝", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona una lista existente o crea una nueva:", color = Color.LightGray, fontSize = 14.sp)
                    presetLists.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedOption = preset
                                    isCustomSelected = false
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (!isCustomSelected && selectedOption == preset),
                                onClick = {
                                    selectedOption = preset
                                    isCustomSelected = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00A884))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preset, color = Color.White, fontSize = 16.sp)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustomSelected = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustomSelected,
                            onClick = { isCustomSelected = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00A884))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✏️ Crear nueva lista...", color = Color.White, fontSize = 16.sp)
                    }
                    if (isCustomSelected) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            placeholder = { Text("Nombre de la lista", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A884),
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalListName = if (isCustomSelected && customName.isNotBlank()) customName.trim() else selectedOption
                        val count = selectedChatIds.size
                        chatsViewModel.addChatsToList(selectedChatIds, finalListName)
                        android.widget.Toast.makeText(context, "Añadido(s) $count chat(s) a la lista '$finalListName' 📝", android.widget.Toast.LENGTH_SHORT).show()
                        selectedChatIds = emptySet()
                        showAddToListDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                ) {
                    Text("Añadir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToListDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1F2C34)
        )
    }

    if (contactToDelete != null) {
        ModalBottomSheet(
            onDismissRequest = { contactToDelete = null },
            containerColor = Color(0xFF161618)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("¿Eliminar a ${contactToDelete!!.displayName}?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val contact = contactToDelete!!
                        contactToDelete = null
                        chatsViewModel.deleteContact(contact)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Contacto eliminado",
                                actionLabel = "Deshacer"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                chatsViewModel.undoDeleteContact(contact)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar")
                }
            }
        }
    }

    var scanResultPin by remember { mutableStateOf<String?>(null) }

    var showPlusBottomSheet by remember { mutableStateOf(false) }
    val feedViewModel: com.example.ui.viewmodel.FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var showContactsDialog by remember { mutableStateOf(false) }
    var showCallsDialog by remember { mutableStateOf(false) }
    var showQuickProfileDialog by remember { mutableStateOf(false) }

    // Cerebro Universal Search & Command Palette States
    var showCerebroOverlay by remember { mutableStateOf(false) }
    var cerebroSearchQuery by remember { mutableStateOf("") }
    val isMinimalistMode by com.example.ui.theme.ThemeManager.isMinimalistMode.collectAsState()
    var isSelectingContactOnly by remember { mutableStateOf(false) }
    var isEarthquakeActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeX by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(40, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeX"
    )
    val shakeY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(35, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeY"
    )
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(45, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeRotation"
    )

    val barcodeLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            val scannedContent = result.contents
            val pin = if (scannedContent.startsWith("panalink:pin:")) {
                scannedContent.substringAfter("panalink:pin:")
            } else {
                scannedContent
            }
            val cleanPin = pin.trim().filter { it.isDigit() }.take(6)
            if (cleanPin.length == 6) {
                chatsViewModel.addContactByPin(cleanPin)
            } else {
                android.widget.Toast.makeText(context, "Código QR inválido o sin PIN de 6 dígitos: $scannedContent", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(addContactState) {
        val state = addContactState
        if (state is AddContactUiState.Success) {
            chatsViewModel.resetAddContactState()
            showAddContactDialog = false
            showRealQrScanner = false
            
            val contactName = state.response.displayName.ifBlank { "Contacto" }
            val toastMsg = if (state.response.isAlreadyContact == true) {
                "Ya $contactName es de tus contactos"
            } else {
                "Solicitud enviada a $contactName: si acepta, será contacto mutuo"
            }
            android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
            
            chatsViewModel.loadContacts(forceRefresh = true)
            chatsViewModel.loadFriendRequests()
            chatsViewModel.loadSentFriendRequests()
            chatsViewModel.loadChats()

            val threadId = state.response.threadId
            val contactId = state.response.contactId
            if (threadId.isNotEmpty() && contactId.isNotEmpty() && threadId != contactId && threadId.length == 36 && contactId.length == 36) {
                onNavigateToChat(threadId, contactId)
            }
        } else if (state is AddContactUiState.Error) {
            val err = state.message
            if (err.contains("ya es un contacto", ignoreCase = true) || err.contains("ya existe", ignoreCase = true)) {
                chatsViewModel.resetAddContactState()
                showAddContactDialog = false
                showRealQrScanner = false
                android.widget.Toast.makeText(context, "Ya es de tus contactos", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Reload data on active screen entry
    LaunchedEffect(Unit) {
        chatsViewModel.loadChats()
        chatsViewModel.loadContacts()
        chatsViewModel.loadFriendRequests()
        chatsViewModel.loadSentFriendRequests()
        statesViewModel.loadActiveStates()
    }

    LaunchedEffect(showContactsDialog) {
        if (!showContactsDialog) {
            isSelectingContactOnly = false
        }
    }

    // observer for social uploads to refresh states
    val workManager = androidx.work.WorkManager.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val uploadInfos by workManager.getWorkInfosByTagLiveData("social_upload")
        .observeAsState(initial = emptyList())

    LaunchedEffect(uploadInfos) {
        val newlyCompleted = uploadInfos.filter { it.state == androidx.work.WorkInfo.State.SUCCEEDED }
        if (newlyCompleted.isNotEmpty()) {
            statesViewModel.loadActiveStates()
        }
    }

    val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "chats"
    val currentPageIndex = when (currentRoute) {
        "chats" -> 0
        "momentos" -> 1
        "clips" -> 2
        "llamadas" -> 3
        "gente" -> 4
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (isEarthquakeActive) {
                    translationX = shakeX.dp.toPx()
                    translationY = shakeY.dp.toPx()
                    rotationZ = shakeRotation
                }
            }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectedChatIds.isNotEmpty()) {
                    SelectionTopAppBar(
                        selectedCount = selectedChatIds.size,
                        onClearSelection = { selectedChatIds = emptySet() },
                        onPinClicked = {
                            val currentChats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
                            val selectedChats = currentChats.filter { selectedChatIds.contains(it.chat.id) }
                            val allPinned = selectedChats.isNotEmpty() && selectedChats.all { it.chat.isPinned }
                            val newPinState = !allPinned
                            val count = selectedChatIds.size
                            selectedChatIds.forEach { cid ->
                                chatsViewModel.pinChat(cid, newPinState)
                            }
                            val msg = if (newPinState) "Anclado(s) $count chat(s) con éxito 📌" else "Desanclado(s) $count chat(s) 📌"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onDeleteClicked = {
                            val count = selectedChatIds.size
                            chatsViewModel.deleteChats(selectedChatIds)
                            deletedChatIds = deletedChatIds + selectedChatIds
                            android.widget.Toast.makeText(context, "Eliminado(s) $count chat(s) con éxito 🗑️", android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onMuteClicked = {
                            val currentChats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
                            val selectedChats = currentChats.filter { selectedChatIds.contains(it.chat.id) }
                            val allMuted = selectedChats.isNotEmpty() && selectedChats.all { it.chat.isMuted }
                            val newMuteState = !allMuted
                            val count = selectedChatIds.size
                            selectedChatIds.forEach { cid ->
                                chatsViewModel.muteChat(cid, newMuteState)
                            }
                            val msg = if (newMuteState) "Silenciado(s) $count chat(s) con éxito 🔇" else "Notificaciones activadas para $count chat(s) 🔔"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onArchiveClicked = {
                            val currentChats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
                            val selectedChats = currentChats.filter { selectedChatIds.contains(it.chat.id) }
                            val allArchived = selectedChats.isNotEmpty() && selectedChats.all { it.chat.isArchived }
                            val newArchiveState = !allArchived
                            val count = selectedChatIds.size
                            selectedChatIds.forEach { chatId ->
                                chatsViewModel.archiveChat(chatId, newArchiveState)
                            }
                            val msg = if (newArchiveState) "Archivado(s) $count chat(s) con éxito 📥" else "Desarchivado(s) $count chat(s) 📤"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onMarkReadClicked = {
                            val currentChats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
                            val selectedChats = currentChats.filter { selectedChatIds.contains(it.chat.id) }
                            val allRead = selectedChats.all { it.unreadCount == 0 }
                            val count = selectedChatIds.size
                            selectedChatIds.forEach { id ->
                                if (allRead) {
                                    chatsViewModel.markChatAsUnread(id)
                                } else {
                                    chatsViewModel.markChatAsRead(id)
                                }
                            }
                            val msg = if (allRead) "Marcado(s) como no leído para $count chat(s) 💬" else "Marcado(s) como leído para $count chat(s) 👁️"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onSelectAllClicked = {
                            val currentChats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
                            val visibleChats = currentChats.filterNot { deletedChatIds.contains(it.chat.id) || it.chat.isArchived }
                            selectedChatIds = visibleChats.map { it.chat.id }.toSet()
                        },
                        onRestrictClicked = {
                            val allRestricted = selectedChatIds.all { restrictedChatIds.contains(it) }
                            val newRestrictState = !allRestricted
                            val count = selectedChatIds.size
                            chatsViewModel.toggleRestrictChats(selectedChatIds, newRestrictState)
                            val msg = if (newRestrictState) "Restringido(s) $count chat(s) con éxito 🔒" else "Restricción removida para $count chat(s) 🔓"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onAddToFavoritesClicked = {
                            val allFavorites = selectedChatIds.all { favoriteChatIds.contains(it) }
                            val newFavoriteState = !allFavorites
                            val count = selectedChatIds.size
                            chatsViewModel.toggleFavoriteChats(selectedChatIds, newFavoriteState)
                            val msg = if (newFavoriteState) "Añadido(s) $count chat(s) a Favoritos ⭐️" else "Removido(s) $count chat(s) de Favoritos ⭐"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        },
                        onAddToListClicked = {
                            showAddToListDialog = true
                        },
                        onClearChatsClicked = {
                            val count = selectedChatIds.size
                            chatsViewModel.clearChats(selectedChatIds)
                            android.widget.Toast.makeText(context, "Conversaciones vaciadas para $count chat(s) 🧼", android.widget.Toast.LENGTH_SHORT).show()
                            selectedChatIds = emptySet()
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Row 1: Panalink title + Icons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PanaLink",
                                style = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Create Button (Plus Icon)
                                IconButton(
                                    onClick = { showPlusBottomSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddBox,
                                        contentDescription = "Crear",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // Search Icon
                                IconButton(
                                    onClick = onNavigateToSearch,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar Panas",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // Saved Messages / Favorites Icon
                                IconButton(
                                    onClick = onNavigateToFavorites,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Mensajes guardados",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                                     // Notifications Bell
                                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    val unreadCount by notificationsViewModel.unreadCount.collectAsState(0)
                                    IconButton(
                                        onClick = onNavigateToNotifications,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Notificaciones",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    // Badge
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 6.dp)
                                                .size(16.dp)
                                                .background(Color(0xFFFF1744), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                color = Color.White, 
                                                fontSize = 9.sp, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigateToProfile() }
                            ) {
                                com.example.ui.components.PanaAvatar(
                                    avatarUrl = SupabaseClient.currentProfile?.avatarUrl,
                                    userId = SupabaseClient.currentUser?.id,
                                    size = 32.dp,
                                    borderWidth = 1.5.dp,
                                    borderColor = Color(0xFFB026FF),
                                    placeholderName = SupabaseClient.currentProfile?.displayName ?: "",
                                    contentDescription = "Perfil"
                                )
                                val myPresence by com.example.data.repository.PresenceRepository.currentUserStatus.collectAsStateWithLifecycle()
                                val mySecondaryPresence by com.example.data.repository.PresenceRepository.currentUserSecondaryStatus.collectAsStateWithLifecycle()
                                com.example.ui.components.chat.list.PresenceIndicator(
                                    status = myPresence.rawValue,
                                    secondaryStatus = if (mySecondaryPresence != com.example.data.repository.SecondaryPresenceStatus.NONE) mySecondaryPresence.rawValue else null,
                                    size = 10.dp,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentPageIndex != 2,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    val totalUnreadCount = remember(chatsState) {
                        (chatsState as? ChatsUiState.Success)?.chats?.sumOf { it.unreadCount } ?: 0
                    }
                    com.example.ui.components.PanaLinkFloatingBottomBar(
                        currentPage = currentPageIndex,
                        onPageSelected = { page ->
                            isSelectingContactOnly = false
                            val route = when (page) {
                                0 -> "chats"
                                1 -> "momentos"
                                2 -> "clips"
                                3 -> "llamadas"
                                4 -> "gente"
                                else -> "chats"
                            }
                            tabNavController.navigate(route) {
                                popUpTo(tabNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        totalUnreadCount = totalUnreadCount
                    )
                }
            },
            containerColor = colors.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PendingUploadsBanner(pendingUploadsViewModel = pendingUploadsViewModel)

                
                NavHost(
                    navController = tabNavController,
                    startDestination = "chats",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { androidx.compose.animation.fadeIn(animationSpec = tween(200)) },
                    exitTransition = { androidx.compose.animation.fadeOut(animationSpec = tween(200)) }
                ) {
                    composable("chats") {
 ChatsTabContent(
                            chatsState = chatsState,
                            typingChats = typingChats,
                            contactsState = contactsState,
                            statesState = storiesState,
                            chatsViewModel = chatsViewModel,
                            onNavigateToChat = onNavigateToChat,
                            onNavigateToViewState = onNavigateToViewState,
                            onNavigateToCreateState = onNavigateToCreateState,
                            onRefresh = {
                                chatsViewModel.loadChats(forceRefresh = true)
                                chatsViewModel.loadContacts(forceRefresh = true)
                                statesViewModel.loadActiveStates()
                            },
                            selectedChatIds = selectedChatIds,
                            onToggleChatSelection = { id ->
                                if (selectedChatIds.contains(id)) {
                                    selectedChatIds = selectedChatIds - id
                                } else {
                                    selectedChatIds = selectedChatIds + id
                                }
                            },
                            onStartChatSelection = { id ->
                                selectedChatIds = setOf(id)
                            },
                            deletedChatIds = deletedChatIds,
                            pinnedChatIds = pinnedChatIds,
                            mutedChatIds = mutedChatIds,
                            customUnreadCounts = customUnreadCounts
                        )
                        }
                    composable("momentos") { InicioTabContent(
                            statesState = storiesState,
                            statesViewModel = statesViewModel,
                            onNavigateToViewState = onNavigateToViewState,
                            onNavigateToCreateState = onNavigateToCreateState,
                            onNavigateToChat = onNavigateToChat
                        )
                        }
                    composable("clips") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                com.example.ui.screen.TikTokVideoFeedScreen(
                                    viewModel = statesViewModel,
                                    initialStateId = "",
                                    isActive = currentPageIndex == 2,
                                    onBack = {
                                        tabNavController.navigate("chats") { popUpTo(tabNavController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                                    },
                                    onNavigateToUserProfile = onNavigateToUserProfile
                                )
                            }
                        }
                    composable("llamadas") { LlamadasTabContent(
                            contactsState = contactsState,
                            onRefresh = {
                                chatsViewModel.loadContacts(forceRefresh = true)
                            }
                        )
                        }
                    composable("gente") { ContactsTabContent(
                            contactsState = contactsState,
                            chatsViewModel = chatsViewModel,
                            isSelectingContactOnly = isSelectingContactOnly,
                            onNavigateToChat = { chatId, otherUserId ->
                                onNavigateToChat(chatId, otherUserId)
                            },
                            onRefresh = {
                                chatsViewModel.loadContacts(forceRefresh = true)
                            },
                            onContactLongClick = { contact ->
                                contactToDelete = contact
                            }
                        )
                    }
                }
            }
        }
    }

    // --- FASE 2: MODERN OVERLAY DIALOGS ---

    // 1. Plus Bottom Sheet
    androidx.compose.animation.AnimatedVisibility(
        visible = showPlusBottomSheet,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { showPlusBottomSheet = false }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // prevent click propagating through sheet
                    .background(
                        color = Color(0xFF0D0D0F),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .border(1.dp, Color(0xFF262629), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top Header
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showPlusBottomSheet = false },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                        Text(
                            text = "Crear",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¿Qué deseas crear?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2 Big Cards: Historia, Reel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Historia Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPlusBottomSheet = false
                                    onNavigateToCreateStory()
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Historia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Comparte momentos\nque desaparecen en\n24 horas.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
                            }
                        }

                        // Reel Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPlusBottomSheet = false
                                    onNavigateToCreateReel()
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFFE65100), Color(0xFFFF9800))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Reel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Crea videos para\ndescubrir y compartir\ncon el mundo.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Más opciones", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Music Studio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
                                onNavigateToMusic()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF38BDF8), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Music Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Administra tus playlists y biblioteca musical.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }

                    // PanaTV
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
                                val intent = android.content.Intent(context, com.example.panatv.PanaTVActivity::class.java)
                                context.startActivity(intent)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE91E63), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PanaTV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Canales nacionales en vivo", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    // Buscar Panas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
                                onNavigateToSearch()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF5E35B1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Buscar Panas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Busca otros usuarios de Panalink por nombre.", color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }

                    // Directorio de Panas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
                                showContactsDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFC62828), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContactPage, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Directorio de Panas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Mira tu lista completa de contactos enlazados.", color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }

                    // Canal (próximamente: aún no hay backend de canales)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.widget.Toast.makeText(context, "Canales: próximamente 📢", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF00B8D4), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Canal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Crea una audiencia y comparte\ndifusiones públicas o privadas.", color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Próximamente", color = Color(0xFFFFB300), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Sala de voz (próximamente: audio rooms en vivo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.widget.Toast.makeText(context, "Salas de voz: próximamente 🎙️", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF7C4DFF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sala de voz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Conversa en vivo con tus panas\nen salas de audio.", color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Próximamente", color = Color(0xFFFFB300), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Accesos rápidos", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Llamadas quick access
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable {
                                    showPlusBottomSheet = false
                                    showCallsDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Llamadas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Escanear QR quick access
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable {
                                    showPlusBottomSheet = false
                                    showRealQrScanner = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escanear QR", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Contacts Full-Screen Dialog
    if (showContactsDialog) {
        androidx.compose.ui.window.Dialog(
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showContactsDialog = false }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Directorio de Panas 👥", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { showContactsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showRealQrScanner = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR", tint = Color.White)
                            }
                            IconButton(onClick = { showAddContactDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Agregar Pana", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                },
                containerColor = Color.Black
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ContactsTabContent(
                        contactsState = contactsState,
                        chatsViewModel = chatsViewModel,
                        isSelectingContactOnly = isSelectingContactOnly,
                        onNavigateToChat = { chatId, otherUserId ->
                            showContactsDialog = false
                            onNavigateToChat(chatId, otherUserId)
                        },
                        onRefresh = {
                            chatsViewModel.loadContacts(forceRefresh = true)
                        },
                        onContactLongClick = { contact ->
                            contactToDelete = contact
                        }
                    )
                }
            }
        }
    }

    // 3. Calls Full-Screen Dialog
    if (showCallsDialog) {
        androidx.compose.ui.window.Dialog(
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showCallsDialog = false }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Historial de Llamadas 📞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { showCallsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                },
                containerColor = Color.Black
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    LlamadasTabContent(
                        contactsState = contactsState,
                        onRefresh = {
                            chatsViewModel.loadContacts(forceRefresh = true)
                        }
                    )
                }
            }
        }
    }

    // 4. Quick Profile Dialog
    if (showQuickProfileDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showQuickProfileDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                border = BorderStroke(1.dp, Color(0xFF262629)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Header
                    com.example.ui.components.PanaAvatar(
    avatarUrl = SupabaseClient.currentProfile?.avatarUrl,
    userId = SupabaseClient.currentUser?.id,
    size = 72.dp,
    borderColor = colors.accent,
    borderWidth = 2.dp,
    placeholderName = SupabaseClient.currentProfile?.displayName
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = SupabaseClient.currentProfile?.displayName ?: "Mi Cuenta",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = SupabaseClient.currentUser?.email ?: "email@domain.com",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = Color(0xFF262629), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Menu Item: Editar Perfil
                    QuickProfileMenuItem(
                        icon = Icons.Default.Person,
                        label = "Editar perfil",
                        tint = Color.White
                    ) {
                        showQuickProfileDialog = false
                        onNavigateToProfile()
                    }

                    // Menu Item: Configuración
                    QuickProfileMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Configuración",
                        tint = colors.accent
                    ) {
                        showQuickProfileDialog = false
                        onNavigateToProfile()
                    }

                    // Menu Item: Modo oscuro
                    val isMinimal by com.example.ui.theme.ThemeManager.isMinimalistMode.collectAsState()
                    QuickProfileMenuItem(
                        icon = if (isMinimal) Icons.Default.Check else Icons.Default.Close,
                        label = "Modo minimalista",
                        tint = Color(0xFF00FF85)
                    ) {
                        val newVal = !isMinimal
                        com.example.ui.theme.ThemeManager.isMinimalistMode.value = newVal
                        val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("minimalist_mode_global", newVal).apply()
                    }

                    // Menu Item: Cerrar sesión
                    QuickProfileMenuItem(
                        icon = Icons.Default.ExitToApp,
                        label = "Cerrar sesión",
                        tint = Color.Red
                    ) {
                        showQuickProfileDialog = false
                        authViewModel.logout()
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showQuickProfileDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1F)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            }
        }
    }

    if (showRealQrScanner) {
        com.example.ui.components.CameraXQrScannerDialog(
            onDismiss = { showRealQrScanner = false },
            onQrCodeDetected = { scannedContent ->
                showRealQrScanner = false
                val pin = if (scannedContent.startsWith("panalink:pin:")) {
                    scannedContent.substringAfter("panalink:pin:")
                } else {
                    scannedContent
                }
                val cleanPin = pin.trim().filter { it.isDigit() }.take(6)
                if (cleanPin.length == 6) {
                    chatsViewModel.addContactByPin(cleanPin)
                } else {
                    android.widget.Toast.makeText(context, "QR inválido o formato incorrecto: $scannedContent", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showAddContactDialog) {
        var pinValue by remember { mutableStateOf("") }

        LaunchedEffect(scanResultPin) {
            scanResultPin?.let { pin ->
                val cleanPin = pin.trim().filter { it.isDigit() }.take(6)
                pinValue = cleanPin
                scanResultPin = null
            }
        }

        AlertDialog(
            onDismissRequest = { 
                chatsViewModel.resetAddContactState()
                showAddContactDialog = false 
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar Contacto de Pana 🇻🇪", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Introduce el PIN de 6 dígitos de tu pana, o escanea directamente su código QR real con tu cámara:",
                        color = Color(0xFF90A4AE),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                pinValue = input
                            }
                        },
                        label = { Text("PIN de 6 dígitos", color = Color(0xFF90A4AE)) },
                        placeholder = { Text("Ej: 222222", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color(0xFF37474F)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_pin_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("Apunta la cámara al código QR de tu Pana 🇻🇪")
                            options.setCameraId(0)
                            options.setBeepEnabled(true)
                            options.setBarcodeImageEnabled(false)
                            barcodeLauncher.launch(options)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear Código QR Real 📸", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (addContactState is AddContactUiState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (addContactState as AddContactUiState.Error).message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (addContactState is AddContactUiState.Loading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validando PIN de Pana...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 6) {
                            chatsViewModel.addContactByPin(pinValue)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    enabled = pinValue.length == 6 && addContactState !is AddContactUiState.Loading,
                    modifier = Modifier.testTag("add_contact_confirm_button")
                ) {
                    Text("Agregar", color = Color(0xFF0F2027), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        chatsViewModel.resetAddContactState()
                        showAddContactDialog = false 
                    }
                ) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF121214)
        )
    }

    if (showCerebroOverlay) {
        val contacts = (contactsState as? ContactsUiState.Success)?.contacts ?: emptyList()
        val chats = (chatsState as? ChatsUiState.Success)?.chats ?: emptyList()
        
        // Define system command items for Cerebro
        val cerebroCommands = remember {
            listOf(
                CerebroCommand("/theme dark_teal", "Verde Pana", "Vuelve al clásico tema verde oscuro de Panalink", Icons.Default.Settings),
                CerebroCommand("/theme cyberpunk", "Cyberpunk", "Estilo neon retro con acentos rosados y azules", Icons.Default.Star),
                CerebroCommand("/theme neon", "Vibe Eléctrico", "Rosado intenso con fondo profundo espacial", Icons.Default.Favorite),
                CerebroCommand("/theme royal_purple", "Púrpura Real", "Elegante tono violeta y amatista sofisticado", Icons.Default.Build),
                CerebroCommand("/theme neon_orange", "Naranja Neon", "Apariencia audaz y ardiente de alta visibilidad", Icons.Default.Warning),
                CerebroCommand("/theme nordic_ice", "Nórdico Glacial", "Fresco, limpio, con tonos azul ártico", Icons.Default.Info),
                CerebroCommand("/theme minimal_white", "Blanco Minimal", "Diseño ultra limpio de alto contraste claro", Icons.Default.Home),
                CerebroCommand("/minimal", "Activar Mínimo", "Oculta paneles extras para una interfaz limpia", Icons.Default.Check),
                CerebroCommand("/full", "Apariencia Completa", "Muestra todos los paneles, estadísticas y widgets", Icons.Default.List),
                CerebroCommand("/nuevo-chat", "Nuevo Chat", "Abre directamente el buscador universal de panas", Icons.Default.Add)
            )
        }

        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        
        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {}
        }

         AlertDialog(
             onDismissRequest = { showCerebroOverlay = false },
             confirmButton = {},
             dismissButton = {
                 TextButton(
                     onClick = { showCerebroOverlay = false },
                     modifier = Modifier.bounceClick()
                 ) {
                     Text("CERRAR", color = Color.White, fontWeight = FontWeight.Bold)
                 }
             },
             containerColor = Color.Black,
             shape = RoundedCornerShape(16.dp),
             modifier = Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(16.dp)),
             title = {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     horizontalArrangement = Arrangement.spacedBy(10.dp)
                 ) {
                     Box(
                         modifier = Modifier
                             .size(36.dp)
                             .background(Color.White.copy(alpha = 0.15f), CircleShape),
                         contentAlignment = Alignment.Center
                     ) {
                         Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                     }
                     Column {
                         Text("Cerebro Spotlight ⚡", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                         Text("Buscador Universal y Comandos Rápidos", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                     }
                 }
             },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = cerebroSearchQuery,
                        onValueChange = { query ->
                            cerebroSearchQuery = query
                            
                            // Instant command runner if typed out fully
                            if (query.trim().lowercase() == "/nuevo-chat") {
                                showCerebroOverlay = false
                                cerebroSearchQuery = ""
                                onNavigateToSearch()
                            } else if (query.startsWith("/theme ")) {
                                val themeParts = query.substring(7).trim().lowercase()
                                val validThemes = listOf("royal_purple", "neon_orange", "nordic_ice", "cyberpunk", "neon", "minimal_white", "dark_teal", "custom")
                                if (themeParts in validThemes) {
                                    com.example.ui.theme.ThemeManager.themeKey.value = themeParts
                                    val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putString("profile_theme_global", themeParts).apply()
                                }
                            } else if (query.trim().lowercase() == "/minimal") {
                                com.example.ui.theme.ThemeManager.isMinimalistMode.value = true
                                val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("minimalist_mode_global", true).apply()
                            } else if (query.trim().lowercase() == "/full") {
                                com.example.ui.theme.ThemeManager.isMinimalistMode.value = false
                                val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("minimalist_mode_global", false).apply()
                            }
                        },
                        label = { Text("¿Qué deseas buscar o ejecutar?", color = Color.White, fontSize = 12.sp) },
                        placeholder = { Text("Escribe / para ver comandos, o busca panas...", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    val query = cerebroSearchQuery.trim().lowercase()

                    if (query.startsWith("/") || query.isEmpty()) {
                        // Display Commands List
                        Text("Comandos Disponibles de Pana:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        
                        val filteredCommands = if (query.isEmpty()) {
                            cerebroCommands
                        } else {
                            cerebroCommands.filter { it.command.lowercase().contains(query) }
                        }

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCommands) { cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F1216), RoundedCornerShape(12.dp))
                                        .border(0.5.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                                        .bounceClick {
                                            if (cmd.command == "/nuevo-chat") {
                                                showCerebroOverlay = false
                                                cerebroSearchQuery = ""
                                                onNavigateToSearch()
                                            } else if (cmd.command.startsWith("/theme ")) {
                                                val t = cmd.command.substring(7)
                                                com.example.ui.theme.ThemeManager.themeKey.value = t
                                                val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putString("profile_theme_global", t).apply()
                                                cerebroSearchQuery = cmd.command // update input
                                            } else if (cmd.command == "/minimal") {
                                                com.example.ui.theme.ThemeManager.isMinimalistMode.value = true
                                                val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putBoolean("minimalist_mode_global", true).apply()
                                                cerebroSearchQuery = cmd.command
                                            } else if (cmd.command == "/full") {
                                                com.example.ui.theme.ThemeManager.isMinimalistMode.value = false
                                                val prefs = com.example.PanaApplication.instance.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putBoolean("minimalist_mode_global", false).apply()
                                                cerebroSearchQuery = cmd.command
                                            }
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(cmd.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cmd.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(cmd.description, color = Color(0xFF9E9E9E), fontSize = 10.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(cmd.command, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Display Live Filtered Search Results!
                        Text("Resultados de Búsqueda:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        
                        val filteredContacts = contacts.filter { 
                            it.displayName.lowercase().contains(query) 
                        }
                        
                        val filteredChats = chats.filter {
                            val otherUser = it.otherMember
                            otherUser != null && otherUser.displayName.lowercase().contains(query)
                        }

                        if (filteredContacts.isEmpty() && filteredChats.isEmpty()) {
                            Text("No se encontraron panas ni chats activos 😢", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 220.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredContacts) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F1216), RoundedCornerShape(12.dp))
                                            .border(0.5.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                                            .bounceClick {
                                                showCerebroOverlay = false
                                                onNavigateToSearch()
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ChatAvatar(
                                                name = contact.displayName,
                                                avatarUrl = contact.avatarUrl,
                                                status = presenceMap[contact.id]?.status?.rawValue ?: "offline", secondaryStatus = if (presenceMap[contact.id]?.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) presenceMap[contact.id]?.secondaryStatus?.rawValue else null,
                                                size = 32.dp
                                            )
                                            Text(contact.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Pana (PIN)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                items(filteredChats) { chat ->
                                    val otherUser = chat.otherMember
                                    val otherName = otherUser?.displayName ?: "Desconocido"
                                    val otherId = otherUser?.id ?: ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F1216), RoundedCornerShape(12.dp))
                                            .border(0.5.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                                            .bounceClick {
                                                showCerebroOverlay = false
                                                onNavigateToChat(chat.chat.id, otherId)
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ChatAvatar(
                                                name = otherName,
                                                avatarUrl = otherUser?.avatarUrl,
                                                status = presenceMap[otherId]?.status?.rawValue ?: "offline", secondaryStatus = if (presenceMap[otherId]?.secondaryStatus != com.example.data.repository.SecondaryPresenceStatus.NONE) presenceMap[otherId]?.secondaryStatus?.rawValue else null,
                                                size = 32.dp
                                            )
                                            Text(otherName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Abrir Chat", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
 

// Simple internal helper class for Cerebro Commands
data class CerebroCommand(
    val command: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)


