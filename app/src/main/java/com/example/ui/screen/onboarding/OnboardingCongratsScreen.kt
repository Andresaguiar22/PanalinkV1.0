package com.example.ui.screen.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.onboarding.OnboardingViewModel

data class PresetAvatar(val emoji: String, val brush: Brush)

@Composable
fun OnboardingCongratsScreen(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val displayName by viewModel.displayName.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()

    // Grab preset background if avatarUrl is preset
    val presets = remember {
        listOf(
            PresetAvatar("🔥", Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFF9800)))),
            PresetAvatar("⚡", Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)))),
            PresetAvatar("👾", Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
            PresetAvatar("🚀", Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Party Popper Celebration Emoji
            Text(
                text = "🎉🥳✨",
                fontSize = 54.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "¡Felicidades, ya eres un Pana! 🇻🇪",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¡Gracias por elegirnos! Panalink ha sido creada con mucho cariño para mantenerte conectado con alta fidelidad, rapidez y absoluta confianza.",
                fontSize = 15.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Profile Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar view
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.5.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            if (avatarUrl!!.startsWith("preset:")) {
                                val symbol = avatarUrl!!.removePrefix("preset:")
                                val preset = presets.firstOrNull { it.emoji == symbol }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(preset?.brush ?: presets[0].brush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = symbol, fontSize = 42.sp)
                                }
                            } else {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase()?.toString() ?: "P",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Panalink Oficial Member ⚡",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00E5FF)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val currentProfile = com.example.data.supabase.SupabaseClient.currentProfile
                    val pin = currentProfile?.pin ?: ""
                    if (pin.isNotEmpty()) {
                        Text(
                            text = "Tu Código PIN Único",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = pin,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Tu Código QR de Pana",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.QrCodeView(
                                pin = pin,
                                payload = if (pin.isNotEmpty()) "panalink:pin:$pin" else "panalink:contact:${currentProfile?.id}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Esta será tu identidad en Panalink.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start chat button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_continue_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continuar y Entrar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
