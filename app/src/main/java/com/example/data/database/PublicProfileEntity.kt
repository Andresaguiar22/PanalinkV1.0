package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for caching public profiles locally in SQLite.
 * Independent entity table `public_profiles`.
 */
@Entity(tableName = "public_profiles")
data class PublicProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val updatedAt: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
