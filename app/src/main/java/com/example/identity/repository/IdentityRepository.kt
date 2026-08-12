package com.example.identity.repository

import android.content.Context
import androidx.annotation.Keep
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.PublicProfileFetchResult
import com.example.data.repository.PublicProfileRepository
import com.example.identity.memory.IdentityMemoryCache
import com.example.identity.model.IdentityUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

@Keep
class IdentityRepository(context: Context) {
    private val db = PanalinkDatabase.getDatabase(context)
    private val profileDao = db.profileDao()
    private val publicProfileRepository = PublicProfileRepository.getInstance(context)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeIdentity(userId: String): Flow<IdentityUiState?> {
        return profileDao.observeProfile(userId).transformLatest { localEntity ->
            if (localEntity != null && !localEntity.avatarUrl.isNullOrEmpty()) {
                val state = IdentityUiState(
                    userId = localEntity.id,
                    displayName = localEntity.displayName,
                    avatarUrl = localEntity.avatarUrl,
                    avatarLocalPath = localEntity.avatarLocalPath
                )
                IdentityMemoryCache.profiles[userId] = state
                emit(state)
            } else {
                // If local profile has no avatar, or is missing entirely, emit partial cache first
                if (localEntity != null) {
                    val state = IdentityUiState(
                        userId = localEntity.id,
                        displayName = localEntity.displayName,
                        avatarUrl = null,
                        avatarLocalPath = localEntity.avatarLocalPath
                    )
                    IdentityMemoryCache.profiles[userId] = state
                    emit(state)
                } else {
                    val cached = IdentityMemoryCache.profiles[userId]
                    if (cached != null) {
                        emit(cached)
                    }
                }

                // Query PublicProfileRepository (it manages single-flight & Room caching internally)
                val result = publicProfileRepository.getPublicProfile(userId)
                if (result is PublicProfileFetchResult.Success) {
                    val pub = result.data
                    val displayName = pub.displayName ?: pub.firstName ?: localEntity?.displayName ?: "Usuario"
                    val state = IdentityUiState(
                        userId = pub.id,
                        displayName = displayName,
                        avatarUrl = pub.avatarUrl,
                        avatarLocalPath = localEntity?.avatarLocalPath
                    )
                    IdentityMemoryCache.profiles[userId] = state
                    emit(state)
                } else if (localEntity == null) {
                    // If no local entity and public profile also not found, show default initials
                    val state = IdentityUiState(
                        userId = userId,
                        displayName = "Usuario",
                        avatarUrl = null,
                        avatarLocalPath = null
                    )
                    IdentityMemoryCache.profiles[userId] = state
                    emit(state)
                }
            }
        }
    }

    suspend fun getProfile(userId: String): IdentityUiState? {
        val localEntity = profileDao.getProfileById(userId)
        if (localEntity != null && !localEntity.avatarUrl.isNullOrEmpty()) {
            return IdentityUiState(
                userId = localEntity.id,
                displayName = localEntity.displayName,
                avatarUrl = localEntity.avatarUrl,
                avatarLocalPath = localEntity.avatarLocalPath
            )
        }
        
        val result = publicProfileRepository.getPublicProfile(userId)
        if (result is PublicProfileFetchResult.Success) {
            val pub = result.data
            return IdentityUiState(
                userId = pub.id,
                displayName = pub.displayName ?: pub.firstName ?: localEntity?.displayName ?: "Usuario",
                avatarUrl = pub.avatarUrl,
                avatarLocalPath = localEntity?.avatarLocalPath
            )
        }
        
        return localEntity?.let {
            IdentityUiState(
                userId = it.id,
                displayName = it.displayName,
                avatarUrl = null,
                avatarLocalPath = it.avatarLocalPath
            )
        } ?: IdentityMemoryCache.profiles[userId] ?: IdentityUiState(userId, "Usuario", null, null)
    }

    suspend fun saveProfile(state: IdentityUiState) {
        // Bridge save back to Room if possible
        val existing = profileDao.getProfileById(state.userId)
        if (existing != null) {
            profileDao.insertProfile(existing.copy(
                displayName = state.displayName ?: existing.displayName,
                avatarUrl = state.avatarUrl ?: existing.avatarUrl,
                avatarLocalPath = state.avatarLocalPath ?: existing.avatarLocalPath
            ))
        }
        IdentityMemoryCache.profiles[state.userId] = state
    }
}
