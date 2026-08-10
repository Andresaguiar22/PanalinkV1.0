package com.example.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val status by viewModel.updateStatus.collectAsState()
    val info by viewModel.latestVersionInfo.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (status != UpdateStatus.UPDATE_AVAILABLE && status != UpdateStatus.MANDATORY_UPDATE) {
        return
    }

    val versionInfo = info ?: return
    val isMandatory = versionInfo.mandatory

    // Intercept back key to prevent dismissal of mandatory updates
    if (isMandatory) {
        BackHandler(enabled = true) {
            // No-op to disable back press dismiss
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isMandatory && downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("update_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Actualización",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isMandatory) "Actualización Obligatoria" else "Nueva Versión Disponible",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Versions Comparison Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121B22), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Instalada", color = Color(0xFF90A4AE), fontSize = 11.sp)
                        Text("v${viewModel.getInstalledVersionName()}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nueva versión", color = Color(0xFF90A4AE), fontSize = 11.sp)
                        Text("v${versionInfo.versionName}", color = Color(0xFF25D366), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog (Scrollable container)
                Text(
                    text = "Novedades:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (versionInfo.changelog.isEmpty()) {
                            Text("- Mejoras de estabilidad y rendimiento general.", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                        } else {
                            versionInfo.changelog.forEach { log ->
                                Text("• $log", color = Color(0xFFB0BEC5), fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Download Progress or Error State
                when (val state = downloadState) {
                    is DownloadState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    color = Color(0xFF25D366),
                                    trackColor = Color(0xFF2A3942),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .testTag("update_progress_bar")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Descargando... ${(state.progress * 100).toInt()}%",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = formatBytes(state.bytesDownloaded) + " / " + formatBytes(state.totalBytes),
                                        color = Color(0xFF90A4AE),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    color = Color(0xFF25D366),
                                    trackColor = Color(0xFF2A3942),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Descargando... " + formatBytes(state.bytesDownloaded),
                                    color = Color(0xFF90A4AE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is DownloadState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE57373).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF5350))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Error: ${state.message}",
                                color = Color(0xFFEF5350),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    else -> {
                        // Success or Idle states do not require inline progress
                    }
                }

                // Installation Error Alert
                errorMessage?.let { error ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE57373).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Error al instalar: $error",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dialog Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (downloadState is DownloadState.Downloading) {
                        TextButton(
                            onClick = {
                                viewModel.cancelDownload()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (!isMandatory) {
                            TextButton(
                                onClick = {
                                    viewModel.cancelDownload()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF90A4AE))
                            ) {
                                Text("Más tarde", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.startDownloadAndInstall()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.testTag("update_now_button")
                        ) {
                            Text(
                                text = if (downloadState is DownloadState.Error || errorMessage != null) "Reintentar" else "Actualizar ahora",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
