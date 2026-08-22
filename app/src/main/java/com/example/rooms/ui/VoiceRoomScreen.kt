package com.example.rooms.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rooms.model.VoiceRoomSeat
import kotlinx.coroutines.launch

/**
 * Pantalla de la Sala de Voz (Fase 1): 1 sillon superior + 6 inferiores,
 * chat de texto y controles de microfono. Todo el estado vive en el ViewModel.
 */
@Composable
fun VoiceRoomScreen(
    onBack: () -> Unit,
    viewModel: VoiceRoomViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    LaunchedEffect(Unit) { viewModel.enterRoom() }

    DisposableEffect(Unit) {
        onDispose { viewModel.leaveRoom() }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.room?.name ?: "Sala de voz",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${state.memberCount} en la sala",
                        color = Color.Gray, fontSize = 11.sp
                    )
                }
            }

            // Sillon superior central (indice 0)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SeatView(
                    seat = state.seats.getOrNull(0) ?: VoiceRoomSeat(index = 0),
                    isMine = state.seats.getOrNull(0)?.userId == state.myUserId,
                    size = 88.dp,
                    onClick = { viewModel.onSeatClicked(0, hasAudioPermission) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sillones inferiores: 3 + 3
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(listOf(1, 2, 3), listOf(4, 5, 6)).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { idx ->
                            SeatView(
                                seat = state.seats.getOrNull(idx) ?: VoiceRoomSeat(index = idx),
                                isMine = state.seats.getOrNull(idx)?.userId == state.myUserId,
                                size = 64.dp,
                                onClick = { viewModel.onSeatClicked(idx, hasAudioPermission) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF262629))
            Text(
                "Chat de la sala",
                color = Color.Gray, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Chat
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    val mine = msg.senderId == state.myUserId
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (mine) Color(0xFF2E7D5B) else Color(0xFF1E1E22),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(msg.content, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
            }

            // Input de chat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3A3A3E),
                        unfocusedBorderColor = Color(0xFF262629)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color(0xFF76CE9F))
                }
            }

            // Controles: micro / mute / salir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro: pedir permiso / sentarse en el primer sillon libre
                ControlButton(
                    icon = Icons.Default.Mic,
                    tint = if (state.isMicEnabled) Color(0xFF4CAF50) else Color.White,
                    background = Color(0xFF1E1E22)
                ) {
                    if (!hasAudioPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (!state.isSeated) {
                        val free = com.example.rooms.model.VoiceRoomSeatReducer.firstFreeSeatIndex(state.seats)
                        if (free != null) viewModel.onSeatClicked(free, hasAudioPermission)
                    }
                }
                // Mute (solo si estoy sentado)
                ControlButton(
                    icon = Icons.Default.Call, // placeholder icono; el estado real lo marca el color
                    tint = if (state.mySeat?.isMuted == true) Color(0xFFFF5252) else Color.White,
                    background = if (state.mySeat?.isMuted == true) Color(0xFF3A1E1E) else Color(0xFF1E1E22)
                ) {
                    if (state.isSeated) viewModel.toggleMute()
                }
                // Salir
                ControlButton(
                    icon = Icons.Default.ExitToApp,
                    tint = Color(0xFFFF5252),
                    background = Color(0xFF1E1E22)
                ) {
                    scope.launch {
                        viewModel.leaveRoom()
                        onBack()
                    }
                }
            }

            if (state.isJoining) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF76CE9F)
                )
            }
        }
    }
}

@Composable
private fun SeatView(
    seat: VoiceRoomSeat,
    isMine: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val borderColor = when {
        seat.isSpeaking -> Color(0xFF4CAF50)
        isMine -> Color(0xFF76CE9F)
        seat.isOccupied -> Color(0xFF3A3A3E)
        else -> Color(0xFF262629)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (seat.isOccupied) Color(0xFF1E1E22) else Color(0xFF141416))
                .border(2.dp, borderColor, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (seat.isOccupied) {
                Text(
                    text = (seat.displayName?.take(1) ?: "👤"),
                    color = Color.White,
                    fontSize = if (size > 80.dp) 26.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (seat.isMuted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔇", fontSize = 9.sp)
                    }
                }
            } else {
                Text("🎙️", fontSize = if (size > 80.dp) 22.sp else 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                seat.isOccupied -> seat.displayName ?: "Pana"
                else -> "Libre"
            },
            color = if (seat.isOccupied) Color.White else Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
