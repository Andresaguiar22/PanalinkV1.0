package com.example.ui.screen.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import com.example.ui.viewmodel.onboarding.OnboardingUiState
import com.example.ui.viewmodel.onboarding.OnboardingViewModel
import com.example.ui.components.AuroraBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    val displayName by viewModel.displayName.collectAsState()
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val statusText by viewModel.status.collectAsState()
    val birthDate by viewModel.birthDate.collectAsState()
    val sex by viewModel.sex.collectAsState()
    val interests by viewModel.interests.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val coverUrl by viewModel.coverUrl.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()
    val isUploadingCover by viewModel.isUploadingCover.collectAsState()
    val avatarError by viewModel.avatarUploadError.collectAsState()
    val coverError by viewModel.coverUploadError.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val scrollState = rememberScrollState()

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadCover(context, it) }
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Configura tu Pana Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Paso $currentStep de $totalSteps",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentStep > 1) {
                            IconButton(onClick = { currentStep-- }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { currentStep.toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                viewModel.finalizeOnboarding(onNext)
                            }
                        },
                        enabled = when (currentStep) {
                            1 -> displayName.isNotBlank() && !isUploadingAvatar && !isUploadingCover
                            else -> true
                        } && uiState !is OnboardingUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("onboarding_next_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        if (uiState is OnboardingUiState.Loading && currentStep == totalSteps) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (currentStep == totalSteps) "¡Listo, Vamos! 🚀" else "Siguiente Paso ✨",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (uiState is OnboardingUiState.Error && currentStep == totalSteps) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as OnboardingUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "step_transition"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (step) {
                            1 -> IdentityStep(
                                displayName = displayName,
                                avatarUrl = avatarUrl,
                                coverUrl = coverUrl,
                                isUploadingAvatar = isUploadingAvatar,
                                isUploadingCover = isUploadingCover,
                                avatarError = avatarError,
                                coverError = coverError,
                                onNameChange = viewModel::setDisplayName,
                                onAvatarClick = { avatarLauncher.launch("image/*") },
                                onCoverClick = { coverLauncher.launch("image/*") }
                            )
                            2 -> DetailsStep(
                                firstName = firstName,
                                lastName = lastName,
                                birthDate = birthDate,
                                sex = sex,
                                onFirstNameChange = viewModel::setFirstName,
                                onLastNameChange = viewModel::setLastName,
                                onBirthDateChange = viewModel::setBirthDate,
                                onSexChange = viewModel::setSex
                            )
                            3 -> VibeStep(
                                statusText = statusText,
                                interests = interests,
                                onStatusChange = viewModel::setStatus,
                                onInterestsChange = viewModel::setInterests
                            )
                            4 -> PreviewStep(
                                displayName = displayName,
                                firstName = firstName,
                                lastName = lastName,
                                statusText = statusText,
                                avatarUrl = avatarUrl,
                                coverUrl = coverUrl,
                                sex = sex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityStep(
    displayName: String,
    avatarUrl: String?,
    coverUrl: String?,
    isUploadingAvatar: Boolean,
    isUploadingCover: Boolean,
    avatarError: String?,
    coverError: String?,
    onNameChange: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Tu Identidad Visual 💎",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            "Sube una foto de portada y de perfil para destacar.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Profile Card with Cover and Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .clickable { onCoverClick() }
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, "Add Cover", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                            Text("Añadir Portada", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
                
                if (isUploadingCover) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
            }

            // Avatar Image
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(4.dp, Color(0xFF00E5FF), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    if (avatarUrl.startsWith("preset:")) {
                        Text(avatarUrl.removePrefix("preset:"), fontSize = 42.sp)
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                }
                
                if (isUploadingAvatar) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    }
                }
                
                // Add icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (avatarError != null || coverError != null) {
            Text(
                text = avatarError ?: coverError ?: "",
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text("¿Cómo te llamamos?") },
            placeholder = { Text("Ej. El Pana Real") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsStep(
    firstName: String,
    lastName: String,
    birthDate: String,
    sex: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onSexChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        onBirthDateChange(formatter.format(java.util.Date(millis)))
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Algo de ti 📝",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            "Tu nombre real y fecha nos ayudan a personalizar tu experiencia.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Apellido") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha de Nacimiento") },
            placeholder = { Text("Selecciona tu fecha") },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors(),
            leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color.White.copy(alpha = 0.5f)) },
            enabled = false
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Género / Identidad",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        var expanded by remember { mutableStateOf(false) }
        val options = listOf("Masculino", "Femenino", "Otro")
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = sex.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors(),
                label = { Text("Selecciona tu género") }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSexChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibeStep(
    statusText: String,
    interests: List<String>,
    onStatusChange: (String) -> Unit,
    onInterestsChange: (List<String>) -> Unit
) {
    val allInterests = listOf(
        "Fútbol ⚽", "Música 🎵", "Tech 💻", "Cine 🎬", "Gamer 🎮",
        "Cocina 🍳", "Viajes ✈️", "Libros 📚", "Baile 💃", "Arte 🎨",
        "Fitness 🏋️", "Naturaleza 🌿", "Fotografía 📸", "Moda 👗", "Mascotas 🐶"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Tu Vibe ✨",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            "¿Qué estás haciendo? ¿Qué te gusta? Cuéntale al mundo.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = statusText,
            onValueChange = onStatusChange,
            label = { Text("Tu frase de hoy") },
            placeholder = { Text("¡Activo en Panalink! ⚡") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Intereses (Elige tus favoritos)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allInterests.forEach { interest ->
                val selected = interests.contains(interest)
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) onInterestsChange(interests - interest)
                        else onInterestsChange(interests + interest)
                    },
                    label = { Text(interest, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.White.copy(alpha = 0.1f),
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = selected
                    )
                )
            }
        }
    }
}

@Composable
fun PreviewStep(
    displayName: String,
    firstName: String,
    lastName: String,
    statusText: String,
    avatarUrl: String?,
    coverUrl: String?,
    sex: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "¡Casi listo! 🏁",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            "Así es como te verán los demás panas.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Modern Profile Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color.Transparent))))
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 20.dp, y = 40.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(3.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl != null) {
                            if (avatarUrl.startsWith("preset:")) {
                                Text(avatarUrl.removePrefix("preset:"), fontSize = 32.sp)
                            } else {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "$firstName $lastName",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            statusText,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = Color(0xFF00E5FF),
    cursorColor = Color(0xFF00E5FF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
)
