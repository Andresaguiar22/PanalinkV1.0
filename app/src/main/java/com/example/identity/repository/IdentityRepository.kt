package com.example.identity.repository

import android.content.Context
import com.example.data.database.PanalinkDatabase
import com.example.data.database.ProfileEntity
import com.example.data.model.Profile
import com.example.identity.analytics.IdentityAnalytics
import com.example.identity.memory.IdentityMemoryCache
import com.example.identity.model.CachedProfile
import com.example.identity.model.ProfileUpdateResult
import com.example.identity.model.IdentityUiState
import com.example.identity.model.toIdentityUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentHashMap

class IdentityRepository(private val context: Context) {
    
    private val profileDao = PanalinkDatabase.getDatabase(context).profileDao()
    
    private val profileFlowCache = ConcurrentHashMap<String, Flow<CachedProfile?>>()
    private val identityFlowCache = ConcurrentHashMap<String, Flow<IdentityUiState?>>()

    fun observeProfile(userId: String): Flow<CachedProfile?> {
        return profileFlowCache.getOrPut(userId) {
            profileDao.observeProfile(userId).map { entity ->
                entity?.let {
                    val profile = it.toProfile()
                    val cached = CachedProfile(
                        profile = profile,
                        avatarLocalPath = it.avatarLocalPath,
                        coverLocalPath = it.coverLocalPath,
                        isDirty = it.isDirty,
                        syncVersion = it.syncVersion,
                        lastSyncedAt = it.lastSyncedAt
                    )
                    IdentityMemoryCache.profiles.put(userId, cached)
                    cached
                }
            }.distinctUntilChanged()
        }
    }
    
    fun observeIdentity(userId: String): Flow<IdentityUiState?> {
        return identityFlowCache.getOrPut(userId) {
            observeProfile(userId).map { it?.toIdentityUiState() }.distinctUntilChanged()
        }
    }

    fun observeProfiles(): Flow<List<CachedProfile>> {
        return profileDao.observeAllProfiles().map { entities ->
            entities.map { entity ->
                val profile = entity.toProfile()
                val cached = CachedProfile(
                    profile = profile,
                    avatarLocalPath = entity.avatarLocalPath,
                    coverLocalPath = entity.coverLocalPath,
                    isDirty = entity.isDirty,
                    syncVersion = entity.syncVersion,
                    lastSyncedAt = entity.lastSyncedAt
                )
                IdentityMemoryCache.profiles.put(entity.id, cached)
                cached
            }
        }
    }

    suspend fun getProfile(userId: String): CachedProfile? = withContext(Dispatchers.IO) {
        val memoryHit = IdentityMemoryCache.profiles.get(userId)
        if (memoryHit != null) {
            IdentityAnalytics.trackRoomHit() // Memory cache implies Room hit originally
            return@withContext memoryHit
        }

        val entity = profileDao.getProfile(userId)
        if (entity != null) {
            IdentityAnalytics.trackRoomHit()
            val profile = entity.toProfile()
            val cached = CachedProfile(
                profile = profile,
                avatarLocalPath = entity.avatarLocalPath,
                coverLocalPath = entity.coverLocalPath,
                isDirty = entity.isDirty,
                syncVersion = entity.syncVersion,
                lastSyncedAt = entity.lastSyncedAt
            )
            IdentityMemoryCache.profiles.put(userId, cached)
            return@withContext cached
        }
        
        null
    }

    suspend fun saveProfile(cachedProfile: CachedProfile): ProfileUpdateResult = withContext(Dispatchers.IO) {
        try {
            val entity = ProfileEntity.fromProfile(cachedProfile.profile).copy(
                avatarLocalPath = cachedProfile.avatarLocalPath,
                coverLocalPath = cachedProfile.coverLocalPath,
                isDirty = cachedProfile.isDirty,
                syncVersion = cachedProfile.syncVersion,
                lastSyncedAt = cachedProfile.lastSyncedAt
            )
            profileDao.insertOrUpdate(entity)
            IdentityMemoryCache.profiles.put(cachedProfile.profile.id, cachedProfile)
            ProfileUpdateResult.Success
        } catch (e: Exception) {
            ProfileUpdateResult.Error(e)
        }
    }
    
    suspend fun updateProfile(cachedProfile: CachedProfile): ProfileUpdateResult = saveProfile(cachedProfile)
    
    suspend fun syncProfile(userId: String) {
        // Handled by IdentitySyncManager, this is a placeholder if needed for direct triggering
    }
    
    suspend fun syncProfiles() {
        // Handled by IdentitySyncManager
    }
}
