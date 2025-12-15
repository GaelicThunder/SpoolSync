package dev.gaelicthunder.spoolsync.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FilamentProfileDao {

    @Query("SELECT * FROM filament_profiles ORDER BY isFavorite DESC, brand ASC, name ASC")
    fun getAllProfiles(): Flow<List<FilamentProfile>>

    @Query("SELECT * FROM filament_profiles WHERE isFavorite = 1 ORDER BY brand ASC, name ASC")
    fun getFavoriteProfiles(): Flow<List<FilamentProfile>>

    @Query("SELECT * FROM filament_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): FilamentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: FilamentProfile): Long

    @Update
    suspend fun update(profile: FilamentProfile)

    @Delete
    suspend fun delete(profile: FilamentProfile)

    @Query("UPDATE filament_profiles SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM filament_profiles WHERE isCustom = 0")
    suspend fun clearNonCustomProfiles()
}
