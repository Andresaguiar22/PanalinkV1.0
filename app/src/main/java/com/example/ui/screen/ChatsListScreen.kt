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
                "Se agregó a $contactName como nuevo contacto"
            }
            android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
            
            chatsViewModel.loadContacts(forceRefresh = true)
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
                    modifier = Modifier.fillMaxWidth()
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

                    // Publicación
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
                                onNavigateToCreateState()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF2D2DB9), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Publicación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Comparte fotos o textos en tu feed.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }

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
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("En vivo", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

                    // Canal Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlusBottomSheet = false
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
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
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

                        // Contactos quick access
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable {
                                    showPlusBottomSheet = false
                                    showContactsDialog = true
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
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Contactos", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                        android.widget.Toast.makeText(context, "Configuración abierta", android.widget.Toast.LENGTH_SHORT).show()
                    }

                    // Menu Item: Modo oscuro
                    val isMinimal by com.example.ui.theme.ThemeManager.isMinimalistMode.collectAsState()
                    QuickProfileMenuItem(
                        icon = if (isMinimal) Icons.Default.Check else Icons.Default.Close,
                        label = "Modo oscuro",
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
fun EstadosTabContent(
    statesState: StatesUiState,
    onNavigateToViewState: (String) -> Unit,
    onNavigateToTikTok: (String) -> Unit,
    onNavigateToCreateState: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
        // "Mi Estado" row to publish
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCreateState() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    com.example.ui.components.PanaAvatar(
                        avatarUrl = SupabaseClient.currentProfile?.avatarUrl,
                        userId = SupabaseClient.currentUser?.id,
                        size = 56.dp,
                        borderWidth = 0.dp,
                        placeholderName = SupabaseClient.currentProfile?.displayName
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, colors.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Mi Estado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Añade una actualización de texto, foto o vídeo", color = Color(0xFF90A4AE), fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1C2D35), thickness = 0.8.dp)
        }

        // Recent Updates - Facebook-style Carousel Title Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Recientes de los Panas ✨👥",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Facebook-style Story Carousel (Horizontal Pager/Row)
        item {
            when (statesState) {
                is StatesUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }
                is StatesUiState.Success -> {
                    val list = statesState.states
                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay estados recientes entre panas. ¡Sé el primero!",
                                color = Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Display Facebook stories in a line carousel
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background)
                        ) {
                            // "Create state" card inside carousel
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(170.dp)
                                        .clickable { onNavigateToCreateState() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.secondary)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        val resolvedAvatar = remember(SupabaseClient.currentProfile?.avatarUrl) {
                                            com.example.data.repository.CdnManager.resolveAvatarUrl(SupabaseClient.currentProfile?.avatarUrl)
                                        }
                                        // My avatar
                                        AsyncImage(
                                            model = resolvedAvatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Blue Add Icon
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(y = 20.dp)
                                                .size(32.dp)
                                                .background(Color.White, CircleShape)
                                                .border(2.dp, colors.secondary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Text(
                                            text = "Crear Estado",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Active user states grouped by user ID
                            val uniqueUserStories = list.distinctBy { it.state.userId }
                            items(uniqueUserStories, key = { it.state.userId }) { stateWithUser ->
                                val firstState = list.firstOrNull { it.state.userId == stateWithUser.state.userId } ?: stateWithUser
                                val state = firstState.state
                                val profile = stateWithUser.profile
                                val userStories = list.filter { it.state.userId == stateWithUser.state.userId }
                                val hasUnread = userStories.any { it.state.viewedByMe != true }

                                // For Thumbnail First logic: fetch a frame if it's a video to serve as a high-fidelity local fallback
                                var videoBitmap by remember(state.mediaUrl) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(state.mediaUrl) {
                                    if (state.mediaType == "video") {
                                        val videoUrl = state.mediaUrl ?: ""
                                        val cached: Bitmap? = null // Dummy cache
                                        if (cached != null) {
                                            videoBitmap = cached
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                val retriever = MediaMetadataRetriever()
                                                try {
                                                    retriever.setDataSource(videoUrl, HashMap<String, String>())
                                                    var fetched: Bitmap? = null
                                                    
                                                    // Try 1: Frame at 1 second sync
                                                    try {
                                                        fetched = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("StoryThumbnail", "Failed to get frame at 1s sync for $videoUrl: ${e.message}")
                                                    }
                                                    
                                                    // Try 2: Frame at 0s sync
                                                    if (fetched == null) {
                                                        try {
                                                            fetched = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                                        } catch (e: Exception) {
                                                            android.util.Log.w("StoryThumbnail", "Failed to get frame at 0s sync for $videoUrl: ${e.message}")
                                                        }
                                                     }
                                                     
                                                     // Try 3: Default representative frame
                                                     if (fetched == null) {
                                                         try {
                                                             fetched = retriever.getFrameAtTime()
                                                         } catch (e: Exception) {
                                                             android.util.Log.w("StoryThumbnail", "Failed to get default frame for $videoUrl: ${e.message}")
                                                         }
                                                     }
                                                     
                                                     // Try 4: Frame at any time (-1)
                                                     if (fetched == null) {
                                                         try {
                                                             fetched = retriever.getFrameAtTime(-1)
                                                         } catch (e: Exception) {
                                                             android.util.Log.e("StoryThumbnail", "Failed to get frame at -1 for $videoUrl: ${e.message}")
                                                         }
                                                     }
                                                     
                                                     if (fetched != null) {
                                                         videoThumbnailCache[videoUrl] = fetched
                                                         videoBitmap = fetched
                                                     } else {
                                                         android.util.Log.e("StoryThumbnail", "All thumbnail extraction attempts failed for $videoUrl")
                                                     }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("StoryThumbnail", "Error setting datasource for $videoUrl", e)
                                                } finally {
                                                    try {
                                                        retriever.release()
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        }
                                    }
                                }

                                val thumbnailUrl = remember(state.mediaUrl) {
                                    val url = state.mediaUrl ?: ""
                                    if (url.contains("/videos/")) {
                                        url.replace("/videos/", "/images/")
                                           .replace("video_", "thumb_video_")
                                           .replace(".mp4", ".jpg")
                                           .replace(".mov", ".jpg")
                                           .replace(".3gp", ".jpg")
                                    } else {
                                        url
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(170.dp)
                                        .clickable { onNavigateToViewState(state.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.secondary),
                                    border = if (hasUnread) {
                                        BorderStroke(1.5.dp, com.example.ui.theme.getPremiumActiveIconGradient())
                                    } else {
                                        BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.5f))
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Media background
                                        if (state.mediaType == "text") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF7E57C2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = state.caption ?: "",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else if (state.mediaType == "video") {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                val currentBitmap = videoBitmap
                                                if (currentBitmap != null) {
                                                    Image(
                                                        bitmap = currentBitmap.asImageBitmap(),
                                                        contentDescription = "Miniatura Historia",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    // High-fidelity dark slate loading state with green progress indicator
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFF1E1E1E)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(24.dp),
                                                            color = Color(0xFF00FF85),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                }
                                                
                                                // Speaker icon (state control) in the bottom-right corner of the Box
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(bottom = 8.dp, end = 8.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = "Contenido Multimedia",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            AsyncImage(
                                                model = state.mediaUrl ?: "https://images.unsplash.com/photo-1563911302283-d2bc1d9e2659?auto=format&fit=crop&w=150&q=80",
                                                contentDescription = "Estado",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Dark gradient overlay for bottom name
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                    )
                                                )
                                        )

                                        // Top-left user avatar
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
                                        val identityState by identityRepository.observeIdentity(stateWithUser.state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(stateWithUser.state.userId)?.toIdentityUiState())
                                        val safeAvatarUrl = identityState?.avatarUrl ?: profile.avatarUrl
                                        val safeUserId = identityState?.userId ?: profile.id
                                        val safeDisplayName = identityState?.displayName ?: profile.displayName

                                        com.example.ui.components.PanaAvatar(
                                            avatarUrl = safeAvatarUrl,
                                            userId = safeUserId,
                                            size = 28.dp,
                                            borderWidth = 1.5.dp,
                                            borderColor = if (hasUnread) colors.primary else Color.Gray.copy(alpha = 0.5f),
                                            placeholderName = safeDisplayName,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopStart)
                                        )
                                        // Bottom name text
                                        Text(
                                            text = safeDisplayName.split(" ").first(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is StatesUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Error al cargar estados", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section Title: Videos from all users (TikTok style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "El Feed de Panalink 🇻🇪",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Grid-like listing of all videos published by all users
        when (statesState) {
            is StatesUiState.Success -> {
                val list = statesState.states
                val videoStates = list.filter { it.state.mediaType == "video" }

                if (videoStates.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aún no hay vídeos publicados en la comunidad.",
                                color = Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "¡Sube un vídeo desde tu galería para comenzar el ambiente!",
                                color = Color(0xFF607D8B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Display them in a beautiful 2-column grid-like structure (by chunking 2 items per row)
                    val rows = videoStates.chunked(2)
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (videoState in rowItems) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
                                val identityState by identityRepository.observeIdentity(videoState.state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(videoState.state.userId)?.toIdentityUiState())
                                val safeAvatarUrl = identityState?.avatarUrl ?: videoState.profile.avatarUrl
                                val safeDisplayName = identityState?.displayName ?: videoState.profile.displayName

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.secondary)
                                        .border(1.dp, Color(0xFF262629), RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToTikTok(videoState.state.id) }
                                ) {
                                    // Visual card design
                                    if (videoState.state.mediaType == "video") {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription="Play", tint=Color.White)
                                        }
                                    } else {
                                        AsyncImage(
                                            model = videoState.state.mediaUrl ?: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80",
                                            contentDescription = "Video Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Immersive Dark Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                                )
                                            )
                                    )

                                    // Play icon overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Ver Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Uploader detail overlay at bottom
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AsyncImage(
                                                model = safeAvatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80",
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = safeDisplayName.split(" ").first(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (!videoState.state.caption.isNullOrBlank()) {
                                            Text(
                                                text = videoState.state.caption,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // If row is not complete, add a spacer to balance the weight
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            else -> {
                items(5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        // Name and message preview column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
                        // Trailing info
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)

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


@Composable
fun StateItemRow(
    stateWithUser: UserStateWithUser,
    onNavigateToViewState: (String) -> Unit
) {
    val state = stateWithUser.state
    val profile = stateWithUser.profile
    val formattedTime = com.example.data.model.formatIsoDateTime(state.createdAt)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(state.userId)?.toIdentityUiState())
    
    val safeAvatarUrl = identityState?.avatarUrl ?: profile.avatarUrl
    val safeDisplayName = identityState?.displayName ?: profile.displayName
    val safeUserId = identityState?.userId ?: profile.id

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToViewState(state.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(2.dp, com.example.ui.theme.getPremiumActiveIconGradient(), CircleShape)
                .padding(3.dp)
        ) {
            com.example.ui.components.PanaAvatar(
                avatarUrl = safeAvatarUrl,
                userId = safeUserId,
                size = 54.dp,
                borderWidth = 0.dp,
                placeholderName = safeDisplayName,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = safeDisplayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Publicado hoy, $formattedTime",
                color = Color(0xFF90A4AE),
                fontSize = 12.sp
            )
        }
    }
}

// Global cache for video thumbnails to avoid fetching repeatedly
val videoThumbnailCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()


@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    var bitmap by remember(videoUrl) { mutableStateOf(videoThumbnailCache[videoUrl]) }
    var isLoading by remember(videoUrl) { mutableStateOf(bitmap == null) }

    LaunchedEffect(videoUrl) {
        if (bitmap == null) {
            isLoading = true
            val fetchedBitmap = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoUrl, HashMap<String, String>())
                    // Fetch a frame at 1 second (1,000,000 microseconds)
                    retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    android.util.Log.e("VideoThumbnail", "Error fetching frame for $videoUrl", e)
                    null
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {}
                }
            }
            if (fetchedBitmap != null) {
                videoThumbnailCache[videoUrl] = fetchedBitmap
                bitmap = fetchedBitmap
            }
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            // Nice fallback landscape background
            AsyncImage(
                model = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80",
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun ContactsTabContent(
    contactsState: ContactsUiState,
    chatsViewModel: ChatsViewModel,
    isSelectingContactOnly: Boolean,
    onNavigateToChat: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onContactLongClick: (Profile) -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsStateWithLifecycle()

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
        when (contactsState) {
        is ContactsUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        is ContactsUiState.Success -> {
            val contacts = contactsState.contacts
            android.util.Log.d("CONTACTS_DEBUG", "cantidad finalmente mostrada por la UI: ${contacts.size}")
            val requestsState by chatsViewModel.friendRequestsState.collectAsState()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (requestsState is FriendRequestsUiState.Success) {
                    val requests = (requestsState as FriendRequestsUiState.Success).requests
                    if (requests.isNotEmpty()) {
                        item {
                            Text(
                                text = "Solicitudes de amistad (${requests.size})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp, 8.dp)
                            )
                        }
                        items(requests) { request ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ui.components.PanaAvatar(
                                    avatarUrl = request.sender?.avatarUrl,
                                    userId = request.sender?.id,
                                    placeholderName = request.sender?.displayName ?: "",
                                    size = 40.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = request.sender?.displayName ?: "",
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { chatsViewModel.acceptFriendRequest(request.id) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = Color.Green)
                                }
                                IconButton(onClick = { chatsViewModel.declineFriendRequest(request.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                if (contacts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF37474F),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aún no tienes panas agregados",
                                color = Color(0xFF90A4AE),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Presiona el botón '+' en la esquina superior para agregar a un pana usando su PIN o escaneando su QR.",
                                color = Color(0xFF607D8B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    if (isSelectingContactOnly) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, colors.primary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "Selecciona un pana para chatear 💬",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = if (isSelectingContactOnly) "Seleccionar Contacto" else "Tus Panas Agregados (${contacts.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = if (isSelectingContactOnly) 4.dp else 16.dp, bottom = 8.dp)
                        )
                    }
                    items(contacts) { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        chatsViewModel.createChat(contact) { chat ->
                                            onNavigateToChat(chat.id, contact.id)
                                        }
                                    },
                                    onLongClick = {
                                        onContactLongClick(contact)
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("contact_row_${contact.displayName}")
                        ) {
                            val isContactOnline = presenceMap[contact.id]?.status != com.example.data.repository.UserPresenceStatus.OFFLINE
                            ChatAvatar(
                                name = contact.displayName,
                                avatarUrl = contact.avatarUrl,
                                status = if (isContactOnline) "online" else "offline",
                                size = 50.dp
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

                            // Actions: message, voice call, video call
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                if (!isSelectingContactOnly) {
                                    IconButton(
                                        onClick = {
                                            com.example.call.CallManager.getInstance(context).startCall(
                                                targetUserId = contact.id,
                                                targetUserName = contact.displayName,
                                                type = com.example.call.CallType.AUDIO
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Llamada de voz",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            com.example.call.CallManager.getInstance(context).startCall(
                                                targetUserId = contact.id,
                                                targetUserName = contact.displayName,
                                                type = com.example.call.CallType.VIDEO
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = "Videollamada",
                                            tint = Color(0xFF3B82F6)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF1E2E36), thickness = 0.5.dp)
                    }
                }
            }
        }
        is ContactsUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contactsState.message,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
}


@Composable
fun LlamadasTabContent(
    contactsState: ContactsUiState,
    onRefresh: () -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    val callManager = remember { com.example.call.CallManager.getInstance(context) }
    val isConnected by callManager.isConnected.collectAsState()
    val presenceMap by com.example.data.repository.PresenceRepository.presenceMap.collectAsStateWithLifecycle()
    val currentProfile by com.example.data.supabase.SupabaseClient.currentProfileState.collectAsState()

    // Ensure signaling is active and tries to reconnect if disconnected when screen is shown
    LaunchedEffect(currentProfile, isConnected) {
        if (currentProfile != null && currentProfile?.isProfileComplete == true && !isConnected) {
            android.util.Log.d("ChatsListScreen", "Signaling disconnected, attempting auto-reconnect...")
            callManager.initialize(currentProfile?.id ?: "")
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Signaling engine status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        scope.launch {
                            // Force refresh CDN URL if user clicks on the status card
                            com.example.data.repository.CdnManager.getCDNUrl(forceRefresh = true)
                            callManager.forceReconnect()
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondary
                ),
                border = BorderStroke(1.dp, Color(0xFF262629)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Servicio de Señalización WebRTC",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "🟢 Conectado - Listo para llamadas" else "🔴 Desconectado - Reconectando...",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Contact list under "Llamar a un pana"
            Text(
                text = "Llamar a un Pana",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (contactsState) {
                is ContactsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is ContactsUiState.Success -> {
                    val contacts = contactsState.contacts
                    if (contacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "📞",
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = "Historial de Llamadas Vacío",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Aún no tienes panas en tu agenda. Agrega panas usando su PIN en la pestaña 'Contactos' para poder llamarlos gratis por WebRTC.",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(contacts) { contact ->
                                val isContactOnline = presenceMap[contact.id]?.status != com.example.data.repository.UserPresenceStatus.OFFLINE
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                        .testTag("call_contact_row_${contact.displayName}")
                                ) {
                                    ChatAvatar(
                                        name = contact.displayName,
                                        avatarUrl = contact.avatarUrl,
                                        status = if (isContactOnline) "online" else "offline",
                                        size = 48.dp
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isContactOnline) "En línea" else "Desconectado",
                                            color = if (isContactOnline) Color(0xFF00FF85) else Color(0xFF90A4AE),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Voice Call Button
                                        IconButton(
                                            onClick = {
                                                callManager.startCall(
                                                    targetUserId = contact.id,
                                                    targetUserName = contact.displayName,
                                                    type = com.example.call.CallType.AUDIO
                                                )
                                            },
                                            modifier = Modifier
                                                .background(
                                                    color = Color.White.copy(alpha = 0.12f),
                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                )
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Llamada de voz",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Video Call Button
                                        IconButton(
                                            onClick = {
                                                callManager.startCall(
                                                    targetUserId = contact.id,
                                                    targetUserName = contact.displayName,
                                                    type = com.example.call.CallType.VIDEO
                                                )
                                            },
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                )
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Videollamada",
                                                tint = Color(0xFF3B82F6),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1E2E36), thickness = 0.5.dp)
                            }
                        }
                    }
                }
                is ContactsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error cargando panas: ${(contactsState as ContactsUiState.Error).message}",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


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
fun TopActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .background(colors.secondary, CircleShape)
            .border(1.dp, colors.primary.copy(alpha = 0.1f), CircleShape)
            .bounceClick()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}


@Composable
fun FunkyBottomNavItem(
    selected: Boolean,
    label: String,
    icon:  () -> Unit,
    colors: com.example.ui.theme.AppColors,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1.00f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(72.dp)
            .height(72.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .scale(scale)
    ) {
        // Cyan-glowing rounded square (squircle) enclosing active icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .then(
                    if (selected) {
                        Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.5.dp, com.example.ui.theme.getPremiumActiveIconGradient(), RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            icon()
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(Color.Red, CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (selected) colors.primary else Color(0xFF9E9E9E),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
            style = if (selected) {
                androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.White.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 8f
                    )
                )
            } else {
                androidx.compose.ui.text.TextStyle.Default
            }
        )
    }
}


@Composable
fun NavChatsIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.15f)
            quadraticTo(w * 0.95f, h * 0.15f, w * 0.95f, h * 0.25f)
            lineTo(w * 0.95f, h * 0.70f)
            quadraticTo(w * 0.95f, h * 0.80f, w * 0.85f, h * 0.80f)
            lineTo(w * 0.45f, h * 0.80f)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.22f, h * 0.80f)
            lineTo(w * 0.15f, h * 0.80f)
            quadraticTo(w * 0.05f, h * 0.80f, w * 0.05f, h * 0.70f)
            lineTo(w * 0.05f, h * 0.25f)
            quadraticTo(w * 0.05f, h * 0.15f, w * 0.15f, h * 0.15f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}


@Composable
fun NavEstadosIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = tint,
            radius = w * 0.45f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = w * 0.14f
        )
    }
}


@Composable
fun NavContactosIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        
        // Main person head
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Main person shoulder
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.32f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Secondary person head
        drawCircle(
            color = tint,
            radius = w * 0.13f,
            center = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.42f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Secondary person shoulder
        drawArc(
            color = tint,
            startAngle = 195f,
            sweepAngle = 145f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.58f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.26f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}


@Composable
fun NavLlamadasIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.28f, h * 0.25f)
            quadraticTo(w * 0.32f, h * 0.20f, w * 0.40f, h * 0.25f)
            lineTo(w * 0.48f, h * 0.33f)
            quadraticTo(w * 0.52f, h * 0.38f, w * 0.47f, h * 0.43f)
            lineTo(w * 0.43f, h * 0.47f)
            quadraticTo(w * 0.53f, h * 0.58f, w * 0.58f, h * 0.53f)
            lineTo(w * 0.62f, h * 0.48f)
            quadraticTo(w * 0.67f, h * 0.43f, w * 0.72f, h * 0.48f)
            lineTo(w * 0.80f, h * 0.56f)
            quadraticTo(w * 0.85f, h * 0.61f, w * 0.80f, h * 0.68f)
            quadraticTo(w * 0.72f, h * 0.80f, w * 0.62f, h * 0.80f)
            quadraticTo(w * 0.35f, h * 0.80f, w * 0.22f, h * 0.52f)
            quadraticTo(w * 0.18f, h * 0.40f, w * 0.28f, h * 0.25f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)

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

@Composable
fun PlusOptionCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        border = BorderStroke(1.dp, Color(0xFF262629))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text(subtitle, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}


@Composable
fun DisabledPlusOptionBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(Color(0xFF161618), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF262629), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("Próximamente", color = Color.Red.copy(alpha = 0.6f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun QuickProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun PendingUploadsBanner(
    pendingUploadsViewModel: com.example.ui.viewmodel.PendingUploadsViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeUploads by pendingUploadsViewModel.activeUploads.collectAsState()
    val progressMap by pendingUploadsViewModel.uploadProgressMap.collectAsState()
    var showStatusCenterModal by remember { mutableStateOf(false) }
    var itemToCancel by remember { mutableStateOf<com.example.data.database.PendingUploadEntity?>(null) }
    var deleteLocalFileOnCancel by remember { mutableStateOf(false) }

    if (activeUploads.isEmpty() && !showStatusCenterModal) return

    val failedUploads = remember(activeUploads) { activeUploads.filter { it.status == "failed" } }
    val uploadingUploads = remember(activeUploads) { activeUploads.filter { it.status == "uploading" } }
    val pendingUploads = remember(activeUploads) { activeUploads.filter { it.status == "pending" } }
    val hasFailed = failedUploads.isNotEmpty()

    if (activeUploads.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasFailed) Color(0xFF2C1618) else Color(0xFF19232D)
            ),
            border = BorderStroke(
                1.dp,
                if (hasFailed) Color(0xFFFF453A) else Color(0xFF007AFF)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (hasFailed) {
                            Text(text = "⚠️", fontSize = 16.sp)
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF007AFF),
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                hasFailed -> "${failedUploads.size} publicación(es) fallida(s)"
                                uploadingUploads.isNotEmpty() -> "Subiendo ${uploadingUploads.size} publicación(es)..."
                                else -> "Esperando red (${pendingUploads.size} en cola)..."
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gestor 📊",
                            color = Color(0xFF64D2FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showStatusCenterModal = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        if (hasFailed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    failedUploads.forEach { item ->
                                        pendingUploadsViewModel.retryUpload(context, item.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Reintentar 🔄", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 1: Uploading Items with real progress
                uploadingUploads.forEach { item ->
                    val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                    val progressInfo = progressMap[item.id]
                    val percent = progressInfo?.progressPercent ?: 0
                    val bytesWritten = progressInfo?.bytesWritten ?: 0L
                    val totalBytes = if ((progressInfo?.totalBytes ?: 0L) > 0L) progressInfo!!.totalBytes else file.length()
                    val statusText = progressInfo?.statusText ?: "Subiendo..."

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "📤", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$statusText ${item.uploadType.uppercase()} ($percent%)",
                                    color = Color(0xFF64D2FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Cancelar ✕",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        itemToCancel = item
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF007AFF),
                            trackColor = Color(0xFF1C2D3D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${localFormatFileSize(bytesWritten)} / ${localFormatFileSize(totalBytes)}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Section 2: Pending Items
                pendingUploads.forEach { item ->
                    val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                    val fileSizeText = remember(file) { localFormatFileSize(file.length()) }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "⏳", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "En cola: ${item.uploadType.uppercase()} ($fileSizeText)",
                                color = Color(0xFFFFD60A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Text(
                            text = "Cancelar ✕",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    itemToCancel = item
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Section 3: Failed Items
                failedUploads.forEach { failedItem ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error al publicar ${failedItem.uploadType.uppercase()}",
                                color = Color(0xFFFF453A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = failedItem.errorMessage ?: "Conexión interrumpida o fallo temporal",
                                color = Color(0xFFFF9F0A),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Reintentar",
                                color = Color(0xFF64D2FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        pendingUploadsViewModel.retryUpload(context, failedItem.id)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eliminar ✕",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        pendingUploadsViewModel.dismissUpload(failedItem.id)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancellation Dialog
    if (itemToCancel != null) {
        val target = itemToCancel!!
        AlertDialog(
            onDismissRequest = { itemToCancel = null },
            title = {
                Text("¿Cancelar subida de ${target.uploadType.uppercase()}?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Se detendrá el proceso de publicación actual.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteLocalFileOnCancel = !deleteLocalFileOnCancel }
                    ) {
                        Checkbox(
                            checked = deleteLocalFileOnCancel,
                            onCheckedChange = { deleteLocalFileOnCancel = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFFF453A),
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Eliminar también archivo local",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUploadsViewModel.cancelUpload(context, target.id, deleteLocalFileOnCancel)
                        itemToCancel = null
                        deleteLocalFileOnCancel = false
                    }
                ) {
                    Text("Cancelar Subida", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToCancel = null }) {
                    Text("Volver", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }

    // Status Center Modal / Sheet
    if (showStatusCenterModal) {
        MediaStatusCenterModal(
            pendingUploadsViewModel = pendingUploadsViewModel,
            onDismiss = { showStatusCenterModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MediaStatusCenterModal(
    pendingUploadsViewModel: com.example.ui.viewmodel.PendingUploadsViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allUploads by pendingUploadsViewModel.allUploads.collectAsState()
    val progressMap by pendingUploadsViewModel.uploadProgressMap.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Todas, 1: Activas, 2: Fallidas, 3: Completadas

    val filteredList = remember(allUploads, selectedTab) {
        when (selectedTab) {
            1 -> allUploads.filter { it.status == "uploading" || it.status == "pending" }
            2 -> allUploads.filter { it.status == "failed" }
            3 -> allUploads.filter { it.status == "completed" || it.status == "cancelled" }
            else -> allUploads
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Centro de Estado Multimedia 📊",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.Gray, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF007AFF)
            ) {
                listOf("Todas", "En proceso", "Fallidas", "Historial").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                color = if (selectedTab == index) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay publicaciones en esta categoría",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                ) {
                    itemsIndexed(filteredList, key = { index, item -> "${item.id}_$index" }) { _, item ->
                        val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                        val sizeText = remember(file) { localFormatFileSize(file.length()) }
                        val progressInfo = progressMap[item.id]
                        val percent = progressInfo?.progressPercent ?: 0
                        val bytesWritten = progressInfo?.bytesWritten ?: 0L
                        val totalBytes = if ((progressInfo?.totalBytes ?: 0L) > 0L) progressInfo!!.totalBytes else file.length()
                        val statusText = progressInfo?.statusText ?: "Procesando"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.uploadType.uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            LocalStatusChip(status = item.status)
                                            if (item.status == "uploading") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "$percent%",
                                                    color = Color(0xFF64D2FF),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        val safeCaption = item.caption
                                        if (!safeCaption.isNullOrEmpty()) {
                                            Text(
                                                text = safeCaption,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = if (item.status == "uploading") {
                                                "$statusText (${localFormatFileSize(bytesWritten)} / ${localFormatFileSize(totalBytes)})"
                                            } else {
                                                "Tamaño: $sizeText ${if (item.errorMessage != null) "• " + item.errorMessage else ""}"
                                            },
                                            color = if (item.status == "failed") Color(0xFFFF453A) else Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.status == "failed") {
                                            Text(
                                                text = "Reintentar 🔄",
                                                color = Color(0xFF64D2FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        pendingUploadsViewModel.retryUpload(context, item.id)
                                                    }
                                                    .padding(6.dp)
                                            )
                                        }
                                        Text(
                                            text = "Borrar ✕",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    pendingUploadsViewModel.dismissUpload(item.id)
                                                }
                                                .padding(6.dp)
                                        )
                                    }
                                }

                                if (item.status == "uploading") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF007AFF),
                                        trackColor = Color(0xFF2C2C2E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    pendingUploadsViewModel.clearCompletedUploads()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Limpiar historial completado 🧹", color = Color.LightGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun LocalStatusChip(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "uploading" -> Triple(Color(0xFF0C2A4A), Color(0xFF64D2FF), "Subiendo")
        "pending" -> Triple(Color(0xFF332B00), Color(0xFFFFD60A), "En cola")
        "failed" -> Triple(Color(0xFF3B0D0C), Color(0xFFFF453A), "Fallido")
        "completed" -> Triple(Color(0xFF0F3818), Color(0xFF30D158), "Publicado")
        "cancelled" -> Triple(Color(0xFF262628), Color(0xFF8E8E93), "Cancelado")
        else -> Triple(Color.DarkGray, Color.White, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

fun localFormatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb < 0.1) {
        val kb = bytes.toDouble() / 1024
        String.format("%.1f KB", kb)
    } else {
        String.format("%.1f MB", mb)
    }
}


@Composable
fun PendingPostCard(post: com.example.data.database.PendingPostEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Subiendo publicación...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("En cola local", color = Color.Gray, fontSize = 11.sp)
                }
            }
            if (!post.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(post.content, color = Color.LightGray, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00FF85),
                trackColor = Color(0xFF2C2C2E)
            )
        }
    }
}
