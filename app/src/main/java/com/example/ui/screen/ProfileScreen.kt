package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.supabase.SupabaseClient
import com.example.ui.components.PanaAvatar
import com.example.ui.components.PanaTopBarTitle
import com.example.ui.profile.components.ReelsGrid
import com.example.ui.profile.components.SavedGrid
import com.example.ui.settings.navigation.SettingsNavGraph
import com.example.ui.settings.screens.ProfileEditScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToReel: (String) -> Unit = {}
) {
    var showControlCenter by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }

    if (showControlCenter) {
        SettingsNavGraph(
            onBackToMain = { showControlCenter = false },
            onLogout = { authViewModel.logout() },
            onDeleteAccount = { authViewModel.logout() }
        )
        return
    }

    if (isEditingProfile) {
        ProfileEditScreen(
            onBack = { isEditingProfile = false },
            viewModel = viewModel
        )
        return
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val contactIdentifier by viewModel.contactIdentifierState.collectAsStateWithLifecycle()

    var displayName by remember { mutableStateOf("") }
    var selectedAvatarUrl by remember { mutableStateOf("") }
    var selectedCoverUrl by remember { mutableStateOf("") }
    var userPin by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Trigger load
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadReels(currentUid)
    }

    // Populate local variables on Success
    LaunchedEffect(profileState, contactIdentifier) {
        if (profileState is ProfileUiState.Success) {
            val prof = (profileState as ProfileUiState.Success).profile
            displayName = prof.displayName
            selectedAvatarUrl = prof.avatarUrl ?: ""
            selectedCoverUrl = prof.coverUrl ?: ""
            userPin = contactIdentifier?.pin ?: prof.pin ?: ""
        }
    }

    val customPState by com.example.ui.theme.ThemeManager.customPrimary.collectAsState()
    val customSState by com.example.ui.theme.ThemeManager.customSecondary.collectAsState()
    val themeKey by com.example.ui.theme.ThemeManager.themeKey.collectAsState()

    val themeGradient = remember(themeKey, customPState, customSState) {
        when (themeKey) {
            "royal_purple" -> Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF1E033A)))
            "neon_orange" -> Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFF1A0E05)))
            "nordic_ice" -> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF10171C)))
            "cyberpunk" -> Brush.linearGradient(listOf(Color(0xFFBC00DD), Color(0xFF00F0FF)))
            "neon" -> Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFF39FF14)))
            "minimal_white" -> Brush.linearGradient(listOf(Color(0xFFF0F0F0), Color(0xFFCCCCCC)))
            "custom" -> Brush.linearGradient(listOf(customPState, customSState))
            else -> Brush.linearGradient(listOf(Color(0xFF075E54), Color(0xFF128C7E)))
        }
    }

    val reputationState = remember(displayName, selectedAvatarUrl, userPin) {
        when {
            userPin.isNotEmpty() && displayName.isNotEmpty() && selectedAvatarUrl.contains("http") -> "Verificado"
            displayName.isNotEmpty() && selectedAvatarUrl.isNotEmpty() -> "Confiable"
            displayName.isNotEmpty() -> "Nuevo"
            else -> "Limitado"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { PanaTopBarTitle(sectionName = "Mi Perfil", primaryColor = Color(0xFF00FF85)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showControlCenter = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Centro de Control", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF075E54))
            )
        },
        containerColor = Color(0xFF101D24)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeGradient)
                ) {
                    Column {
                        // Cover Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable { isEditingProfile = true }
                        ) {
                            if (selectedCoverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = selectedCoverUrl,
                                    contentDescription = "Portada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddAPhoto, null, tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }

                        // Avatar & Info
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .offset(y = (-30).dp)
                                        .clickable { isEditingProfile = true }
                                ) {
                                    PanaAvatar(
                                        avatarUrl = selectedAvatarUrl.ifEmpty { null },
                                        size = 86.dp,
                                        borderWidth = 3.dp,
                                        borderColor = Color(0xFF101D24),
                                        contentDescription = "Avatar de Perfil",
                                        placeholderName = displayName
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.BottomEnd)
                                            .offset(x = (-4).dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25D366))
                                            .border(2.5.dp, Color(0xFF101D24), CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = displayName.ifEmpty { "Pana de Panalink" },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (reputationState == "Verificado") {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Cuenta Verificada",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = SupabaseClient.currentUser?.email ?: "sin_correo@panalink.com",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Stats & Action Buttons Row
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                val followersCount by viewModel.followersCount.collectAsStateWithLifecycle()
                                val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
                                val likesCount by viewModel.totalLikesCount.collectAsStateWithLifecycle()

                                val stats = listOf(
                                    followersCount to "Seguidores",
                                    followingCount to "Siguiendo",
                                    likesCount to "Me gusta"
                                )
                                stats.forEach { (count, label) ->
                                    Column {
                                        Text(text = "$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BadgeSurface(reputationState, when (reputationState) {
                                        "Verificado" -> Color(0xFF00E5FF)
                                        "Confiable" -> Color(0xFF64B5F6)
                                        else -> Color(0xFFFFD54F)
                                    })
                                    BadgeSurface("Fundador 🌟", Color(0xFFFFD700))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { isEditingProfile = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showControlCenter = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF121B22), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ajustes", color = Color(0xFF121B22), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for Reels & Guardados
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF25D366),
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF25D366)
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Reels 🎬", color = if (selectedTabIndex == 0) Color(0xFF25D366) else Color.White.copy(alpha = 0.6f)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        viewModel.loadSavedContent()
                    },
                    text = { Text("Guardados 🔖", color = if (selectedTabIndex == 1) Color(0xFF25D366) else Color.White.copy(alpha = 0.6f)) }
                )
            }

            if (selectedTabIndex == 0) {
                ReelsGrid(viewModel = viewModel, onNavigateToReel = onNavigateToReel)
            } else {
                SavedGrid(viewModel = viewModel, onNavigateToReel = onNavigateToReel)
            }
        }
    }
}

@Composable
fun BadgeSurface(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
