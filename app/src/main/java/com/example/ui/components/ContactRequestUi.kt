package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Profile

/**
 * Diálogo final delim un (flujo minimalista) mostrado antes de enviar una
 * solicitud de contacto vía PIN/QR. Ayuda a que el flujo sea de `pre-crear`
 * a <->solicitante y evita envíos erróneos.
 */
@Composable
fun ContactRequestPreviewDialog(
    profile: Profile,
    pinOrToken: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1419),
        title = {
            Text("¿Quieres agregar a este contacto?", color = Color.White)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF00FF85), RoundedCornerShape(36.dp))
                        .padding(2.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = profile.displayName.ifNullOrBlank { "Pana ${pinOrToken.take(6)}" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PIN/QR: $pinOrToken",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cuando acepte, se agregarán como contacto mutuos. Si rechaza, se elimina de tu lista.",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))
            ) {
                Text("Enviar solicitud", color = Color.Black)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Banner sencillo para mostrar cada solicitud pendiente con accesos,
 * hecho para encajar en el row de ContactTabContent con Icon/Buttons.
 */
@Composable
fun ContactRequestRow(
    request: com.example.data.model.FriendRequestEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.example.ui.components.PanaAvatar(
            avatarUrl = request.sender?.avatarUrl,
            userId = request.sender?.id,
            placeholderName = request.sender?.displayName ?: "",
            size = 40.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.sender?.displayName ?: "Pana",
                color = Color.White
            )
            Text(
                text = "¿Aceptar o rechazar?",
                color = Color(0xFF90A4AE),
                fontSize = 12.sp
            )
        }
        Row() {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85)),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Aceptar", color = Color.Black, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Rechazar", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

private fun String.ifNullOrBlank(defaultValue: () -> String): String =
    if (isBlank()) defaultValue() else this
