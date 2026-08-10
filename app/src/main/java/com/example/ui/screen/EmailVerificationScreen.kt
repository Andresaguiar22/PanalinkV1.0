package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AuroraBackground
import com.example.ui.components.AuroraButton
import com.example.ui.components.AuroraOutlinedButton
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel,
    email: String,
    onBackToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResendSuccess by remember { mutableStateOf(false) }

    val isAlreadyAuthenticated = uiState is AuthUiState.Authenticated ||
            uiState is AuthUiState.NeedsProfileSetup ||
            uiState is AuthUiState.AuthenticatedReady ||
            uiState is AuthUiState.AuthenticatedIncomplete

    // ON_RESUME listener when returning from email app / browser
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isAlreadyAuthenticated) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (!isAlreadyAuthenticated) {
                    viewModel.checkEmailVerificationStatus(silent = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Polling every 4 seconds while screen is active
    LaunchedEffect(isAlreadyAuthenticated) {
        if (!isAlreadyAuthenticated) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                viewModel.checkEmailVerificationStatus(silent = true)
            }
        }
    }

    AuroraBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.1f),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(48.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Verifica tu correo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Hemos enviado un correo mágico a tu bandeja. Verifícalo antes de iniciar sesión en Panalink.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                GlassCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuroraButton(
                            text = "Ya verifiqué",
                            onClick = {
                                if (!isAlreadyAuthenticated) {
                                    viewModel.checkVerification()
                                }
                            },
                            icon = Icons.Default.Refresh,
                            isLoading = uiState is AuthUiState.Loading,
                            modifier = Modifier.testTag("verify_confirm_button")
                        )
                        
                        AuroraOutlinedButton(
                            text = "Reenviar correo",
                            onClick = {
                                viewModel.resendVerificationEmail()
                                showResendSuccess = true
                            },
                            icon = Icons.Default.Email,
                            modifier = Modifier.testTag("verify_resend_button")
                        )
                        
                        if (showResendSuccess) {
                            Text(
                                text = "¡Correo reenviado con éxito! 📨",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Error indicator if verification fails
                        if (uiState is AuthUiState.Error) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (uiState as AuthUiState.Error).message,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TextButton(
                    onClick = onBackToLogin,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Volver al Inicio", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
