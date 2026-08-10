package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Message
import com.example.ui.components.chat.bubble.MessageBubbleEngine
import com.example.ui.components.chat.bubble.MessageGroupPosition
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.AudioPlayer
import com.example.data.supabase.SupabaseClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val favoritedMessages by com.example.data.repository.MessagesRepository.getInstance()
        .getFavoritedMessagesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val audioPlayer = remember { com.example.util.AudioPlayer() }
    var playingAudioUrl by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mensajes guardados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${favoritedMessages.size} mensajes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8596A0)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF202C33),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B141A)
    ) { padding ->
        if (favoritedMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔖", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No tienes mensajes guardados",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Mantén presionado un mensaje para guardarlo",
                        color = Color(0xFF8596A0),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0B141A)),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(favoritedMessages, key = { index, message -> "${message.id}_$index" }) { _, message ->
                    val isMe = message.senderId == (SupabaseClient.currentUser?.id ?: "")
                    
                    MessageBubbleEngine(
                        message = message,
                        isMe = isMe,
                        groupPosition = MessageGroupPosition.SINGLE,
                        myAvatarUrl = SupabaseClient.currentProfile?.avatarUrl,
                        otherAvatarUrl = null, // In favorites we might not easily have the other user profile here
                        textSizeSp = 15f,
                        allMessages = favoritedMessages,
                        onReply = { /* Not applicable in favorites yet */ },
                        onDeleteForMe = { /* Already handled in menu */ },
                        onDeleteForEveryone = { /* Only if owner/admin */ },
                        onForward = { /* Forward favorite */ },
                        isSelected = false,
                        onSelect = { 
                            // Telegram style: click on saved message takes you to the chat
                            onNavigateToChat(message.chatId)
                        },
                        onImageClick = { /* View image */ },
                        playingAudioUrl = playingAudioUrl,
                        isAudioPlaying = isAudioPlaying,
                        audioProgress = audioProgress,
                        audioDurationMs = 0,
                        audioCurrentPositionMs = 0,
                        audioPlayer = audioPlayer,
                        onAudioPlayStateChange = { url, play ->
                            playingAudioUrl = url
                            isAudioPlaying = play
                        },
                        reactions = emptyMap(),
                        onReact = { /* Reactions in favorites? maybe */ },
                        onToggleFavorite = { chatViewModel.toggleFavorite(it) }
                    )
                }
            }
        }
    }
}
