package com.example.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors
import com.example.ui.viewmodel.onboarding.OnboardingUiState
import com.example.ui.viewmodel.onboarding.OnboardingViewModel

@Composable
fun FinalizingSetupScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.finalizeOnboarding(context)
    }

    LaunchedEffect(uiState) {
        if (uiState is OnboardingUiState.Success) {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is OnboardingUiState.Loading, OnboardingUiState.Idle -> {
                    CircularProgressIndicator(color = appColors.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Configurando tu perfil...",
                        color = appColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                is OnboardingUiState.Error -> {
                    Text(
                        text = "No hay conexión. Revisa tu internet e intenta nuevamente.",
                        color = appColors.error,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.finalizeOnboarding(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                    ) {
                        Text("Reintentar", color = Color.White)
                    }
                }
                is OnboardingUiState.Success -> {
                    // Handled by LaunchedEffect
                }
            }
        }
    }
}
