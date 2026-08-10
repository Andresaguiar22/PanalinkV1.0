package com.example.creative.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CreativeProjectDao {

    @Query("SELECT * FROM creative_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): CreativeProjectEntity?

    @Query("SELECT * FROM creative_projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<CreativeProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: CreativeProjectEntity)

    @Query("DELETE FROM creative_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
