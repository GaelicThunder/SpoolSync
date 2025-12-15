package dev.gaelicthunder.spoolsync.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedFilamentDao {
    @Query("SELECT * FROM cached_filaments ORDER BY manufacturer, name")
    suspend fun getAll(): List<CachedSpoolmanFilament>
    
    @Query("SELECT DISTINCT manufacturer FROM cached_filaments ORDER BY manufacturer")
    suspend fun getAllBrands(): List<String>
    
    @Query("SELECT DISTINCT material FROM cached_filaments ORDER BY material")
    suspend fun getAllMaterials(): List<String>
    
    @Query("SELECT * FROM cached_filaments WHERE " +
           "(:searchQuery = '' OR name LIKE '%' || :searchQuery || '%' OR manufacturer LIKE '%' || :searchQuery || '%') " +
           "AND (:brands IS NULL OR manufacturer IN (:brands)) " +
           "AND (:materials IS NULL OR material IN (:materials))")
    suspend fun search(
        searchQuery: String,
        brands: List<String>?,
        materials: List<String>?
    ): List<CachedSpoolmanFilament>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filaments: List<CachedSpoolmanFilament>)
    
    @Query("DELETE FROM cached_filaments")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM cached_filaments")
    suspend fun getCount(): Int
}
