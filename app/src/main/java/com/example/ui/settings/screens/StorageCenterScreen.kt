package com.example.ui.settings.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.SettingsKeys
import com.example.ui.settings.viewmodel.ActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCenterScreen(
    onBack: () -> Unit,
    viewModel: ActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(SettingsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var showStorageDialog by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }

    var autoMobile by remember { mutableStateOf(prefs.getBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_MOBILE, true)) }
    var autoWifi by remember { mutableStateOf(prefs.getBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_WIFI, true)) }
    var autoRoaming by remember { mutableStateOf(prefs.getBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_ROAMING, false)) }
    var uploadQuality by remember { mutableStateOf(prefs.getString(SettingsKeys.STORAGE_UPLOAD_QUALITY, "auto") ?: "auto") }

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
                subtitle = "${uiState.storageUsed} • DB ${uiState.databaseSize} • Medios ${uiState.mediaSize}",
                onClick = { showStorageDialog = true }
            )

            StorageItemRow(
                icon = Icons.Default.DataUsage,
                title = "Uso de datos (esta app)",
                subtitle = uiState.dataUsageToday,
                onClick = { }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Descarga automática de medios",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Controla si PanaLink pre-descarga fotos y vídeos en segundo plano según tu red.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            StorageSwitchRow(
                title = "Descargar con datos móviles",
                checked = autoMobile,
                onCheckedChange = {
                    autoMobile = it
                    prefs.edit().putBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_MOBILE, it).apply()
                }
            )
            StorageSwitchRow(
                title = "Descargar con Wi-Fi",
                checked = autoWifi,
                onCheckedChange = {
                    autoWifi = it
                    prefs.edit().putBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_WIFI, it).apply()
                }
            )
            StorageSwitchRow(
                title = "Descargar en itinerancia (roaming)",
                checked = autoRoaming,
                onCheckedChange = {
                    autoRoaming = it
                    prefs.edit().putBoolean(SettingsKeys.STORAGE_AUTO_DOWNLOAD_ROAMING, it).apply()
                }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Calidad de carga de fotos",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            listOf(
                "auto" to "Automática (recomendada)",
                "high" to "Alta calidad (más datos)",
                "data_saver" to "Ahorro de datos"
            ).forEach { (key, label) ->
                StorageSettingRow(
                    title = label,
                    subtitle = if (uploadQuality == key) "Seleccionada ✅" else "",
                    onClick = {
                        uploadQuality = key
                        prefs.edit().putString(SettingsKeys.STORAGE_UPLOAD_QUALITY, key).apply()
                        Toast.makeText(context, "Calidad de carga: $label", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCleaning) showStorageDialog = false },
            containerColor = Color(0xFF1E2D35),
            title = { Text("Administrar almacenamiento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Uso local actual:", color = Color(0xFF90A4AE), fontSize = 12.sp)
                    Text("• Total: ${uiState.storageUsed}", color = Color.White, fontSize = 13.sp)
                    Text("• Base de datos (Room): ${uiState.databaseSize}", color = Color.White, fontSize = 13.sp)
                    Text("• Medios y archivos: ${uiState.mediaSize}", color = Color.White, fontSize = 13.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "Limpiar caché elimina imágenes, miniaturas y descargas temporales. Tus mensajes y fotos enviadas/recibidas no se borran.",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isCleaning,
                    onClick = {
                        isCleaning = true
                        scope.launch {
                            val freed = withContext(Dispatchers.IO) { clearAppCache(context) }
                            isCleaning = false
                            showStorageDialog = false
                            viewModel.dispatch(com.example.ui.settings.models.ActivityAction.RefreshStorage)
                            Toast.makeText(context, "Caché liberada: $freed", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (isCleaning) "Limpiando..." else "Limpiar caché", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isCleaning, onClick = { showStorageDialog = false }) {
                    Text("Cerrar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

private fun clearAppCache(context: android.content.Context): String {
    var freed = 0L
    fun sizeOf(file: java.io.File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { sizeOf(it) } ?: 0L
    }
    try {
        val targets = listOfNotNull(context.cacheDir, context.externalCacheDir)
        for (dir in targets) {
            freed += sizeOf(dir)
            dir.deleteRecursively()
            dir.mkdirs()
        }
    } catch (_: Exception) { }
    return when {
        freed >= 1024 * 1024 * 1024 -> "%.1f GB".format(freed / (1024.0 * 1024 * 1024))
        freed >= 1024 * 1024 -> "%.1f MB".format(freed / (1024.0 * 1024))
        freed >= 1024 -> "%.1f KB".format(freed / 1024.0)
        else -> "$freed B"
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
fun StorageSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF25D366),
                uncheckedThumbColor = Color(0xFF90A4AE),
                uncheckedTrackColor = Color(0xFF37474F)
            )
        )
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
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = Color(0xFF25D366), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
