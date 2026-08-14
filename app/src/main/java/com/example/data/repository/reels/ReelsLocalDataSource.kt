package com.example.data.repository.reels

import com.example.data.database.StateEntity
import com.example.data.database.StatesDao
import kotlinx.coroutines.flow.Flow

/**
 * Local data boundary for Reels.
 *
 * Reels use the existing Room table for persistence, but the feature no longer
 * needs to know that the shared StatesDao is the physical storage mechanism.
 * Remote synchronization remains outside this boundary until the next
 * migration step.
 */
class ReelsLocalDataSource(
    private val statesDao: StatesDao
) {
    fun observe(): Flow<List<StateEntity>> = statesDao.getStatesFlow(isReel = true)

    suspend fun getById(id: String): StateEntity? = statesDao.getStateById(id)

    suspend fun save(state: StateEntity) {
        require(state.isReel) { "ReelsLocalDataSource only accepts Reel states" }
        statesDao.insertState(state)
    }

    suspend fun saveAll(states: List<StateEntity>) {
        require(states.all { it.isReel }) { "ReelsLocalDataSource only accepts Reel states" }
        statesDao.insertStates(states)
    }

    suspend fun deleteById(id: String) = statesDao.deleteById(id)

    suspend fun updateLocalPath(id: String, path: String?) = statesDao.updateLocalPath(id, path)
}
