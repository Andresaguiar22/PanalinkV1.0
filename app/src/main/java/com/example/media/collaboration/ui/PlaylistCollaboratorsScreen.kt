package com.example.media.collaboration.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.player.ui.PlaylistViewModel
import com.example.media.playlist.PlaylistCollaboratorEntity
import com.example.media.playlist.PlaylistInvitationEntity
import com.example.media.playlist.PlaylistMemberRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCollaboratorsScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Colaboradores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.userRole == PlaylistMemberRole.OWNER) {
                FloatingActionButton(onClick = { showInviteDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Invitar")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Miembros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Owner
                    uiState.playlist?.let { p ->
                        item {
                            CollaboratorItem(
                                name = "Propietario (${p.ownerId})", // TODO: Resolve name
                                role = "OWNER",
                                isMe = false, // TODO: Check if me
                                canManage = false
                            )
                        }
                    }

                    // Effective Collaborators
                    items(uiState.collaborators) { collaborator ->
                        CollaboratorItem(
                            name = collaborator.userId, // TODO: Resolve name
                            role = collaborator.role,
                            isMe = false, // TODO: Check if me
                            canManage = uiState.userRole == PlaylistMemberRole.OWNER,
                            onRemove = { viewModel.removeCollaborator(collaborator.id) },
                            onUpdateRole = { newRole -> viewModel.updateCollaboratorRole(collaborator.id, newRole) }
                        )
                    }

                    if (uiState.invitations.any { it.status == "PENDING" }) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Invitaciones Pendientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(uiState.invitations.filter { it.status == "PENDING" }) { invitation ->
                            InvitationItem(
                                name = invitation.receiverId, // TODO: Resolve name
                                role = invitation.role,
                                canRevoke = uiState.userRole == PlaylistMemberRole.OWNER,
                                onRevoke = { viewModel.revokeInvitation(invitation.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        InviteCollaboratorDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { contactId, role ->
                viewModel.inviteCollaborator(contactId, role)
                showInviteDialog = false
            }
        )
    }
}

@Composable
fun CollaboratorItem(
    name: String,
    role: String,
    isMe: Boolean,
    canManage: Boolean,
    onRemove: (() -> Unit)? = null,
    onUpdateRole: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (canManage && role != "OWNER") {
                Row {
                    IconButton(onClick = { 
                        val nextRole = if (role == "EDITOR") "VIEWER" else "EDITOR"
                        onUpdateRole?.invoke(nextRole)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Cambiar Rol")
                    }
                    IconButton(onClick = { onRemove?.invoke() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun InvitationItem(
    name: String,
    role: String,
    canRevoke: Boolean,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Invitado como $role", style = MaterialTheme.typography.bodySmall)
            }

            if (canRevoke) {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Default.Close, contentDescription = "Revocar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
