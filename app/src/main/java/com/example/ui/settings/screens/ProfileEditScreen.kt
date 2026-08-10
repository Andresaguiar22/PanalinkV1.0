package com.example.ui.settings.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.components.*
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.SaveProfileUiState
import com.example.util.PanalinkMediaManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    // Form fields local states
    var displayName by remember { mutableStateOf("") }
    var selectedAvatarUrl by remember { mutableStateOf("") }
    var selectedCoverUrl by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf<List<String>>(emptyList()) }

    // Custom Profile Photo upload states
    var uploadStatusMessage by remember { mutableStateOf("") }
    var isUploadingCustomPhoto by remember { mutableStateOf(false) }
    var isUploadingCustomCover by remember { mutableStateOf(false) }

    // Image Launchers
    val profileGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            isUploadingCustomPhoto = true
            uploadStatusMessage = "Subiendo tu foto de perfil de Pana... 📸"
            scope.launch {
                try {
                    val localPath = PanalinkMediaManager.saveMediaToLocal(
                        context = context,
                        uri = uri,
                        sourceFile = null,
                        fileName = "profile_avatar_${System.currentTimeMillis()}.jpg"
                    )
                    if (localPath != null) {
                        viewModel.uploadProfilePhoto(
                            context = context,
                            file = File(localPath),
                            mimeType = mime,
                            onSuccess = { url ->
                                selectedAvatarUrl = url
                                isUploadingCustomPhoto = false
                                uploadStatusMessage = "¡Foto de perfil subida exitosamente! 🎉"
                                viewModel.saveProfile(
                                    displayName = displayName,
                                    avatarUrl = url,
                                    firstName = firstName,
                                    lastName = lastName,
                                    status = statusText,
                                    birthDate = birthDate,
                                    sex = sex,
                                    interests = interests,
                                    coverUrl = selectedCoverUrl,
                                    avatarLocalPath = localPath
                                )
                            },
                            onFailure = { error ->
                                isUploadingCustomPhoto = false
                                uploadStatusMessage = "Error al subir foto: $error 😢"
                            }
                        )
                    } else {
                        isUploadingCustomPhoto = false
                        uploadStatusMessage = "Error al guardar la foto de perfil temporalmente 😢"
                    }
                } catch (e: Exception) {
                    isUploadingCustomPhoto = false
                    uploadStatusMessage = "Error al procesar la imagen: ${e.localizedMessage} 😢"
                }
            }
        }
    }

    val coverGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            isUploadingCustomCover = true
            uploadStatusMessage = "Subiendo tu portada de Pana... 🎨"
            scope.launch {
                try {
                    val localPath = PanalinkMediaManager.saveMediaToLocal(
                        context = context,
                        uri = uri,
                        sourceFile = null,
                        fileName = "profile_cover_${System.currentTimeMillis()}.jpg"
                    )
                    if (localPath != null) {
                        viewModel.uploadCoverPhoto(
                            context = context,
                            file = File(localPath),
                            mimeType = mime,
                            onSuccess = { url ->
                                selectedCoverUrl = url
                                isUploadingCustomCover = false
                                uploadStatusMessage = "¡Portada subida exitosamente! 🎉"
                                viewModel.saveProfile(
                                    displayName = displayName,
                                    avatarUrl = selectedAvatarUrl,
                                    firstName = firstName,
                                    lastName = lastName,
                                    status = statusText,
                                    birthDate = birthDate,
                                    sex = sex,
                                    interests = interests,
                                    coverUrl = url,
                                    coverLocalPath = localPath
                                )
                            },
                            onFailure = { error ->
                                isUploadingCustomCover = false
                                uploadStatusMessage = "Error al subir portada: $error 😢"
                            }
                        )
                    }
                } catch (e: Exception) {
                    isUploadingCustomCover = false
                }
            }
        }
    }

    // Load profile on start
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // Populate local variables on Success
    LaunchedEffect(profileState) {
        if (profileState is ProfileUiState.Success) {
            val prof = (profileState as ProfileUiState.Success).profile
            displayName = prof.displayName
            selectedAvatarUrl = prof.avatarUrl ?: ""
            selectedCoverUrl = prof.coverUrl ?: ""
            firstName = prof.firstName ?: ""
            lastName = prof.lastName ?: ""
            statusText = prof.status ?: ""
            birthDate = prof.birthDate ?: ""
            sex = prof.sex ?: ""
            interests = prof.interests ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil y Datos de Identidad", color = Color.White, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (profileState) {
                    is ProfileUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF25D366))
                        }
                    }
                    is ProfileUiState.Error -> {
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (profileState as ProfileUiState.Error).message,
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                    is ProfileUiState.Success, is ProfileUiState.Idle -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Header Editor (Cover photo & Avatar photo)
                            ProfileHeaderEditor(
                                avatarUrl = selectedAvatarUrl,
                                coverUrl = selectedCoverUrl,
                                displayName = displayName,
                                statusText = statusText,
                                onPickAvatar = {
                                    profileGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickCover = {
                                    coverGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                isUploadingAvatar = isUploadingCustomPhoto,
                                isUploadingCover = isUploadingCustomCover
                            )

                            // Avatar Picker Button & Upload Status Feedback
                            AvatarPicker(
                                onPickImage = {
                                    profileGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                isUploading = isUploadingCustomPhoto,
                                statusMessage = uploadStatusMessage
                            )

                            // Display Name Field
                            ProfileField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = "Tu Nombre Público (Apodo)",
                                leadingIcon = Icons.Default.Person,
                                testTag = "profile_display_name_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // First and Last Name Fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    label = "Nombre",
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = "Apellido",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Status Message Field
                            ProfileField(
                                value = statusText,
                                onValueChange = { statusText = it },
                                label = "Tu Frase de Estado 💬",
                                leadingIcon = Icons.Default.Info,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Birth Date & Sex Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileField(
                                    value = birthDate,
                                    onValueChange = { birthDate = it },
                                    label = "F. Nac (AAAA-MM-DD)",
                                    placeholder = "1998-05-15",
                                    modifier = Modifier.weight(1.1f)
                                )

                                Column(modifier = Modifier.weight(0.9f)) {
                                    Text(
                                        text = "Género 👤",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val genders = listOf("M" to "👦", "F" to "👧", "X" to "👤")
                                        genders.forEach { (gCode, gEmoji) ->
                                            val isSelected = sex == gCode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(0xFF25D366) else Color(0xFF101D24))
                                                    .border(1.dp, if (isSelected) Color.Transparent else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                                    .clickable { sex = gCode },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(gEmoji, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Predefined Interests Section
                            Text(
                                text = "Tus Intereses / Gustos 🚀:",
                                color = Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            val predefinedInterests = listOf(
                                "Fútbol ⚽", "Música 🎵", "Programación 💻", "Películas 🎬", "Videojuegos 🎮",
                                "Cocina 🍳", "Viajes ✈️", "Lectura 📚", "Baile 💃", "Arte 🎨"
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    predefinedInterests.take(5).forEach { interest ->
                                        val isSelected = interests.contains(interest)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFF101D24))
                                                .border(1.dp, if (isSelected) Color(0xFF25D366) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    interests = if (isSelected) interests - interest else interests + interest
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(interest, fontSize = 9.sp, color = if (isSelected) Color(0xFF25D366) else Color.White)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    predefinedInterests.drop(5).forEach { interest ->
                                        val isSelected = interests.contains(interest)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFF101D24))
                                                .border(1.dp, if (isSelected) Color(0xFF25D366) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    interests = if (isSelected) interests - interest else interests + interest
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(interest, fontSize = 9.sp, color = if (isSelected) Color(0xFF25D366) else Color.White)
                                        }
                                    }
                                }
                            }

                            // Save Error Message Feedback
                            if (saveState is SaveProfileUiState.Error) {
                                Text(
                                    text = (saveState as SaveProfileUiState.Error).message,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Save Success Message Feedback
                            if (saveState is SaveProfileUiState.Success) {
                                Surface(
                                    color = Color(0xFF25D366).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "¡Perfil actualizado con éxito! 🎉",
                                        color = Color(0xFF25D366),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            // Save Button
                            ProfileSaveButton(
                                onSave = {
                                    viewModel.saveProfile(
                                        displayName = displayName,
                                        avatarUrl = selectedAvatarUrl,
                                        firstName = firstName,
                                        lastName = lastName,
                                        status = statusText,
                                        birthDate = birthDate,
                                        sex = sex,
                                        interests = interests,
                                        coverUrl = selectedCoverUrl
                                    )
                                },
                                isLoading = saveState is SaveProfileUiState.Loading
                            )
                        }
                    }
                }
            }
        }
    }
}
