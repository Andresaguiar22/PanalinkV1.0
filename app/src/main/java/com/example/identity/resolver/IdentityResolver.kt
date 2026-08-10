package com.example.identity.resolver

import com.example.identity.model.IdentityUiState
import com.example.identity.model.toIdentityUiState
import com.example.identity.repository.IdentityRepository
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IdentityResolver(
    private val identityRepository: IdentityRepository,
    private val database: PanalinkDatabase
) {
    suspend fun resolve(userId: String): IdentityUiState? {
        val cached = identityRepository.getProfile(userId)
        return cached?.toIdentityUiState()
    }

    fun observeIdentities(userIds: List<String>): Flow<Map<String, IdentityUiState>> {
        return database.profileDao().observeProfiles(userIds).map { entities ->
            val map = mutableMapOf<String, IdentityUiState>()
            entities.forEach { entity ->
                val cached = com.example.identity.model.CachedProfile(
                    profile = entity.toProfile(),
                    avatarLocalPath = entity.avatarLocalPath,
                    coverLocalPath = entity.coverLocalPath,
                    isDirty = entity.isDirty,
                    syncVersion = entity.syncVersion,
                    lastSyncedAt = entity.lastSyncedAt
                )
                com.example.identity.memory.IdentityMemoryCache.profiles.put(entity.id, cached)
                map[entity.id] = cached.toIdentityUiState()
            }
            map
        }
    }
}
