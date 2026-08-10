package com.example.ui.screen.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.LocalAppColors
import com.example.ui.viewmodel.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val appColors = LocalAppColors.current
    val displayName by viewModel.displayName.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    
    val isValid = displayName.trim().length in 2..30
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setAvatarUri(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Configura tu perfil",
                color = appColors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Agrega una foto y tu nombre para que tus panas te reconozcan",
                color = appColors.textSecondary,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(appColors.surface)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(avatarUri),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Select Avatar",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = displayName,
                onValueChange = { viewModel.setDisplayName(it) },
                label = { Text("Nombre a mostrar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border,
                    focusedLabelColor = appColors.primary,
                    unfocusedLabelColor = appColors.textSecondary,
                    cursorColor = appColors.primary,
                    textColor = appColors.textPrimary
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primary,
                    disabledContainerColor = appColors.border
                )
            ) {
                Text(
                    text = "Continuar",
                    color = if (isValid) Color.White else appColors.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
