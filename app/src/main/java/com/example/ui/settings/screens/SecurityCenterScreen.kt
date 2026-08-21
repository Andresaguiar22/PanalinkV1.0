package com.example.ui.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.QrCodeView
import com.example.ui.settings.models.SecurityAction
import com.example.ui.settings.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    onBack: () -> Unit,
    viewModel: SecurityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var newPinInput by remember { mutableStateOf("") }
    var scanInputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dispatch(SecurityAction.ClearMessages)
        }
        uiState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.dispatch(SecurityAction.ClearMessages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Seguridad", color = Color.White) },
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
                // Header Shield Card
                item {
                    val isProtected = uiState.hasPin || uiState.hasPattern
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isProtected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isProtected) Icons.Default.Shield else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isProtected) Color(0xFF25D366) else Color(0xFFFFB300),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isProtected) "Nivel de Protección: Alto 🛡️" else "Nivel de Protección: Estándar ⚠️",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isProtected) "PanaLink se bloqueará y pedirá tu PIN o patrón al abrirse." else "Configura un PIN o patrón para que la app se bloquee de verdad al abrirla.",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 1. PIN de Seguridad App
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PIN de Seguridad de la App",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    color = if (uiState.hasPin) Color(0xFF25D366).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.hasPin) "Configurado ✅" else "Sin Configurar",
                                        color = if (uiState.hasPin) Color(0xFF25D366) else Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Un PIN de acceso te permite proteger la aplicación frente a accesos no autorizados en tu dispositivo.",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.dispatch(SecurityAction.ShowPinDialog(true)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (uiState.hasPin) "Cambiar PIN" else "Configurar PIN",
                                        color = Color(0xFF121B22),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (uiState.hasPin) {
                                    OutlinedButton(
                                        onClick = { viewModel.dispatch(SecurityAction.RemovePin) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Eliminar PIN", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.5 Patrón de Desbloqueo
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Pattern,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Patrón de Desbloqueo",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Surface(
                                    color = if (uiState.hasPattern) Color(0xFF25D366).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.hasPattern) "Activo ✅" else "Inactivo",
                                        color = if (uiState.hasPattern) Color(0xFF25D366) else Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Desbloquea PanaLink dibujando un patrón en una cuadrícula de 3×3. Si ambos están activos, el patrón tiene prioridad.",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.dispatch(SecurityAction.ShowPatternDialog(true)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (uiState.hasPattern) "Cambiar Patrón" else "Configurar Patrón",
                                        color = Color(0xFF121B22),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (uiState.hasPattern) {
                                    OutlinedButton(
                                        onClick = { viewModel.dispatch(SecurityAction.RemovePattern) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Eliminar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.6 Bloqueo Automático
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bloqueo Automático",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Tiempo que puede pasar en segundo plano antes de volver a pedir tu código.",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    0L to "Inmediato",
                                    30_000L to "30 s",
                                    60_000L to "1 min",
                                    300_000L to "5 min"
                                ).forEach { (delayMs, label) ->
                                    val selected = uiState.autoLockMs == delayMs
                                    Surface(
                                        color = if (selected) Color(0xFF25D366).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = uiState.hasPin || uiState.hasPattern) {
                                                viewModel.dispatch(SecurityAction.SetAutoLock(delayMs))
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            color = when {
                                                selected -> Color(0xFF25D366)
                                                uiState.hasPin || uiState.hasPattern -> Color.White
                                                else -> Color.White.copy(alpha = 0.35f)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                            if (!uiState.hasPin && !uiState.hasPattern) {
                                Text(
                                    text = "Configura primero un PIN o patrón para activar el bloqueo automático.",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // 2. Identidad Digital y Código QR Pana
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Identidad Digital y QR Pana",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = "Tu PIN único de usuario y Código QR te identifican de forma segura en PanaLink.",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )

                            // Display PIN Code Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF101D24), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Tu PIN de Identidad", color = Color(0xFF90A4AE), fontSize = 10.sp)
                                    Text(
                                        text = uiState.userPinCode.ifEmpty { "PANA-78901" },
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Pana PIN", uiState.userPinCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "PIN copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar PIN", tint = Color.White)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.dispatch(SecurityAction.ShowQrDialog(true)) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ver mi QR Pana", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.dispatch(SecurityAction.ShowScanner(true)) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Escanear QR", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Autenticación en 2 Pasos & Biometría
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Bloquear ahora (real: activa el lock overlay al instante)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloquear Ahora",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Bloquea PanaLink al instante. Tendrás que desbloquear para seguir usándola.",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 11.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { com.example.security.AppLockManager.lockNow(context) },
                                    enabled = uiState.hasPin || uiState.hasPattern,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bloquear", fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Biometrics Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Desbloqueo Biométrico",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.biometricsAvailable)
                                            "Utiliza tu huella dactilar o reconocimiento facial para desbloquear PanaLink."
                                        else
                                            "No disponible: este dispositivo no tiene biometría configurada.",
                                        color = if (uiState.biometricsAvailable) Color(0xFF90A4AE) else Color(0xFFFFB300),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.isBiometricsEnabled,
                                    enabled = uiState.biometricsAvailable,
                                    onCheckedChange = { enabled ->
                                        viewModel.dispatch(SecurityAction.ToggleBiometrics(enabled))
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
            }
        }
    }

    // Dialog 1: Set/Edit PIN Dialog
    if (uiState.isPinDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dispatch(SecurityAction.ShowPinDialog(false)) },
            containerColor = Color(0xFF1E2D35),
            title = {
                Text(
                    text = if (uiState.hasPin) "Cambiar PIN de Seguridad" else "Configurar PIN de Seguridad",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingresa un PIN numérico de 4 a 6 dígitos para proteger la aplicación:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) newPinInput = it },
                        label = { Text("PIN de Seguridad", color = Color(0xFF90A4AE)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF25D366),
                            unfocusedBorderColor = Color(0xFF90A4AE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dispatch(SecurityAction.SetPin(newPinInput))
                        newPinInput = ""
                    }
                ) {
                    Text("Guardar", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dispatch(SecurityAction.ShowPinDialog(false)) }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Dialog 2: QR Code Viewer
    if (uiState.isQrDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dispatch(SecurityAction.ShowQrDialog(false)) },
            containerColor = Color(0xFF1E2D35),
            title = {
                Text(
                    text = "Tu Código QR de Identidad",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeView(
                            pin = uiState.userPinCode,
                            payload = "panalink:pin:${uiState.userPinCode}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "PIN: ${uiState.userPinCode}",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Muestra este código a otro Pana para que te agregue instantáneamente.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dispatch(SecurityAction.ShowQrDialog(false)) }) {
                    Text("Cerrar", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog 4: Pattern Setup (two-step: draw + confirm)
    if (uiState.isPatternDialogVisible) {
        var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
        var patternError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dispatch(SecurityAction.ShowPatternDialog(false)) },
            containerColor = Color(0xFF1E2D35),
            title = {
                Text(
                    text = if (firstPattern == null) "Dibuja tu nuevo patrón" else "Confirma tu patrón",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (firstPattern == null)
                            "Conecta al menos 4 puntos. Lo usarás para desbloquear PanaLink."
                        else
                            "Dibuja el mismo patrón otra vez para confirmarlo.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    com.example.ui.security.PatternPad(
                        onPatternComplete = { drawn ->
                            val first = firstPattern
                            when {
                                first == null -> {
                                    if (drawn.size < 4) {
                                        patternError = "Conecta al menos 4 puntos"
                                    } else {
                                        patternError = null
                                        firstPattern = drawn
                                    }
                                }
                                first == drawn -> {
                                    patternError = null
                                    viewModel.dispatch(SecurityAction.SetPattern(drawn))
                                }
                                else -> {
                                    patternError = "Los patrones no coinciden, inténtalo de nuevo"
                                    firstPattern = null
                                }
                            }
                        },
                        modifier = Modifier.size(240.dp)
                    )
                    patternError?.let {
                        Text(it, color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dispatch(SecurityAction.ShowPatternDialog(false)) }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Dialog 3: QR Scanner / Verification Dialog
    if (uiState.isScannerVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dispatch(SecurityAction.ShowScanner(false)) },
            containerColor = Color(0xFF1E2D35),
            title = {
                Text(
                    text = "Escanear QR de Pana",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ingresa o escanea el código payload recibido (ej: panalink:pin:123456):",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = scanInputText,
                        onValueChange = { scanInputText = it },
                        label = { Text("Payload de QR", color = Color(0xFF90A4AE)) },
                        placeholder = { Text("panalink:pin:123456", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF25D366),
                            unfocusedBorderColor = Color(0xFF90A4AE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dispatch(SecurityAction.ProcessScannedQr(scanInputText))
                        scanInputText = ""
                    }
                ) {
                    Text("Validar QR", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dispatch(SecurityAction.ShowScanner(false)) }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}
