package com.example.data.mapper

import com.example.data.database.PublicProfileEntity
import com.example.data.model.PublicProfile
import com.example.data.model.PublicProfileDto
import com.example.data.repository.CdnManager

/**
 * Mapper for converting between PublicProfile DTO, Domain Model, and Room Entity.
 * Centralizes avatar URL normalization via [CdnManager.resolveAvatarUrl].
 */
object PublicProfileMapper {

    fun dtoToModel(dto: PublicProfileDto): PublicProfile {
        return PublicProfile(
            id = dto.id,
            displayName = dto.displayName,
            firstName = dto.firstName,
            lastName = dto.lastName,
            avatarUrl = CdnManager.resolveAvatarUrl(dto.avatarUrl),
            updatedAt = dto.updatedAt
        )
    }

    fun modelToEntity(model: PublicProfile, lastSyncedAt: Long = System.currentTimeMillis()): PublicProfileEntity {
        return PublicProfileEntity(
            id = model.id,
            displayName = model.displayName,
            firstName = model.firstName,
            lastName = model.lastName,
            avatarUrl = CdnManager.resolveAvatarUrl(model.avatarUrl),
            updatedAt = model.updatedAt,
            lastSyncedAt = lastSyncedAt
        )
    }

    fun entityToModel(entity: PublicProfileEntity): PublicProfile {
        return PublicProfile(
            id = entity.id,
            displayName = entity.displayName,
            firstName = entity.firstName,
            lastName = entity.lastName,
            avatarUrl = CdnManager.resolveAvatarUrl(entity.avatarUrl),
            updatedAt = entity.updatedAt
        )
    }

    fun dtoToEntity(dto: PublicProfileDto, lastSyncedAt: Long = System.currentTimeMillis()): PublicProfileEntity {
        return modelToEntity(dtoToModel(dto), lastSyncedAt)
    }
}
