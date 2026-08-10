package com.example.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Room DAO for managing `public_profiles` table operations.
 */
@Dao
interface PublicProfileDao {
    @Query("SELECT * FROM public_profiles WHERE id = :id")
    suspend fun getById(id: String): PublicProfileEntity?

    @Query("SELECT * FROM public_profiles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PublicProfileEntity>

    @Upsert
    suspend fun upsert(entity: PublicProfileEntity)

    @Upsert
    suspend fun upsertAll(entities: List<PublicProfileEntity>)

    @Query("DELETE FROM public_profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM public_profiles")
    suspend fun deleteAll()
}
