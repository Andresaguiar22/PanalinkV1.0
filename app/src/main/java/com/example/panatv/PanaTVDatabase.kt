package com.example.panatv

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "panatv_favorites")
data class PanaTVFavoriteEntity(
    @PrimaryKey val id: String
)

@Dao
interface PanaTVFavoriteDao {
    @Query("SELECT * FROM panatv_favorites")
    fun getFavorites(): Flow<List<PanaTVFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: PanaTVFavoriteEntity)

    @Query("DELETE FROM panatv_favorites WHERE id = :id")
    suspend fun removeFavorite(id: String)
}

@Dao
interface PanaTVChannelDao {
    @Query("SELECT * FROM panatv_channels WHERE (name LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%') AND (country = :country OR :country = '')")
    fun searchChannels(query: String, country: String): Flow<List<PanaTVChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<PanaTVChannelEntity>)
    
    @Query("SELECT COUNT(*) FROM panatv_channels")
    suspend fun getChannelCount(): Int

    @Query("DELETE FROM panatv_channels")
    suspend fun clearChannels()
}

@Database(entities = [PanaTVChannelEntity::class, PanaTVFavoriteEntity::class], version = 3, exportSchema = false)
abstract class PanaTVDatabase : RoomDatabase() {
    abstract fun channelDao(): PanaTVChannelDao
    abstract fun favoriteDao(): PanaTVFavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: PanaTVDatabase? = null

        fun getDatabase(context: Context): PanaTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PanaTVDatabase::class.java,
                    "panatv_independent_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
