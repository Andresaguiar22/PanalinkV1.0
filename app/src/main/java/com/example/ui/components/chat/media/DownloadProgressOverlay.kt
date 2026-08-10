package com.example.ui.components.chat.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay circular con animación de progreso e icono de cancelar/pausar
 * para descargas o cargas de multimedia y documentos.
 */
@Composable
fun DownloadProgressOverlay(
    isVisible: Boolean,
    progress: Float? = null, // Null = Indeterminado
    isUploading: Boolean = false,
    isError: Boolean = false,
    onCancelOrRetryClick: () -> Unit = {},
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2C34).copy(alpha = 0.85f))
                        .clickable { onCancelOrRetryClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isError) {
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(50.dp),
                                color = Color(0xFF00A884),
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 3.dp
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp),
                                color = Color(0xFF00A884),
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 3.dp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isUploading) "Cancelar subida" else "Cancelar descarga",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (progress != null && !isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!statusText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText!!,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
