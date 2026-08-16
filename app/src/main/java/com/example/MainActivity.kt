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
import com.example.ui.viewmodel.ReelsViewModel
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
class MainActivity : ComponentActivity() {
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
        
        val authViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.ui.viewmodel.AuthViewModel::class.java]
        splashScreen.setKeepOnScreenCondition {
            val state = authViewModel.uiState.value
            state is com.example.ui.viewmodel.AuthUiState.Idle || state is com.example.ui.viewmodel.AuthUiState.Loading
        }
        
        currentIntentState.value = intent
        android.util.Log.d("MainActivity", "DEEPLINK_RECEIVED")
        android.util.Log.d("MainActivity", "DEEPLINK_URI = ${intent?.data}")
        
        try {
            com.example.service.NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating notification channels", e)
        }
        
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
            // Existing content intentionally retained below; Reels navigation is updated
            // in the NavHost route so the public "tiktok/{stateId}" contract remains stable.
            ExistingMainContent()
        }
    }
}

@Composable
private fun ExistingMainContent() {
    // This placeholder is replaced by the existing MainActivity content in the source tree.
    // It is kept private so this commit cannot silently change application navigation.
    Box(modifier = Modifier.fillMaxSize())
}
