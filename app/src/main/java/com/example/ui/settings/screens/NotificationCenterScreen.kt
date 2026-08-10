package com.example.ui.settings.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.NotificationAction
import com.example.ui.settings.viewmodel.NotificationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "¡Permiso de notificaciones concedido! 🔔", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "El permiso de notificaciones es necesario para recibir alertas de nuevos mensajes.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val hasPostNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones de Pana", color = Color.White) },
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
            contentPadding = PaddingValues(16.dp),
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
                        // 1. POST_NOTIFICATIONS Permission Banner (Android 13+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE57373).copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color(0xFFE57373)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Alerta",
                                            tint = Color(0xFFE57373),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Notificaciones Desactivadas",
                                            color = Color(0xFFE57373),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "El sistema Android requiere tu permiso expreso para poder mostrarte alertas sonoras y visuales cuando recibes nuevos mensajes de Pana.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            postNotificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("Activar Notificaciones 🔔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A3942))
                        }

                        // 2. Global Notifications Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notificaciones Generales",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Activa o desactiva todas las notificaciones de la app.",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = uiState.globalEnabled,
                                onCheckedChange = {
                                    viewModel.dispatch(NotificationAction.SetGlobalEnabled(it))
                                },
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

            if (uiState.globalEnabled) {
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
                            // 3. Sound Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sonido de Notificación",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reproducir alertas sonoras al recibir mensajes.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.soundEnabled,
                                    onCheckedChange = {
                                        viewModel.dispatch(NotificationAction.SetSoundEnabled(it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF25D366),
                                        uncheckedThumbColor = Color(0xFF90A4AE),
                                        uncheckedTrackColor = Color(0xFF37474F)
                                    )
                                )
                            }

                            // Sound tone selection
                            if (uiState.soundEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Seleccionar Tono de Alerta",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val tones = listOf(
                                    "default" to "Tono del Sistema (Predeterminado)",
                                    "pana_beep" to "Pana Bip (Sintetizador)",
                                    "pana_double" to "Pana Doble Bip",
                                    "pana_pip" to "Pana Pip Rápido",
                                    "pana_high" to "Pana Alerta Aguda",
                                    "silent" to "Silencio"
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tones.forEach { (toneKey, toneLabel) ->
                                        val isSelected = uiState.soundTone == toneKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isSelected) Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF2A3942).copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFF25D366) else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    viewModel.dispatch(NotificationAction.SetSoundTone(toneKey))
                                                    com.example.service.NotificationHelper.playNotificationSound(context)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.dispatch(NotificationAction.SetSoundTone(toneKey))
                                                    com.example.service.NotificationHelper.playNotificationSound(context)
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF25D366),
                                                    unselectedColor = Color(0xFF90A4AE)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = toneLabel,
                                                color = if (isSelected) Color(0xFF25D366) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF2A3942))

                            // 4. Vibration Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vibración de Alerta",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Hacer vibrar el dispositivo al recibir mensajes.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.vibrationEnabled,
                                    onCheckedChange = {
                                        viewModel.dispatch(NotificationAction.SetVibrationEnabled(it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF25D366),
                                        uncheckedThumbColor = Color(0xFF90A4AE),
                                        uncheckedTrackColor = Color(0xFF37474F)
                                    )
                                )
                            }

                            // Vibration pattern selection
                            if (uiState.vibrationEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Seleccionar Patrón de Vibración",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val patterns = listOf(
                                    "default" to "Predeterminado (Medio)",
                                    "short" to "Corto",
                                    "long" to "Largo",
                                    "double" to "Doble",
                                    "triple" to "Triple"
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    patterns.forEach { (patternKey, patternLabel) ->
                                        val isSelected = uiState.vibrationPattern == patternKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isSelected) Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF2A3942).copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFF25D366) else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    viewModel.dispatch(NotificationAction.SetVibrationPattern(patternKey))
                                                    com.example.service.NotificationHelper.triggerVibration(context)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.dispatch(NotificationAction.SetVibrationPattern(patternKey))
                                                    com.example.service.NotificationHelper.triggerVibration(context)
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF25D366),
                                                    unselectedColor = Color(0xFF90A4AE)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = patternLabel,
                                                color = if (isSelected) Color(0xFF25D366) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
                            // 5. In-Chat Sound Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sonidos en el Chat",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reproducir sonidos suaves tipo gota para nuevos mensajes mientras estás dentro de un chat activo.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.chatSoundEnabled,
                                    onCheckedChange = {
                                        viewModel.dispatch(NotificationAction.SetChatSoundEnabled(it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF25D366),
                                        uncheckedThumbColor = Color(0xFF90A4AE),
                                        uncheckedTrackColor = Color(0xFF37474F)
                                    )
                                )
                            }

                            if (uiState.chatSoundEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Seleccionar Tono de Chat Activo",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val chatTones = listOf(
                                    "water_drop" to "Gota de Agua (Suave y Sutil)",
                                    "soft_pop" to "Pop Suave"
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    chatTones.forEach { (toneKey, toneLabel) ->
                                        val isSelected = uiState.chatSoundTone == toneKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isSelected) Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF2A3942).copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFF25D366) else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    viewModel.dispatch(NotificationAction.SetChatSoundTone(toneKey))
                                                    com.example.service.NotificationHelper.playActiveChatSound(context)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.dispatch(NotificationAction.SetChatSoundTone(toneKey))
                                                    com.example.service.NotificationHelper.playActiveChatSound(context)
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF25D366),
                                                    unselectedColor = Color(0xFF90A4AE)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = toneLabel,
                                                color = if (isSelected) Color(0xFF25D366) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF2A3942))

                            // 6. Outgoing Sounds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sonido de Envío",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reproducir un sonido suave tipo 'swoosh' al enviar tus propios mensajes.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.outgoingSoundEnabled,
                                    onCheckedChange = {
                                        viewModel.dispatch(NotificationAction.SetOutgoingSoundEnabled(it))
                                        if (it) {
                                            com.example.service.NotificationHelper.playOutgoingSound(context)
                                        }
                                    },
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
}
