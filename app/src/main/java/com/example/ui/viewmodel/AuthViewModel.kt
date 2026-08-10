package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthUser
import com.example.data.model.Profile
import com.example.data.repository.ProfilesRepository
import com.example.data.supabase.AuthManager
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object LoggedOut : AuthUiState()

    data class NeedsEmailVerification(val email: String, val userId: String = "") : AuthUiState()
    data class NeedsVerification(val email: String) : AuthUiState()
    data class NeedsProfileSetup(val user: AuthUser, val profile: Profile) : AuthUiState()
    data class Authenticated(val user: AuthUser, val profile: Profile) : AuthUiState()

    data class AuthenticatedReady(val user: AuthUser, val profile: Profile) : AuthUiState()
    data class AuthenticatedIncomplete(val user: AuthUser, val profile: Profile?) : AuthUiState()
    data class Success(val user: AuthUser) : AuthUiState()

    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val authManager: AuthManager = AuthManager()) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    val isConfigured: Boolean = SupabaseClient.isConfigured

    private suspend fun determineAuthenticatedState(user: AuthUser): AuthUiState {
        var prof = SupabaseClient.currentProfile
        if (prof == null) {
            val profilesRepo = ProfilesRepository()
            prof = profilesRepo.getProfile(user.id).getOrNull()
            
            if (prof == null) {
                // Try to recover from metadata if DB entry is missing
                val fallbackName = user.userMetadata?.get("display_name")?.toString() 
                    ?: user.email?.substringBefore("@") 
                    ?: "Pana"
                prof = Profile(id = user.id, displayName = fallbackName, avatarUrl = null, isProfileComplete = false)
            }
            SupabaseClient.currentProfile = prof
        }

        return if (prof.isProfileComplete) {
            AuthUiState.Authenticated(user, prof)
        } else {
            AuthUiState.NeedsProfileSetup(user, prof)
        }
    }

    private var isCheckingVerification = false
    private var isProcessingDeepLink = false

    fun onOnboardingComplete() {
        val currentUser = SupabaseClient.currentUser
        val currentProfile = SupabaseClient.currentProfile
        if (currentUser != null && currentProfile != null) {
            _uiState.value = AuthUiState.Authenticated(currentUser, currentProfile)
        }
    }

    init {
        // Auto-login check if already active session exists
        viewModelScope.launch {
            val user = SupabaseClient.currentUser
            if (user != null) {
                if (user.emailConfirmedAt != null || SupabaseClient.currentToken != null) {
                    _uiState.value = determineAuthenticatedState(user)
                } else {
                    _uiState.value = AuthUiState.NeedsEmailVerification(user.email ?: "")
                }
            } else {
                _uiState.value = AuthUiState.LoggedOut
            }
        }
    }

    fun register(displayName: String, email: String, pword: String) {
        if (displayName.isBlank() || email.isBlank() || pword.isBlank()) {
            _uiState.value = AuthUiState.Error("Por favor, rellena todos los campos.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authManager.signUp(displayName, email, pword)
                .onSuccess { user ->
                    if (user.emailConfirmedAt != null || SupabaseClient.currentToken != null) {
                        _uiState.value = determineAuthenticatedState(user)
                    } else {
                        _uiState.value = AuthUiState.NeedsEmailVerification(email)
                    }
                }
                .onFailure { error ->
                    val rawMessage = error.message ?: ""
                    val friendlyMessage = when {
                        rawMessage.contains("over_email_send_rate_limit", ignoreCase = true) || 
                        rawMessage.contains("rate limit", ignoreCase = true) || 
                        rawMessage.contains("429") ->
                            "¡Cálmate un poquito, pana! 😂 Supabase dice que se han enviado muchos correos. Espera un par de minutos e intenta de nuevo."

                        rawMessage.contains("User already registered", ignoreCase = true) || 
                        rawMessage.contains("already exists", ignoreCase = true) ||
                        rawMessage.contains("already registered", ignoreCase = true) ->
                            "Este correo ya tiene cuenta. ¡Dale a 'Inicia sesión' mejor!"

                        rawMessage.contains("Error sending confirmation email", ignoreCase = true) ||
                        rawMessage.contains("confirmation email", ignoreCase = true) ||
                        rawMessage.contains("SMTP", ignoreCase = true) ->
                            "Error al enviar el correo de confirmación por SMTP. Verifica tu dirección de correo o contacta al administrador."

                        rawMessage.contains("Password should be", ignoreCase = true) ->
                            "Esa contraseña está muy flaca. Ponle al menos 6 caracteres."

                        rawMessage.contains("invalid email", ignoreCase = true) ||
                        rawMessage.contains("validate email", ignoreCase = true) ->
                            "Ese correo no parece válido. Chequéalo bien."

                        rawMessage.contains("Database error", ignoreCase = true) ->
                            "Error en la base de datos al registrar. Intenta de nuevo más tarde."

                        rawMessage.isNotBlank() ->
                            "Error al registrarte: $rawMessage"

                        else -> "Hubo un lío al registrarte. Intenta de nuevo en un rato."
                    }
                    _uiState.value = AuthUiState.Error(friendlyMessage)
                }
        }
    }

    fun login(email: String, pword: String) {
        if (email.isBlank() || pword.isBlank()) {
            _uiState.value = AuthUiState.Error("Email y contraseña requeridos.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authManager.signIn(email, pword)
                .onSuccess { user ->
                    if (user.emailConfirmedAt != null || SupabaseClient.currentToken != null) {
                        _uiState.value = determineAuthenticatedState(user)
                    } else {
                        _uiState.value = AuthUiState.NeedsEmailVerification(email)
                    }
                }
                .onFailure { error ->
                    val rawMessage = error.message ?: ""
                    val friendlyMessage = when {
                        rawMessage.contains("Debes confirmar tu correo electrónico", ignoreCase = true) ||
                        rawMessage.contains("Email not confirmed", ignoreCase = true) ||
                        rawMessage.contains("email_not_confirmed", ignoreCase = true) -> 
                            "Debes confirmar tu correo electrónico antes de entrar a Panalink."
                        rawMessage.contains("Invalid login credentials", ignoreCase = true) -> 
                            "Email o contraseña incorrectos. Revisa tus datos, pana."
                        rawMessage.contains("Rate limit exceeded", ignoreCase = true) -> 
                            "Demasiados intentos fallidos. Por seguridad, espera un ratico e intenta de nuevo."
                        rawMessage.contains("User not found", ignoreCase = true) -> 
                            "No encontramos ninguna cuenta con ese email."
                        else -> "Error de inicio de sesión. Inténtalo de nuevo. (${rawMessage.take(120)})"
                    }
                    _uiState.value = AuthUiState.Error(friendlyMessage)
                }
        }
    }

    fun checkEmailVerificationStatus(silent: Boolean = false) {
        val current = _uiState.value
        if (current is AuthUiState.Authenticated ||
            current is AuthUiState.NeedsProfileSetup ||
            current is AuthUiState.AuthenticatedReady ||
            current is AuthUiState.AuthenticatedIncomplete) {
            Log.d("AuthViewModel", "Already authenticated in state: $current. Ignoring check.")
            return
        }

        if (isProcessingDeepLink) {
            Log.d("AuthViewModel", "Deep link is currently being processed. Ignoring email verification status check.")
            return
        }

        val user = SupabaseClient.currentUser
        if (user != null && (user.emailConfirmedAt != null || SupabaseClient.currentToken != null)) {
            viewModelScope.launch {
                _uiState.value = determineAuthenticatedState(user)
            }
            return
        }

        if (isCheckingVerification) {
            Log.d("AuthViewModel", "Verification check already in progress. Ignoring parallel call.")
            return
        }
        isCheckingVerification = true

        val email = user?.email ?: authManager.pendingEmail ?: ""
        if (!silent) {
            _uiState.value = AuthUiState.Loading
        }
        viewModelScope.launch {
            try {
                authManager.checkEmailVerification()
                    .onSuccess { isVerified ->
                        if (isVerified) {
                            val refreshedUser = SupabaseClient.currentUser
                            if (refreshedUser != null) {
                                _uiState.value = determineAuthenticatedState(refreshedUser)
                            } else {
                                if (!silent) _uiState.value = AuthUiState.Error("Error de sesión, intenta de nuevo.")
                            }
                        } else {
                            if (!silent) {
                                _uiState.value = AuthUiState.Error("El email aún no ha sido verificado. Por favor, revisa tu bandeja de entrada.")
                                delay(2000)
                            }
                            if (_uiState.value is AuthUiState.Authenticated ||
                                _uiState.value is AuthUiState.NeedsProfileSetup ||
                                _uiState.value is AuthUiState.AuthenticatedReady ||
                                _uiState.value is AuthUiState.AuthenticatedIncomplete) {
                                return@onSuccess
                            }
                            _uiState.value = AuthUiState.NeedsEmailVerification(email)
                        }
                    }
                    .onFailure { error ->
                        if (!silent) {
                            _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Error al verificar")
                            delay(2000)
                        }
                        if (_uiState.value is AuthUiState.Authenticated ||
                            _uiState.value is AuthUiState.NeedsProfileSetup ||
                            _uiState.value is AuthUiState.AuthenticatedReady ||
                            _uiState.value is AuthUiState.AuthenticatedIncomplete) {
                            return@onFailure
                        }
                        _uiState.value = AuthUiState.NeedsEmailVerification(email)
                    }
            } finally {
                isCheckingVerification = false
            }
        }
    }

    fun checkVerification() {
        checkEmailVerificationStatus(silent = false)
    }

    // Bypass verification in Demo Mode to let developers try the dashboard instantly
    fun forceVerifyDemo() {
        authManager.verifyDemoUser()
        val user = SupabaseClient.currentUser
        if (user != null) {
            viewModelScope.launch {
                _uiState.value = determineAuthenticatedState(user)
            }
        }
    }

    fun resendVerificationEmail() {
        val email = SupabaseClient.currentUser?.email ?: return
        viewModelScope.launch {
            authManager.resendVerification(email)
                .onSuccess {
                    // Temporarily signal success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Error al reenviar email")
                    delay(2000)
                    _uiState.value = AuthUiState.NeedsEmailVerification(email)
                }
        }
    }

    fun logout() {
        authManager.signOut()
        _uiState.value = AuthUiState.LoggedOut
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AuthViewModel", "FCM Token deleted successfully on logout")
                    } else {
                        Log.e("AuthViewModel", "Failed to delete FCM token on logout", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error deleting FCM token on logout", e)
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is AuthUiState.Error) {
            val email = SupabaseClient.currentUser?.email
            if (email != null) {
                _uiState.value = AuthUiState.NeedsEmailVerification(email)
            } else {
                _uiState.value = AuthUiState.LoggedOut
            }
        }
    }

    fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        val uriString = data.toString()
        Log.i("AuthViewModel", "Received deep link intent: $data")

        val accessToken = extractQueryOrFragmentParam(uriString, "access_token")
        val refreshToken = extractQueryOrFragmentParam(uriString, "refresh_token")
        val code = extractQueryOrFragmentParam(uriString, "code")
        val tokenHash = extractQueryOrFragmentParam(uriString, "token_hash")

        Log.d("AuthViewModel", "DEEPLINK_RECEIVED")
        Log.d("AuthViewModel", "DEEPLINK_URI=$uriString")
        Log.d("AuthViewModel", "DEEPLINK_CODE=$code")
        Log.d("AuthViewModel", "DEEPLINK_ACCESS_TOKEN_PRESENT=${!accessToken.isNullOrEmpty()}")

        if (data.scheme == "panalink" && (data.host == "verify" || uriString.contains("access_token") || !code.isNullOrEmpty() || !tokenHash.isNullOrEmpty())) {
            isProcessingDeepLink = true
            viewModelScope.launch {
                try {
                    if (!accessToken.isNullOrEmpty()) {
                        authManager.setTokensAndFetchProfile(accessToken, refreshToken)
                            .onSuccess { user ->
                                _uiState.value = determineAuthenticatedState(user)
                            }
                            .onFailure {
                                checkEmailVerificationStatus(silent = false)
                            }
                    } else if (!tokenHash.isNullOrEmpty()) {
                        val type = extractQueryOrFragmentParam(uriString, "type") ?: "signup"
                        authManager.verifyOtpByHash(tokenHash, type)
                            .onSuccess { user ->
                                _uiState.value = determineAuthenticatedState(user)
                            }
                            .onFailure {
                                checkEmailVerificationStatus(silent = false)
                            }
                    } else {
                        SessionManager.validateAndRefreshSessionIfNeeded()
                        val user = SupabaseClient.currentUser
                        if (user != null) {
                            _uiState.value = determineAuthenticatedState(user)
                        } else {
                            checkEmailVerificationStatus(silent = false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error processing deep link", e)
                } finally {
                    isProcessingDeepLink = false
                }
            }
        }
    }

    private fun extractQueryOrFragmentParam(url: String, key: String): String? {
        val pattern = Regex("[?&#]$key=([^&#]*)")
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
    }
}
