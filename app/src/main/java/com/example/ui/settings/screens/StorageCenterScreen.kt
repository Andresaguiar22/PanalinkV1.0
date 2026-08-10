package com.example.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.viewmodel.ActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCenterScreen(
    onBack: () -> Unit,
    viewModel: ActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Almacenamiento y datos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161618))
            )
        },
        containerColor = Color(0xFF161618)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Storage Section
            Text(
                text = "Almacenamiento",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )

            StorageItemRow(
                icon = Icons.Default.Folder,
                title = "Administrar almacenamiento",
                subtitle = uiState.storageUsed,
                onClick = { /* TODO */ }
            )

            StorageItemRow(
                icon = Icons.Default.DataUsage,
                title = "Uso de datos",
                subtitle = uiState.dataUsageToday,
                onClick = { /* TODO */ }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            // Auto-Download Section
            Text(
                text = "Descarga automática",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            
            Text(
                text = "Los mensajes de voz siempre se descargan automáticamente.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            var mobileDataPref by remember { mutableStateOf("Fotos") }
            var wifiDataPref by remember { mutableStateOf("Todos los archivos") }
            var roamingPref by remember { mutableStateOf("Ningún archivo") }

            StorageSettingRow(
                title = "Descargar con datos móviles",
                subtitle = mobileDataPref,
                onClick = { /* TODO */ }
            )

            StorageSettingRow(
                title = "Descargar con Wi-Fi",
                subtitle = wifiDataPref,
                onClick = { /* TODO */ }
            )

            StorageSettingRow(
                title = "En itinerancia de datos",
                subtitle = roamingPref,
                onClick = { /* TODO */ }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            // Media Upload Quality
            Text(
                text = "Calidad de carga de los archivos",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            var uploadQuality by remember { mutableStateOf("Automática (recomendada)") }
            
            StorageSettingRow(
                title = "Calidad de carga de fotos",
                subtitle = uploadQuality,
                onClick = { /* TODO */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StorageItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 16.sp)
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StorageSettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = subtitle, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
