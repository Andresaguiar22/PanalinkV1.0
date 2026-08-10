package com.example.ui.session

import android.content.Context
import com.example.data.model.Profile
import com.example.data.repository.ProfilesRepository
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SessionRepository(private val context: Context) {

    private val profilesRepo = ProfilesRepository()

    val currentProfileState: StateFlow<Profile?> = SupabaseClient.currentProfileState

    val isOffline: StateFlow<Boolean> = SessionManager.isOffline

    val sessionUiState: Flow<SessionUiState> = SupabaseClient.currentProfileState.map { profile ->
        val user = SupabaseClient.currentUser
        SessionUiState(
            isAuthenticated = user != null,
            userId = user?.id ?: "",
            email = user?.email ?: "",
            displayName = profile?.displayName ?: user?.userMetadata?.get("displayName")?.toString() ?: "Pana",
            avatarUrl = profile?.avatarUrl ?: user?.userMetadata?.get("avatar_url")?.toString(),
            isProfileComplete = profile?.isProfileComplete ?: false,
            profile = profile,
            isOffline = SessionManager.isOffline.value,
            isLoading = false
        )
    }

    fun getCurrentUserId(): String {
        return SupabaseClient.currentUser?.id ?: ""
    }

    fun getCurrentEmail(): String {
        return SupabaseClient.currentUser?.email ?: ""
    }

    fun getCurrentProfile(): Profile? {
        return SupabaseClient.currentProfile ?: SessionManager.getCachedProfile()
    }

    suspend fun refreshProfile(): Result<Profile> = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId()
        if (uid.isEmpty()) return@withContext Result.failure(IllegalStateException("No authenticated user"))
        val result = profilesRepo.getProfile(uid)
        result.onSuccess { fresh ->
            SupabaseClient.currentProfile = fresh
            SessionManager.saveSession(
                SupabaseClient.currentToken,
                SupabaseClient.currentRefreshToken,
                SupabaseClient.currentUser,
                fresh
            )
        }
        result
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        SessionManager.clearSession()
    }
}
