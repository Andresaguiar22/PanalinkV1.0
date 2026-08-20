package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PendingUploadEntity

@Composable
fun CompactUploadIndicator(
    upload: PendingUploadEntity,
    progress: Float,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F2C), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when (upload.uploadType) {
                "REEL" -> when (upload.status) {
                    "pending" -> "Esperando conexión..."
                    "uploading" -> "Publicando Reel..."
                    "failed" -> "Error al publicar Reel"
                    else -> "Publicando..."
                }
                "STATE" -> when (upload.status) {
                    "pending" -> "Esperando conexión..."
                    "uploading" -> "Publicando Historia..."
                    "failed" -> "Error al publicar Historia"
                    else -> "Publicando..."
                }
                else -> when (upload.status) {
                    "pending" -> "Esperando conexión..."
                    "uploading" -> "Publicando..."
                    "failed" -> "Error al publicar"
                    else -> "Publicando..."
                }
            }
            
            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            if (upload.status == "uploading") {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            
            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Cancelar", color = Color(0xFFFF5252), fontSize = 12.sp)
            }
        }
        
        if (upload.status == "uploading") {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF00FF85),
                trackColor = Color.DarkGray
            )
        }
    }
}
