package com.example.data.database

import androidx.room.*

@Dao
interface ProfileDao {
    @Query("SELECT * FROM local_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM local_profiles WHERE id = :id")
    suspend fun getProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM local_profiles WHERE id = :id")
    fun observeProfile(id: String): kotlinx.coroutines.flow.Flow<ProfileEntity?>

    @Query("SELECT * FROM local_profiles")
    fun observeAllProfiles(): kotlinx.coroutines.flow.Flow<List<ProfileEntity>>
    
    @Query("SELECT * FROM local_profiles")
    suspend fun getAllProfilesSync(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: ProfileEntity)

    @Query("SELECT * FROM local_profiles WHERE id IN (:ids)")
    fun observeProfiles(ids: List<String>): kotlinx.coroutines.flow.Flow<List<ProfileEntity>>

    @Query("SELECT * FROM local_profiles WHERE id IN (:ids)")
    suspend fun getProfilesByIds(ids: List<String>): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)
}
