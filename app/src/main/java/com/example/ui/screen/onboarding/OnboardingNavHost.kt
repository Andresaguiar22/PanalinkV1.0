package com.example.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.viewmodel.onboarding.OnboardingViewModel

@Composable
fun OnboardingNavHost(
    onOnboardingComplete: () -> Unit
) {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            OnboardingWelcomeScreen(
                onNext = { navController.navigate("setup_profile") }
            )
        }
        composable("setup_profile") {
            SetupProfileScreen(
                viewModel = onboardingViewModel,
                onNext = { navController.navigate("permissions") }
            )
        }
        composable("permissions") {
            PermissionsScreen(
                onNext = { navController.navigate("congrats") }
            )
        }
        composable("congrats") {
            OnboardingCongratsScreen(
                viewModel = onboardingViewModel,
                onNext = { navController.navigate("finalizing") }
            )
        }
        composable("finalizing") {
            FinalizingSetupScreen(
                viewModel = onboardingViewModel,
                onFinish = onOnboardingComplete
            )
        }
    }
}
