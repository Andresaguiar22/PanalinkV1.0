package com.example.identity.repository

import android.content.Context
import androidx.annotation.Keep
import com.example.data.database.PanalinkDatabase
import com.example.data.repository.PublicProfileRepository
import com.example.identity.memory.IdentityMemoryCache
import com.example.identity.model.IdentityUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Keep
class IdentityRepository(context: Context) {
    private val db = PanalinkDatabase.getDatabase(context)
    private val profileDao = db.profileDao()
    private val publicProfileRepository = PublicProfileRepository(db.publicProfileDao())

    fun observeIdentity(userId: String): Flow<IdentityUiState?> {
        // First try to observe from local_profiles (Private profiles / friends)
        return profileDao.observeProfile(userId).map { entity ->
            if (entity != null) {
                val state = IdentityUiState(
                    userId = entity.id,
                    displayName = entity.displayName,
                    avatarUrl = entity.avatarUrl,
                    avatarLocalPath = entity.avatarLocalPath
                )
                IdentityMemoryCache.profiles[userId] = state
                state
            } else {
                // If not in local_profiles, try public_profiles via PublicProfileRepository
                // Note: PublicProfileRepository returns a StateFlow of List<PublicProfile> 
                // but we can just use the memory cache of the repository if it's there.
                // For simplicity in this bridge, we'll return null if not found locally, 
                // but we should probably trigger a fetch in the background.
                IdentityMemoryCache.profiles[userId]
            }
        }
    }

    suspend fun getProfile(userId: String): IdentityUiState? {
        val entity = profileDao.getProfileById(userId)
        return if (entity != null) {
            IdentityUiState(
                userId = entity.id,
                displayName = entity.displayName,
                avatarUrl = entity.avatarUrl,
                avatarLocalPath = entity.avatarLocalPath
            )
        } else {
            IdentityMemoryCache.profiles[userId]
        }
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
