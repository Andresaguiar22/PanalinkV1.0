package com.example.media.collaboration.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.ProfilesRepository
import com.example.data.model.Profile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCollaboratorDialog(
    onDismiss: () -> Unit,
    onInvite: (String, String) -> Unit
) {
    val profilesRepo = remember { ProfilesRepository() }
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Profile?>(null) }
    var selectedRole by remember { mutableStateOf("EDITOR") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            profilesRepo.getMyContacts(forceRefresh = true).onSuccess {
                contacts = it
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    val filteredContacts = contacts.filter { 
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.id.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invitar Colaborador") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedContact == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar contacto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(filteredContacts) { profile ->
                                ListItem(
                                    headlineContent = { Text(profile.displayName) },
                                    supportingContent = { Text("@${profile.id}") },
                                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.clickable { selectedContact = profile }
                                )
                            }
                        }
                    }
                } else {
                    // Role selection for selected contact
                    Text(
                        text = "Seleccionado: ${selectedContact?.displayName}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedRole = "EDITOR" }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedRole == "EDITOR", onClick = { selectedRole = "EDITOR" })
                        Column {
                            Text("Editor")
                            Text("Puede agregar y quitar canciones", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedRole = "VIEWER" }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedRole == "VIEWER", onClick = { selectedRole = "VIEWER" })
                        Column {
                            Text("Lector")
                            Text("Solo puede ver la playlist", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    TextButton(onClick = { selectedContact = null }) {
                        Text("Cambiar contacto")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedContact?.let { onInvite(it.id, selectedRole) } },
                enabled = selectedContact != null
            ) {
                Text("Enviar Invitación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
