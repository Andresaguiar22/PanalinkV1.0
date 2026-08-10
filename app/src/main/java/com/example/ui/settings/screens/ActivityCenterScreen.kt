package com.example.ui.settings.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.ActivityAction
import com.example.ui.settings.models.DeviceInfo
import com.example.ui.settings.viewmodel.ActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCenterScreen(
    onBack: () -> Unit,
    viewModel: ActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.dispatch(ActivityAction.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Actividad", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.dispatch(ActivityAction.RefreshSummary) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
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
                // Header: Resumen de Uso Real
                item {
                    Text(
                        text = "Resumen de uso",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActivityStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Mensajes",
                            value = uiState.messagesCount.toString(),
                            icon = Icons.Default.Chat,
                            iconColor = Color(0xFF03A9F4)
                        )
                        ActivityStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Llamadas",
                            value = uiState.callsCount.toString(),
                            icon = Icons.Default.Call,
                            iconColor = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActivityStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Almacenamiento",
                            value = uiState.storageUsed,
                            icon = Icons.Default.Storage,
                            iconColor = Color(0xFFFF9800)
                        )
                        ActivityStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Red",
                            value = if (uiState.isOnline) "Conectado" else "Sin red",
                            icon = Icons.Default.Wifi,
                            iconColor = if (uiState.isOnline) Color(0xFF25D366) else Color(0xFFFF5252)
                        )
                    }
                }

                // Desglose de Almacenamiento Local
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Desglose de Almacenamiento Local",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            SystemStatusRow("Base de Datos (Room)", uiState.databaseSize, Icons.Default.Storage, Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.height(12.dp))
                            SystemStatusRow("Caché y Multimedia", uiState.mediaSize, Icons.Default.Folder, Color(0xFFFFB300))
                        }
                    }
                }

                // Estado del Sistema
                item {
                    Text(
                        text = "Estado del sistema",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SystemStatusRow(
                                title = "Sincronización de chats",
                                subtitle = uiState.lastSynchronization,
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SystemStatusRow(
                                title = "Calidad de conexión",
                                subtitle = uiState.connectionStatus,
                                icon = Icons.Default.Wifi,
                                iconColor = if (uiState.isOnline) Color(0xFF4CAF50) else Color(0xFFFF5252)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SystemStatusRow(
                                title = "Caché de datos en disco",
                                subtitle = uiState.dataUsageToday,
                                icon = Icons.Default.DataUsage,
                                iconColor = Color(0xFF03A9F4)
                            )
                        }
                    }
                }

                // Dispositivos Activos
                item {
                    Text(
                        text = "Dispositivos activos",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.activeDevices.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay dispositivos registrados", color = Color(0xFF90A4AE), fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.activeDevices.forEachIndexed { index, device ->
                                    DeviceRow(
                                        name = device.name,
                                        time = device.lastActive,
                                        icon = if (device.iconType == "computer") Icons.Default.Computer else Icons.Default.Smartphone,
                                        isCurrent = device.isCurrent
                                    )
                                    if (index < uiState.activeDevices.lastIndex) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E2B33),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color(0xFF90A4AE), fontSize = 13.sp)
        }
    }
}

@Composable
fun SystemStatusRow(title: String, subtitle: String, icon: ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF90A4AE), fontSize = 13.sp)
        }
    }
}

@Composable
fun DeviceRow(name: String, time: String, icon: ImageVector, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF2A3942), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(time, color = if (isCurrent) Color(0xFF4CAF50) else Color(0xFF90A4AE), fontSize = 12.sp)
            }
        }
    }
}
