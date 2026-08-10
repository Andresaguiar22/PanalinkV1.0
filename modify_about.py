import sys

with open("app/src/main/java/com/example/ui/settings/screens/AboutScreen.kt", "r") as f:
    content = f.read()

# I will replace the text and structure
new_about_screen = """package com.example.ui.settings.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Información", color = Color.White) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "PanaLink Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "PanaLink",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "v2.0 🇻🇪",
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "PanaLink es una plataforma de comunicación creada para mantener cerca a familiares, amigos y comunidades, sin importar dónde se encuentren. Nuestra misión es ofrecer una experiencia rápida, segura y confiable para conversar, compartir momentos y mantenerse siempre conectado.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B33)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutFeatureRow(
                            icon = Icons.Default.Security,
                            title = "Plataforma y Compatibilidad",
                            description = "Mensajería en tiempo real, optimizada para diferentes tipos de conexión."
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2A3942))
                        AboutFeatureRow(
                            icon = Icons.Default.Sync,
                            title = "Sincronización en la nube",
                            description = "Rápida, segura y confiable."
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2A3942))
                        AboutFeatureRow(
                            icon = Icons.Default.Description,
                            title = "Multimedia Integrada",
                            description = "Fotos, videos, documentos, notas de voz y llamadas."
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2A3942))
                        AboutFeatureRow(
                            icon = Icons.Default.HelpOutline,
                            title = "Soporte Técnico",
                            description = "Comunícate con el equipo de soporte desde la sección de Ayuda."
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Hecho con el ❤️ para la comunidad",
                    color = Color(0xFF607D8B),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "En PanaLink trabajamos continuamente para mejorar el rendimiento, incorporar nuevas funciones y ofrecer una experiencia estable, segura y fácil de usar para todos.",
                    color = Color(0xFF607D8B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }
}

@Composable
fun AboutFeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2A3942), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Color(0xFF90A4AE), fontSize = 13.sp)
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/settings/screens/AboutScreen.kt", "w") as f:
    f.write(new_about_screen)

