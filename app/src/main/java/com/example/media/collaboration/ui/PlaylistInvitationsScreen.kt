package com.example.media.collaboration.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.player.ui.PlaylistViewModel
import com.example.media.playlist.PlaylistInvitationEntity
import com.example.media.playlist.PlaylistInvitationRepository
import com.example.data.database.PanalinkDatabase
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistInvitationsScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = PanalinkDatabase.getDatabase(context)
    val invitationRepo = remember { 
        PlaylistInvitationRepository(
            db.invitationDao(),
            SupabaseClient.apiService!!,
            SupabaseClient.supabaseAnonKey
        )
    }
    val currentUserId = SessionManager.getCurrentUserId() ?: ""
    val invitations by invitationRepo.observeReceivedInvitations(currentUserId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invitaciones de Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (invitations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes invitaciones pendientes")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(invitations) { invitation ->
                    ReceivedInvitationItem(
                        invitation = invitation,
                        onAccept = { 
                            scope.launch {
                                val authHeader = "Bearer ${SessionManager.getUserAuthToken()}"
                                invitationRepo.acceptInvitation(invitation.id, authHeader)
                                onOpenPlaylist(invitation.playlistId)
                            }
                        },
                        onReject = {
                            scope.launch {
                                val authHeader = "Bearer ${SessionManager.getUserAuthToken()}"
                                invitationRepo.rejectInvitation(invitation.id, authHeader)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReceivedInvitationItem(
    invitation: PlaylistInvitationEntity,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Invitación a colaborar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "De: ${invitation.senderId}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Playlist: ${invitation.playlistId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Rol: ${invitation.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aceptar")
                }
            }
        }
    }
}
