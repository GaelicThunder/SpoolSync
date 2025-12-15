package dev.gaelicthunder.spoolsync.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    suspend fun get(key: String): CacheMetadata?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: CacheMetadata)
    
    @Query("DELETE FROM cache_metadata WHERE key = :key")
    suspend fun delete(key: String)
}
