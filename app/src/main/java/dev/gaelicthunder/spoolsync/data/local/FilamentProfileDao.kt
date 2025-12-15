package dev.gaelicthunder.spoolsync.data.local

import androidx.room.*
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface FilamentProfileDao {

    @Query("SELECT * FROM filament_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<FilamentProfile>>

    @Query("SELECT * FROM filament_profiles WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteProfiles(): Flow<List<FilamentProfile>>

    @Query("SELECT * FROM filament_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): FilamentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: FilamentProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<FilamentProfile>)

    @Update
    suspend fun update(profile: FilamentProfile)

    @Delete
    suspend fun delete(profile: FilamentProfile)

    @Query("UPDATE filament_profiles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM filament_profiles")
    suspend fun deleteAll()
}
