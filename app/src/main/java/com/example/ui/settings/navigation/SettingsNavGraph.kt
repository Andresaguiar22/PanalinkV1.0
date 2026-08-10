package com.example.ui.settings.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.settings.screens.AboutScreen
import com.example.ui.settings.screens.ActivityCenterScreen
import com.example.ui.settings.screens.ChatsCenterScreen
import com.example.ui.settings.screens.ControlCenterScreen
import com.example.ui.settings.screens.CustomizationCenterScreen
import com.example.ui.settings.screens.NotificationCenterScreen
import com.example.ui.settings.screens.PresenceCenterScreen
import com.example.ui.settings.screens.PrivacyCenterScreen
import com.example.ui.settings.screens.ProfileEditScreen
import com.example.ui.settings.screens.SecurityCenterScreen
import com.example.ui.settings.screens.StorageCenterScreen

@Composable
fun SettingsNavGraph(
    onBackToMain: () -> Unit,
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SettingsDestination.Dashboard.route
    ) {
        composable(SettingsDestination.Dashboard.route) {
            ControlCenterScreen(
                onBack = onBackToMain,
                onNavigateToProfile = { navController.navigate(SettingsDestination.ProfileEdit.route) },
                onNavigateToPresence = { navController.navigate(SettingsDestination.PresenceCenter.route) },
                onNavigateToPrivacy = { navController.navigate(SettingsDestination.PrivacyCenter.route) },
                onNavigateToSecurity = { navController.navigate(SettingsDestination.SecurityCenter.route) },
                onNavigateToChats = { navController.navigate(SettingsDestination.ChatsCenter.route) },
                onNavigateToNotifications = { navController.navigate(SettingsDestination.NotificationCenter.route) },
                onNavigateToCustomization = { navController.navigate(SettingsDestination.CustomizationCenter.route) },
                onNavigateToStorage = { navController.navigate(SettingsDestination.StorageCenter.route) },
                onNavigateToActivity = { navController.navigate(SettingsDestination.ActivityCenter.route) },
                onNavigateToAbout = { navController.navigate(SettingsDestination.About.route) },
                onLogout = onLogout,
                onDeleteAccount = onDeleteAccount
            )
        }
        
        composable(SettingsDestination.ProfileEdit.route) {
            ProfileEditScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.PresenceCenter.route) {
            PresenceCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.PrivacyCenter.route) {
            PrivacyCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.SecurityCenter.route) {
            SecurityCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.ChatsCenter.route) {
            ChatsCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.NotificationCenter.route) {
            NotificationCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.CustomizationCenter.route) {
            CustomizationCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.StorageCenter.route) {
            StorageCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.ActivityCenter.route) {
            ActivityCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsDestination.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
