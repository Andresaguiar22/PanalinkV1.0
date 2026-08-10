package com.example.ui.settings.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.PrivacyAction
import com.example.ui.settings.viewmodel.PrivacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dispatch(PrivacyAction.ClearMessages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad de Pana", color = Color.White) },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF25D366))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Control Total de tu Privacidad",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Tus preferencias se guardan de forma segura en tu dispositivo.",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 1. Last Seen
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Última vez y En línea",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "¿Quién puede ver cuándo estuviste en línea por última vez?",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )

                            val options = listOf("Todos", "Mis Contactos", "Nadie")
                            options.forEach { option ->
                                val isSelected = uiState.lastSeenVisibility.equals(option, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isSelected) Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF2A3942).copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF25D366) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.dispatch(PrivacyAction.UpdateLastSeen(option)) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.dispatch(PrivacyAction.UpdateLastSeen(option)) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF25D366),
                                            unselectedColor = Color(0xFF90A4AE)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        color = if (isSelected) Color(0xFF25D366) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Read Receipts
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Confirmaciones de Lectura",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Si desactivas esta opción, no podrás enviar ni recibir confirmaciones de lectura (doble tilde azul).",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.readReceiptsEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.dispatch(PrivacyAction.ToggleReadReceipts(enabled))
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

                // 3. Invisible / Ghost Mode
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.invisibleModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (uiState.invisibleModeEnabled) Color(0xFF00E5FF) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Modo Invisible (Ghost)",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Navega y lee mensajes sin mostrar el estado 'En línea' ni actualizar tu última conexión.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.invisibleModeEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.dispatch(PrivacyAction.ToggleInvisibleMode(enabled))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF00E5FF),
                                        uncheckedThumbColor = Color(0xFF90A4AE),
                                        uncheckedTrackColor = Color(0xFF37474F)
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Smart Read Receipts
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lectura Inteligente",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Envía la confirmación de lectura de manera inmediata al abrir la conversación.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.smartReadReceiptsEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.dispatch(PrivacyAction.ToggleSmartReadReceipts(enabled))
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

                // 5. Default Presence
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Estado de Presencia Predeterminado",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Define cómo te verán tus contactos por defecto al ingresar a la app.",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )

                            val presenceOptions = listOf(
                                "online" to "En línea 🟢",
                                "busy" to "Ocupado 🔴",
                                "invisible" to "Invisible ⚪"
                            )
                            presenceOptions.forEach { (statusKey, label) ->
                                val isSelected = uiState.profilePresence.equals(statusKey, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isSelected) Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF2A3942).copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF25D366) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.dispatch(PrivacyAction.UpdatePresence(statusKey)) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.dispatch(PrivacyAction.UpdatePresence(statusKey)) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF25D366),
                                            unselectedColor = Color(0xFF90A4AE)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF25D366) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
