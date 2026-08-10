package com.example.domain.profile

import android.content.Context
import com.example.data.model.Profile
import com.example.data.repository.ProfilesRepository
import com.example.core.error.ResultState
import com.example.core.error.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateProfileUseCase(private val context: Context) {
    private val profilesRepository by lazy { ProfilesRepository() }

    suspend operator fun invoke(
        userId: String,
        displayName: String,
        avatarUrl: String? = null
    ): ResultState<Profile> = withContext(Dispatchers.IO) {
        try {
            val result = profilesRepository.updateProfile(userId, displayName, avatarUrl)
            if (result.isSuccess) {
                ResultState.Success(result.getOrThrow())
            } else {
                ResultState.Error(result.exceptionOrNull() ?: Exception("Error al actualizar perfil"))
            }
        } catch (e: Exception) {
            ResultState.Error(ErrorMapper.map(e))
        }
    }
}
