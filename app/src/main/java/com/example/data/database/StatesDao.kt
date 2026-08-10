package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatesDao {
    @Query("SELECT * FROM user_states ORDER BY createdAt DESC")
    suspend fun getAllStatesSync(): List<StateEntity>

    @Query("SELECT * FROM user_states WHERE isReel = :isReel ORDER BY createdAt DESC")
    fun getStatesFlow(isReel: Boolean): Flow<List<StateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStates(states: List<StateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: StateEntity)

    @Query("DELETE FROM user_states WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM user_states WHERE id LIKE 'optimistic_%' AND userId = :userId AND (caption = :caption OR (caption IS NULL AND :caption IS NULL))")
    suspend fun deleteOptimistic(userId: String, caption: String?)

    @Query("DELETE FROM user_states WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpired(now: String)

    @Query("UPDATE user_states SET localVideoPath = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String?)

    @Query("SELECT * FROM user_states WHERE id = :id")
    suspend fun getStateById(id: String): StateEntity?
}
