#!/bin/bash
SCREENS=("PresenceCenterScreen|Presencia" "PrivacyCenterScreen|Privacidad" "SecurityCenterScreen|Seguridad" "ChatsCenterScreen|Chats" "NotificationCenterScreen|Notificaciones" "CustomizationCenterScreen|Personalización" "StorageCenterScreen|Almacenamiento" "ActivityCenterScreen|Centro de Actividad" "AboutScreen|Acerca de")

for item in "${SCREENS[@]}"; do
    FILE_NAME="${item%%|*}"
    TITLE="${item##*|}"
    cat << TEMPLATE > "app/src/main/java/com/example/ui/settings/screens/${FILE_NAME}.kt"
package com.example.ui.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ${FILE_NAME}(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${TITLE}", color = Color.White) },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Pantalla ${TITLE}", color = Color.White)
        }
    }
}
TEMPLATE
done
