package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.supabase.SupabaseClient
import com.example.ui.screen.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatsViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.StatesViewModel
import androidx.lifecycle.lifecycleScope
import com.example.data.supabase.SessionManager
import com.example.media.audio.AudioRepository
import com.example.media.playlist.PlaylistRepository
import com.example.media.sync.MusicPlaylistRealtimeManager
import com.example.media.worker.MusicSocialSyncWorker
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@UnstableApi
class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val currentIntentState = androidx.compose.runtime.mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
        android.util.Log.d("MainActivity", "DEEPLINK_RECEIVED")
        android.util.Log.d("MainActivity", "DEEPLINK_URI = ${intent.data}")
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                SessionManager.validateAndRefreshSessionIfNeeded()
                SessionManager.triggerSync()
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Error during onResume session sync", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            com.example.security.AppLockManager.onAppForegrounded(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "AppLock foreground check failed", e)
        }
    }

    override fun onStop() {
        try {
            com.example.security.AppLockManager.onAppBackgrounded()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "AppLock background hook failed", e)
        }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val prefs = getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
        val isPipEnabled = prefs.getBoolean("floating_pip_enabled", true)
        if (!isPipEnabled) return

        val manager = com.example.util.AppFloatingPlayerManager
        if (manager.exoPlayer != null && (manager.activeType == "reel" || manager.activeType == "panatv")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = android.app.PictureInPictureParams.Builder()
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        com.example.util.AppFloatingPlayerManager.isInNativePip = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        try {
            com.example.security.AppLockManager.onAppLaunched(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "AppLock launch check failed", e)
        }
        
        val authViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.ui.viewmodel.AuthViewModel::class.java]
        splashScreen.setKeepOnScreenCondition {
            val state = authViewModel.uiState.value
            state is com.example.ui.viewmodel.AuthUiState.Idle || state is com.example.ui.viewmodel.AuthUiState.Loading
        }
        
        currentIntentState.value = intent
        android.util.Log.d("MainActivity", "DEEPLINK_RECEIVED")
        android.util.Log.d("MainActivity", "DEEPLINK_URI = ${intent?.data}")
        
        // Register all separated Notification Channels (Messages, Calls, System, Alerts)
        try {
            com.example.service.NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating notification channels", e)
        }
        
        // Initialize dynamic ThemeManager from preferences on start
        try {
            val prefs = getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
            val savedTheme = prefs.getString("profile_theme_global", "halo_dark") ?: "halo_dark"
            com.example.ui.theme.ThemeManager.themeKey.value = savedTheme

            val isMinimal = prefs.getBoolean("minimalist_mode_global", false)
            com.example.ui.theme.ThemeManager.isMinimalistMode.value = isMinimal

            val savedColorPreset = prefs.getString("bottom_bar_color_preset", "tropical") ?: "tropical"
            com.example.ui.theme.ThemeManager.bottomBarColorPreset.value = savedColorPreset

            val savedShapePreset = prefs.getString("bottom_bar_shape_preset", "pill") ?: "pill"
            com.example.ui.theme.ThemeManager.bottomBarShapePreset.value = savedShapePreset

            val customP = prefs.getInt("custom_primary", 0xFF00E5FF.toInt())
            val customB = prefs.getInt("custom_background", 0xFF000000.toInt())
            val customAc = prefs.getInt("custom_accent", 0xFF8B5CF6.toInt())
            val customS = prefs.getInt("custom_surface", 0xFF121212.toInt())
            val customSec = prefs.getInt("custom_secondary", 0xFF161618.toInt())

            com.example.ui.theme.ThemeManager.customPrimary.value = androidx.compose.ui.graphics.Color(customP)
            com.example.ui.theme.ThemeManager.customBackground.value = androidx.compose.ui.graphics.Color(customB)
            com.example.ui.theme.ThemeManager.customAccent.value = androidx.compose.ui.graphics.Color(customAc)
            com.example.ui.theme.ThemeManager.customSurface.value = androidx.compose.ui.graphics.Color(customS)
            com.example.ui.theme.ThemeManager.customSecondary.value = androidx.compose.ui.graphics.Color(customSec)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load custom theme preferences", e)
        }

        enableEdgeToEdge()
        setContent {
            val isAppLocked by com.example.security.AppLockManager.isLocked.collectAsState()
            val activeThemeKey by com.example.ui.theme.ThemeManager.themeKey.collectAsState()
            val customPrimary by com.example.ui.theme.ThemeManager.customPrimary.collectAsState()
            val customBackground by com.example.ui.theme.ThemeManager.customBackground.collectAsState()
            val customAccent by com.example.ui.theme.ThemeManager.customAccent.collectAsState()
            val customSurface by com.example.ui.theme.ThemeManager.customSurface.collectAsState()
            val customSecondary by com.example.ui.theme.ThemeManager.customSecondary.collectAsState()

            val customColors = androidx.compose.runtime.remember(customPrimary, customBackground, customAccent, customSurface, customSecondary) {
                com.example.ui.theme.AppColors(
                    primary = customPrimary,
                    secondary = customSecondary,
                    background = customBackground,
                    surface = customSurface,
                    bubbleMe = customPrimary,
                    bubbleOther = customSurface,
                    topBar = customSecondary,
                    bottomBar = customSecondary,
                    accent = customAccent,
                    isDark = activeThemeKey != "minimal_white" && activeThemeKey != "halo_light" && activeThemeKey != "whatsapp_light",
                    onPrimary = Color.White,
                    onSecondary = Color.Black,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }

            MyApplicationTheme(themeKey = activeThemeKey, customColors = customColors) {
                // Instantiate central ViewModels
                val authViewModel: AuthViewModel = viewModel()
                val chatsViewModel: ChatsViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                val statesViewModel: StatesViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                val notificationsViewModel: com.example.ui.viewmodel.NotificationsViewModel = viewModel()
                val playerViewModel: com.example.media.player.ui.PlayerViewModel = viewModel()
                
                val currentContext = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.runtime.DisposableEffect(Unit) {
                    val receiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                            statesViewModel.loadActiveStates(showLoading = false)
                        }
                    }
                    val filter = android.content.IntentFilter("com.example.REEL_UPLOADED")
                    ContextCompat.registerReceiver(
                        currentContext,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose {
                        currentContext.unregisterReceiver(receiver)
                    }
                }

                val authUiState by authViewModel.uiState.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(currentIntentState.value) {
                    val intent = currentIntentState.value
                    if (intent != null) {
                        authViewModel.handleDeepLinkIntent(intent)
                    }
                }

                val callManager = remember { com.example.call.CallManager.getInstance(context) }
                val callState by callManager.callState.collectAsState()
                val callType by callManager.callType.collectAsState()
                val opponentName by callManager.opponentName.collectAsState()
                val opponentId by callManager.opponentId.collectAsState()
                val isMuted by callManager.isMuted.collectAsState()
                val isSpeakerOn by callManager.isSpeakerOn.collectAsState()
                val isCameraOn by callManager.isCameraOn.collectAsState()

                val callPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val recordAudioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
                    val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
                    android.util.Log.d("MainActivity", "Call permissions requested. Mic: $recordAudioGranted, Cam: $cameraGranted")
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        android.util.Log.d("MainActivity", "Notification permission granted")
                    }
                }

                // Listen to Auth State logouts/logins globally to control top-level services
                LaunchedEffect(authUiState, com.example.data.supabase.SupabaseClient.currentProfile) {
                    val state = authUiState
                    val profile = com.example.data.supabase.SupabaseClient.currentProfile
                    
                    val isComplete = profile?.isProfileComplete == true

                    if (state is AuthUiState.Idle) {
                        try {
                            callManager.release()
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to release CallManager", e)
                        }
                        try {
                            val serviceIntent = Intent(context, com.example.service.PanalinkRealtimeService::class.java)
                            context.stopService(serviceIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to stop service", e)
                        }
                        com.example.util.PanalinkInitializationManager.reset()
                    } else if ((state is AuthUiState.Success || state is AuthUiState.AuthenticatedReady || state is AuthUiState.AuthenticatedIncomplete || state is AuthUiState.Authenticated || state is AuthUiState.NeedsProfileSetup) && profile != null && isComplete) {
                        // Initialize all core Panalink background services safely for complete profile users
                        com.example.util.PanalinkInitializationManager.initializeCompleteUser(
                            context = context,
                            profile = profile,
                            scope = lifecycleScope
                        )

                        // P6.7.6B - Initialize Music Social Sync
                        lifecycleScope.launch {
                            MusicSocialSyncWorker.schedulePeriodicSync(context)
                            MusicSocialSyncWorker.runOnce(context)
                        }

                        // Request POST_NOTIFICATIONS permission cleanly for Android 13+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var isSplashActive by remember { mutableStateOf(true) }
                var lastUserId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    isSplashActive = false
                }

                LaunchedEffect(authUiState, com.example.data.supabase.SupabaseClient.currentProfile) {
                    val isAuthenticated = authUiState is AuthUiState.Success ||
                            authUiState is AuthUiState.AuthenticatedReady ||
                            authUiState is AuthUiState.AuthenticatedIncomplete ||
                            authUiState is AuthUiState.Authenticated ||
                            authUiState is AuthUiState.NeedsProfileSetup
                    val profile = com.example.data.supabase.SupabaseClient.currentProfile
                    val isComplete = profile?.isProfileComplete == true
                    
                    if (isAuthenticated && profile != null && isComplete) {
                        val userId = profile.id
                        if (lastUserId != userId) {
                            lastUserId = userId
                            if (!isSplashActive) {
                                isSplashActive = true
                                chatsViewModel.loadChats(forceRefresh = true)
                                statesViewModel.loadActiveStates(showLoading = false)
                                delay(1500)
                                isSplashActive = false
                            }
                        }
                    } else {
                        lastUserId = null
                    }
                }

                LaunchedEffect(callState) {
                    if (callState is com.example.call.CallState.RINGING || 
                        callState is com.example.call.CallState.OUTGOING) {
                        callPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.RECORD_AUDIO,
                                android.Manifest.permission.CAMERA
                            )
                        )
                    }
                }

                // Determine active navigation structure from AuthUiState, ignoring transient states like Loading, Error, and Idle (unless starting up)
                val initialFlow = remember {
                    val user = com.example.data.supabase.SupabaseClient.currentUser
                    val profile = com.example.data.supabase.SupabaseClient.currentProfile
                    if (user != null) {
                        if (profile != null && profile.isProfileComplete) {
                            AuthUiState.Authenticated(user, profile)
                        } else if (profile != null) {
                            AuthUiState.NeedsProfileSetup(user, profile)
                        } else {
                            AuthUiState.LoggedOut
                        }
                    } else {
                        AuthUiState.LoggedOut
                    }
                }
                var currentFlow by remember { mutableStateOf<AuthUiState>(initialFlow) }

                LaunchedEffect(authUiState) {
                    when (val newState = authUiState) {
                        is AuthUiState.LoggedOut -> {
                            currentFlow = AuthUiState.LoggedOut
                        }
                        is AuthUiState.NeedsEmailVerification -> {
                            currentFlow = newState
                        }
                        is AuthUiState.NeedsVerification -> {
                            currentFlow = AuthUiState.NeedsEmailVerification(newState.email)
                        }
                        is AuthUiState.NeedsProfileSetup -> {
                            currentFlow = newState
                        }
                        is AuthUiState.Authenticated -> {
                            currentFlow = newState
                        }
                        is AuthUiState.Success -> {
                            val user = newState.user
                            val profile = com.example.data.supabase.SupabaseClient.currentProfile
                            val isComplete = profile?.isProfileComplete == true
                            
                            currentFlow = if (profile != null && isComplete) {
                                AuthUiState.Authenticated(user, profile)
                            } else if (profile != null) {
                                AuthUiState.NeedsProfileSetup(user, profile)
                            } else {
                                AuthUiState.NeedsProfileSetup(user, com.example.data.model.Profile(id = user.id, displayName = user.email?.substringBefore("@") ?: "", avatarUrl = null, isProfileComplete = false))
                            }
                        }
                        is AuthUiState.AuthenticatedReady -> {
                            currentFlow = AuthUiState.Authenticated(newState.user, newState.profile)
                        }
                        is AuthUiState.AuthenticatedIncomplete -> {
                            val user = newState.user
                            val profile = newState.profile
                            val isComplete = profile?.isProfileComplete == true
                            
                            currentFlow = if (profile != null && isComplete) {
                                AuthUiState.Authenticated(user, profile)
                            } else if (profile != null) {
                                AuthUiState.NeedsProfileSetup(user, profile)
                            } else {
                                AuthUiState.NeedsProfileSetup(user, com.example.data.model.Profile(id = user.id, displayName = user.email?.substringBefore("@") ?: "", avatarUrl = null, isProfileComplete = false))
                            }
                        }
                        // Loading, Error, and Idle do not change the active root layout tree!
                        is AuthUiState.Loading, is AuthUiState.Error, is AuthUiState.Idle -> {
                            // Keep previous flow
                        }
                    }
                }

                val isUserAuthenticated = remember(currentFlow) {
                    currentFlow is AuthUiState.Authenticated || 
                    currentFlow is AuthUiState.NeedsProfileSetup
                }

                var isPlayerFullVisible by remember { mutableStateOf(false) }
                val playerState by playerViewModel.playerState.collectAsState()

                if (com.example.util.AppFloatingPlayerManager.isInNativePip) {
                    val player = com.example.util.AppFloatingPlayerManager.exoPlayer
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (player != null) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        useController = false
                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        this.player = player
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = player
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Cargando video...", color = Color.White, fontSize = 14.sp)
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (isUserAuthenticated && playerState.currentTrack != null) {
                                com.example.media.player.ui.MiniPlayerBar(
                                    track = playerState.currentTrack,
                                    isPlaying = playerState.isPlaying,
                                    progress = if (playerState.durationMs > 0) playerState.currentPositionMs.toFloat() / playerState.durationMs else 0f,
                                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                    onNext = { playerViewModel.nextTrack() },
                                    onClick = { isPlayerFullVisible = true }
                                )
                            }
                        }
                    ) { innerPadding ->
                    if (isSplashActive) {
                        SplashScreen()
                    } else {
                        if (isUserAuthenticated) {
                            if (callState != com.example.call.CallState.IDLE) {
                                val opponentNameStr = opponentName ?: ""
                                val formattedDuration = callManager.formattedDuration()
                                val videoViewModel: com.example.call.VideoCallViewModel? = if (callType == com.example.call.CallType.VIDEO) {
                                    androidx.lifecycle.viewmodel.compose.viewModel()
                                } else {
                                    null
                                }
                                
                                com.example.ui.call.CallScreen(
                                    opponentId = opponentId,
                                    opponentName = opponentNameStr,
                                    callState = callState,
                                    callType = callType,
                                    formattedDuration = formattedDuration,
                                    isMuted = isMuted,
                                    isSpeakerOn = isSpeakerOn,
                                    isCameraOn = isCameraOn,
                                    videoViewModel = videoViewModel,
                                    onAcceptCall = { callManager.acceptCall() },
                                    onRejectCall = { callManager.rejectCall() },
                                    onEndCall = { callManager.endCall() },
                                    onMuteToggle = { callManager.toggleMute() },
                                    onSpeakerToggle = { callManager.toggleSpeaker() },
                                    onCameraToggle = { callManager.toggleCamera() },
                                    onSwitchCamera = { callManager.switchCamera() },
                                    onDismissError = { callManager.endCall() }
                                )
                            } else {
                                if (currentFlow is AuthUiState.NeedsProfileSetup) {
                                    com.example.ui.screen.onboarding.OnboardingNavHost(
                                        onOnboardingComplete = {
                                            authViewModel.onOnboardingComplete()
                                        }
                                    )
                                } else {
                                // ----------------------------------------------------
                                // MAIN FLOW: Completely isolated from Auth backstack
                                // ----------------------------------------------------
                                val mainNavController = rememberNavController()

                                val intentToProcess = currentIntentState.value
                                LaunchedEffect(intentToProcess) {
                                    intentToProcess?.let { intent ->
                                        val chatId = intent.getStringExtra("chat_id") ?: intent.getStringExtra("chatId")
                                        val stateId = intent.getStringExtra("state_id") ?: intent.getStringExtra("stateId")
                                        val type = intent.getStringExtra("notification_type") ?: intent.getStringExtra("notificationType")
                                        
                                        android.util.Log.d("MainActivity", "Deep Link Intent received - chatId: $chatId, stateId: $stateId, type: $type")
                                        
                                        if (type == "llamada_entrante") {
                                            val callerId = intent.getStringExtra("callerId") ?: ""
                                            val callerName = intent.getStringExtra("callerName") ?: ""
                                            val callTypeStr = intent.getStringExtra("callType") ?: "audio"
                                            val sdpStr = intent.getStringExtra("sdp") ?: "" // If available in FCM
                                            intent.removeExtra("notification_type")
                                            intent.removeExtra("notificationType")
                                            
                                            // Handle incoming call if CallManager is IDLE
                                            if (callManager.callState.value == com.example.call.CallState.IDLE) {
                                                callManager.handleFCMIncomingCall(callerId, callerName, callTypeStr)
                                            }
                                        } else if (!chatId.isNullOrEmpty()) {
                                            intent.removeExtra("chat_id")
                                            intent.removeExtra("chatId")
                                            delay(300)
                                            mainNavController.navigate("chat/$chatId/unknown") { launchSingleTop = true }
                                        } else if (!stateId.isNullOrEmpty()) {
                                            intent.removeExtra("state_id")
                                            intent.removeExtra("stateId")
                                            delay(300)
                                            if (type == "new_story") {
                                                mainNavController.navigate("viewState/$stateId") { launchSingleTop = true }
                                            } else if (type == "new_reel") {
                                                mainNavController.navigate("tiktok/$stateId") { launchSingleTop = true }
                                            } else if (type == "new_post" || type == "new_like" || type == "new_comment") {
                                                mainNavController.navigate("postDetail/$stateId") { launchSingleTop = true }
                                            }
                                        } else if (type == "system_news" || type == "app_update" || type == "new_content") {
                                            intent.removeExtra("notification_type")
                                            intent.removeExtra("notificationType")
                                            delay(300)
                                            mainNavController.navigate("notifications") { launchSingleTop = true }
                                        }
                                    }
                                }
                            Box(modifier = Modifier.fillMaxSize()) {
                                NavHost(
                            navController = mainNavController,
                            startDestination = "chatsList",
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            }
                        ) {
                            // Main Chats List & States Dashboard
                            composable("chatsList") {
                                ChatsListScreen(
                                    chatsViewModel = chatsViewModel,
                                    statesViewModel = statesViewModel,
                                    authViewModel = authViewModel,
                                    profileViewModel = profileViewModel,
                                    notificationsViewModel = notificationsViewModel,
                                    onNavigateToChat = { chatId, otherUserId ->
                                        mainNavController.navigate("chat/$chatId/$otherUserId") { launchSingleTop = true }
                                    },
                                    onNavigateToSearch = { mainNavController.navigate("search") { launchSingleTop = true } },
                                    onNavigateToCreateState = { mainNavController.navigate("createStory") { launchSingleTop = true } },
                                    onNavigateToCreateStory = { mainNavController.navigate("createStory") { launchSingleTop = true } },
                                    onNavigateToCreateReel = { mainNavController.navigate("createReel") { launchSingleTop = true } },
                                    onNavigateToViewState = { stateId ->
                                        mainNavController.navigate("viewState/$stateId") { launchSingleTop = true }
                                    },
                                    onNavigateToTikTok = { stateId ->
                                        mainNavController.navigate("tiktok/$stateId") { launchSingleTop = true }
                                    },
                                    onNavigateToProfile = { mainNavController.navigate("profile") { launchSingleTop = true } },
                                    onNavigateToUserProfile = { userId ->
                                        mainNavController.navigate("userProfile/$userId") { launchSingleTop = true }
                                    },
                                    onNavigateToNotifications = { mainNavController.navigate("notifications") { launchSingleTop = true } },
                                    onNavigateToFavorites = { mainNavController.navigate("favorites") { launchSingleTop = true } },
                                    onNavigateToMusic = { mainNavController.navigate("musicHome") { launchSingleTop = true } },
                                    onNavigateToVoiceRoom = { mainNavController.navigate("voiceRoom") { launchSingleTop = true } }
                                )
                            }

                            // Sala de Voz (modulo independiente com.example.rooms)
                            composable("voiceRoom") {
                                com.example.rooms.ui.VoiceRoomScreen(
                                    onBack = { mainNavController.popBackStack() }
                                )
                            }

                            // Music Studio Home
                            composable("musicHome") {
                                com.example.media.ui.MusicHomeScreen(
                                    onBackClick = { mainNavController.popBackStack() },
                                    onPlaylistClick = { playlistId -> 
                                        mainNavController.navigate("playlist/$playlistId")
                                    },
                                     onInvitationsClick = { mainNavController.navigate("playlist/invitations") },
                                    onPlayTrack = { track ->
                                        playerViewModel.playTrack(track)
                                    }
                                )
                            }

                            // Playlist Details
                            composable(
                                route = "playlist/{playlistId}",
                                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                                val playlistRepository = remember { com.example.media.playlist.PlaylistRepository(db.playlistDao(), db.collaboratorDao()) }
                                val invitationRepository = remember {
                                    com.example.media.playlist.PlaylistInvitationRepository(
                                        db.invitationDao(),
                                        com.example.data.supabase.SupabaseClient.apiService!!,
                                        com.example.data.supabase.SupabaseClient.supabaseAnonKey
                                    )
                                }
                                val audioRepository = remember { com.example.media.audio.AudioRepository(db.audioDao()) }
                                val playlistManager = remember { com.example.media.playlist.PlaylistManager(playlistRepository, audioRepository) }
                                
                                // P6.7.7A - Realtime Setup
                                val syncManager = remember {
                                    com.example.media.sync.MusicSocialSyncManager(
                                        context = context.applicationContext,
                                        supabaseApi = com.example.data.supabase.SupabaseClient.apiService!!,
                                        playlistRepo = playlistRepository,
                                        invitationRepo = invitationRepository,
                                        audioRepo = audioRepository,
                                        apiKey = com.example.data.supabase.SupabaseClient.supabaseAnonKey
                                    )
                                }
                                val realtimeManager = remember { com.example.media.sync.MusicPlaylistRealtimeManager(syncManager) }

                                val playlistViewModel: com.example.media.player.ui.PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = playlistId,
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return com.example.media.player.ui.PlaylistViewModel(
                                                playlistId,
                                                playlistManager,
                                                playlistRepository,
                                                invitationRepository,
                                                realtimeManager
                                            ) as T
                                        }
                                    }
                                )
                                
                                val uiState by playlistViewModel.uiState.collectAsState()
                                
                                uiState.playlist?.let { p ->
                                    com.example.media.ui.PlaylistScreen(
                                        playlist = p,
                                        songs = uiState.tracks,
                                        userRole = uiState.userRole,
                                        onBackClick = { mainNavController.popBackStack() },
                                        onPlayAllClick = { playlistViewModel.playAll(playerViewModel) },
                                        onShuffleClick = { playlistViewModel.shuffleAndPlay(playerViewModel) },
                                        onPlayTrackClick = { track -> playlistViewModel.playTrack(track, playerViewModel) },
                                        onRemoveTrackClick = { track -> playlistViewModel.removeTrack(track) },
                                        onSharePlaylistClick = { 
                                            android.widget.Toast.makeText(context, "Selecciona un chat para compartir", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                         onCollaboratorsClick = { mainNavController.navigate("playlist/$playlistId/collaborators") },
                                        onGenerateCoverClick = { 
                                            mainNavController.navigate("playlistCoverStudio/$playlistId")
                                        }
                                    )
                                } ?: run {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF38BDF8))
                                    }
                                }
                            }

                            composable(
                                route = "playlistCoverStudio/{playlistId}",
                                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                                
                                val repository = remember { com.example.media.playlist.cover.PlaylistCoverRepository(db.creativeProjectDao()) }
                                val storageManager = remember { com.example.media.storage.MediaStorageManager(context) }
                                val exporter = remember { com.example.media.playlist.cover.PlaylistCoverExporter(context, storageManager) }
                                
                                val coverViewModel: com.example.media.playlist.cover.PlaylistCoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return com.example.media.playlist.cover.PlaylistCoverViewModel(
                                                context.applicationContext as android.app.Application,
                                                repository,
                                                exporter
                                            ) as T
                                        }
                                    }
                                )

                                com.example.media.playlist.cover.PlaylistCoverStudioScreen(
                                    viewModel = coverViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onFinish = { _ -> mainNavController.popBackStack() }
                                )
                            }

                            composable(
                                route = "playlist/{playlistId}/collaborators",
                                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                                val playlistRepository = remember { com.example.media.playlist.PlaylistRepository(db.playlistDao(), db.collaboratorDao()) }
                                val invitationRepository = remember {
                                    com.example.media.playlist.PlaylistInvitationRepository(
                                        db.invitationDao(),
                                        com.example.data.supabase.SupabaseClient.apiService!!,
                                        com.example.data.supabase.SupabaseClient.supabaseAnonKey
                                    )
                                }
                                val audioRepository = remember { com.example.media.audio.AudioRepository(db.audioDao()) }
                                val playlistManager = remember { com.example.media.playlist.PlaylistManager(playlistRepository, audioRepository) }
                                
                                val viewModel: com.example.media.player.ui.PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = playlistId,
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return com.example.media.player.ui.PlaylistViewModel(
                                                playlistId,
                                                playlistManager,
                                                playlistRepository,
                                                invitationRepository,
                                                null
                                            ) as T
                                        }
                                    }
                                )
                                
                                com.example.media.collaboration.ui.PlaylistCollaboratorsScreen(
                                    playlistId = playlistId,
                                    viewModel = viewModel,
                                    onBack = { mainNavController.popBackStack() }
                                )
                            }

                            composable("playlist/invitations") {
                                com.example.media.collaboration.ui.PlaylistInvitationsScreen(
                                    onBack = { mainNavController.popBackStack() },
                                    onOpenPlaylist = { playlistId ->
                                        mainNavController.navigate("playlist/$playlistId")
                                    }
                                )
                            }

                            // Favorites / Saved Messages Screen
                            composable("favorites") {
                                FavoritesScreen(
                                    chatViewModel = chatViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onNavigateToChat = { chatId ->
                                        mainNavController.navigate("chat/$chatId/unknown") { launchSingleTop = true }
                                    }
                                )
                            }

                            // Search Users Screen
                            composable("search") {
                                SearchUsersScreen(
                                    viewModel = chatsViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onChatOpened = { chatId, otherUserId ->
                                        mainNavController.navigate("chat/$chatId/$otherUserId") { launchSingleTop = true; 
                                            popUpTo("search") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            
                            composable("notifications") {
                                com.example.ui.screen.NotificationsScreen(
                                    viewModel = notificationsViewModel,
                                    onNavigateBack = { mainNavController.popBackStack() },
                                    onNavigateToState = { stateId ->
                                        mainNavController.navigate("viewState/$stateId") { launchSingleTop = true }
                                    },
                                    onNavigateToChat = { chatId, otherUserId ->
                                        mainNavController.navigate("chat/$chatId/$otherUserId") { launchSingleTop = true }
                                    },
                                    onNavigateToProfile = { userId ->
                                        // Navigate to profile (we might not have another user profile view yet, so just fallback to chatsList)
                                        mainNavController.navigate("userProfile/$userId") { launchSingleTop = true }
                                    },
                                    onNavigateToReel = { reelId ->
                                        mainNavController.navigate("tiktok/$reelId") { launchSingleTop = true }
                                    },
                                    onNavigateToPostDetail = { postId ->
                                        mainNavController.navigate("postDetail/$postId") { launchSingleTop = true }
                                    }
                                )
                            }

                            // Messaging Chat Screen
                            composable(
                                route = "chat/{chatId}/{otherUserId}",
                                arguments = listOf(
                                    navArgument("chatId") { type = NavType.StringType },
                                    navArgument("otherUserId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                                val playlistRepo = remember { com.example.media.playlist.PlaylistRepository(db.playlistDao(), db.collaboratorDao()) }
                                val audioRepo = remember { com.example.media.audio.AudioRepository(db.audioDao()) }
                                val playlistManager = remember { com.example.media.playlist.PlaylistManager(playlistRepo, audioRepo) }

                                val scope = androidx.compose.runtime.rememberCoroutineScope()
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    onBack = { mainNavController.popBackStack() },
                                    onNavigateToChatMedia = { mainNavController.navigate("chatGallery/$chatId") },
                                    onNavigateToSearch = { mainNavController.navigate("chatSearch/$chatId") },
                                    navController = mainNavController,
                                    onPlaylistAction = { playlistId, action ->
                                        when (action) {
                                            "OPEN" -> mainNavController.navigate("playlist/$playlistId")
                                            "PLAY" -> {
                                                scope.launch {
                                                    val tracks = playlistRepo.getTracksForPlaylistSync(playlistId)
                                                    if (tracks.isNotEmpty()) {
                                                        playerViewModel.playTracks(tracks, 0)
                                                    }
                                                }
                                            }
                                            "SAVE" -> {
                                                scope.launch {
                                                    playlistManager.duplicatePlaylist(playlistId, "Copia de Playlist")
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            // Chat Media Gallery Screen
                            composable(
                                route = "chatGallery/{chatId}",
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                                ChatMediaGalleryScreen(
                                    chatId = chatId,
                                    onBack = { mainNavController.popBackStack() }
                                )
                            }

                            // Chat Search Screen
                            composable(
                                route = "chatSearch/{chatId}",
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                                ChatSearchScreen(
                                    chatId = chatId,
                                    onBack = { mainNavController.popBackStack() },
                                    onResultClick = { messageId ->
                                        // Save target message ID in a shared state or pass via navigation result
                                        mainNavController.previousBackStackEntry?.savedStateHandle?.set("targetMessageId", messageId)
                                        mainNavController.popBackStack()
                                    }
                                )
                            }

                            // Story Editor Screen (editor limpio, foto/vídeo/texto + audio real)
                            composable("createStory") {
                                CleanStoryEditorScreen(
                                    viewModel = statesViewModel,
                                    onBack = { mainNavController.popBackStack() }
                                )
                            }

                            // Reel Editor Screen (FASE 4B)
                            composable("createReel") {
                                ReelEditorScreen(
                                    viewModel = statesViewModel,
                                    onBack = { mainNavController.popBackStack() }
                                )
                            }

                            // View Status Player Screen
                            composable(
                                route = "viewState/{stateId}",
                                arguments = listOf(navArgument("stateId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val stateId = backStackEntry.arguments?.getString("stateId") ?: ""
                                ViewStateScreen(
                                    viewModel = statesViewModel,
                                    stateId = stateId,
                                    onClose = { mainNavController.popBackStack() },
                                    onNavigateToUserProfile = { userId ->
                                        mainNavController.navigate("userProfile/$userId") { launchSingleTop = true }
                                    }
                                )
                            }

                            // Post Detail Screen
                            composable(
                                route = "postDetail/{postId}",
                                arguments = listOf(navArgument("postId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                val feedViewModel: com.example.ui.viewmodel.FeedViewModel = viewModel()
                                PostDetailScreen(
                                    postId = postId,
                                    viewModel = feedViewModel,
                                    onBackClick = { mainNavController.popBackStack() }
                                )
                            }

                            // TikTok Video Feed Screen
                            composable(
                                route = "tiktok/{stateId}",
                                arguments = listOf(navArgument("stateId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val stateId = backStackEntry.arguments?.getString("stateId") ?: ""
                                TikTokVideoFeedScreen(
                                    viewModel = statesViewModel,
                                    initialStateId = stateId,
                                    onBack = { mainNavController.popBackStack() },
                                    onNavigateToUserProfile = { userId ->
                                        mainNavController.navigate("userProfile/$userId") { launchSingleTop = true }
                                    },
                                    onNavigateToHashtag = { tag ->
                                        mainNavController.navigate("search_results/$tag") { launchSingleTop = true }
                                    }
                                )
                            }
                            
                            // Search Results for Hashtag
                            composable(
                                route = "search_results/{tag}",
                                arguments = listOf(navArgument("tag") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val tag = backStackEntry.arguments?.getString("tag") ?: ""
                                com.example.ui.screen.SearchResultsScreen(
                                    tag = tag,
                                    viewModel = statesViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onVideoClick = { stateId ->
                                        mainNavController.navigate("tiktok/$stateId") { launchSingleTop = true }
                                    }
                                )
                            }

                            // Edit User Profile Screen
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    authViewModel = authViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onNavigateToReel = { reelId ->
                                        mainNavController.navigate("tiktok/$reelId") { launchSingleTop = true }
                                    }
                                )
                            }

                            // Public User Profile View
                            composable(
                                route = "userProfile/{userId}",
                                arguments = listOf(navArgument("userId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                UserProfileScreen(
                                    userId = userId,
                                    statesViewModel = statesViewModel,
                                    onBack = { mainNavController.popBackStack() },
                                    onNavigateToChat = { chatId, otherUserId ->
                                        mainNavController.navigate("chat/$chatId/$otherUserId") { launchSingleTop = true }
                                    },
                                    onNavigateToReel = { reelId ->
                                        mainNavController.navigate("tiktok/$reelId") { launchSingleTop = true }
                                    }
                                )
                            }
                        }
                        com.example.ui.components.FloatingPlayerBubble(
                            onNavigateToReels = { stateId ->
                                mainNavController.navigate("tiktok/$stateId") { launchSingleTop = true }
                            }
                        )
                        com.example.ui.components.FloatingVideoOverlay(
                            onNavigateBackToReels = {
                                mainNavController.navigate("clips") { launchSingleTop = true }
                            }
                        )

                        if (isPlayerFullVisible) {
                            com.example.media.player.ui.MusicPlayerScreen(
                                viewModel = playerViewModel,
                                onClose = { isPlayerFullVisible = false }
                            )
                        }
                    }
                    }
                    }
                } else {
                        // ----------------------------------------------------
                        // AUTHENTICATION FLOW: Isolated welcome/login/register/verification
                        // ----------------------------------------------------
                        val authNavController = rememberNavController()
                        val startDest = when (currentFlow) {
                            is AuthUiState.NeedsEmailVerification -> "verification/${(currentFlow as AuthUiState.NeedsEmailVerification).email}"
                            is AuthUiState.LoggedOut -> "welcome"
                            else -> "welcome"
                        }

                        NavHost(
                            navController = authNavController,
                            startDestination = startDest,
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            }
                        ) {
                            // Welcome Screen
                            composable("welcome") {
                                WelcomeScreen(
                                    onNavigateToLogin = { authNavController.navigate("login") { launchSingleTop = true } },
                                    onNavigateToRegister = { authNavController.navigate("register") { launchSingleTop = true } }
                                )
                            }

                            // Login Screen
                            composable("login") {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRegister = { authNavController.navigate("register") { launchSingleTop = true } }
                                )
                            }

                            // Register Screen
                            composable("register") {
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = { authNavController.navigate("login") { launchSingleTop = true } }
                                )
                            }

                            // Email Verification Screen
                            composable(
                                route = "verification/{email}",
                                arguments = listOf(navArgument("email") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val email = backStackEntry.arguments?.getString("email") ?: ""
                                EmailVerificationScreen(
                                    viewModel = authViewModel,
                                    email = email,
                                    onBackToLogin = {
                                        authViewModel.logout()
                                    }
                                )
                            }
                        }
                    }
                }
                }
                }
            }
        
            // App Lock overlay: drawn last so it covers the whole UI when locked.
            if (isAppLocked) {
                com.example.ui.security.LockScreen()
            }
}
    }
}
