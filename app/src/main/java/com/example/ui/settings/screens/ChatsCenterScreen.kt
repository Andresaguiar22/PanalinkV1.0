package com.example.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.ChatsSettingsAction
import com.example.ui.settings.viewmodel.ChatsSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsCenterScreen(
    onBack: () -> Unit,
    viewModel: ChatsSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats y Apariencia", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121B22))
            )
        },
        containerColor = Color(0xFF121B22)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Interactive Font Size Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tamaño del texto en los Chats",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${uiState.textSize.toInt()} sp",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = uiState.textSize,
                                onValueChange = { viewModel.dispatch(ChatsSettingsAction.UpdateTextSize(it)) },
                                valueRange = 12f..24f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF25D366),
                                    activeTrackColor = Color(0xFF25D366),
                                    inactiveTrackColor = Color(0xFF37474F)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // High fidelity live visual chat bubble preview!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (uiState.wallpaper == "classic_teal") Color(0xFF0F2027) else if (uiState.wallpaper == "midnight_blue") Color(0xFF0A0E17) else Color(0xFF0B141A),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "VISTA PREVIA EN VIVO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8596A0),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    
                                    // Received
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF202C33), RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                            .padding(10.dp)
                                            .align(Alignment.Start)
                                            .widthIn(max = 220.dp)
                                    ) {
                                        Text(
                                            text = "¿Qué pasó chamo? ¿Cómo vas?",
                                            color = Color.White,
                                            fontSize = uiState.textSize.sp,
                                            lineHeight = (uiState.textSize + 5).sp
                                        )
                                    }
                                    
                                    // Sent
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF005C4B), RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp))
                                            .padding(10.dp)
                                            .align(Alignment.End)
                                            .widthIn(max = 220.dp)
                                    ) {
                                        Text(
                                            text = "¡Todo fino de pana! Mira el tamaño de letra.",
                                            color = Color.White,
                                            fontSize = uiState.textSize.sp,
                                            lineHeight = (uiState.textSize + 5).sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2A3942))
                        
                        // 2. Wallpaper selector
                        Column {
                            Text(
                                text = "Fondo de Pantalla de Chats",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val wallpapers = listOf(
                                    "dark_slate" to "Gris Oscuro",
                                    "classic_teal" to "Azul Verdoso",
                                    "midnight_blue" to "Azul Medianoche"
                                )
                                wallpapers.forEach { (wpKey, wpLabel) ->
                                    val isSelected = uiState.wallpaper == wpKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = when (wpKey) {
                                                    "classic_teal" -> Color(0xFF0F2027)
                                                    "midnight_blue" -> Color(0xFF0A0E17)
                                                    else -> Color(0xFF0B141A)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF25D366) else Color(0xFF37474F),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.dispatch(ChatsSettingsAction.SetWallpaper(wpKey)) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = wpLabel,
                                            color = if (isSelected) Color(0xFF25D366) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2A3942))
                        
                        // 3. Enter Sends Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enter para enviar mensaje",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "La tecla Enter enviará tus mensajes de chat directamente.",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = uiState.enterSends,
                                onCheckedChange = { viewModel.dispatch(ChatsSettingsAction.SetEnterSends(it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF25D366),
                                    uncheckedThumbColor = Color(0xFF90A4AE),
                                    uncheckedTrackColor = Color(0xFF37474F)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
