package dev.gaelicthunder.spoolsync.data

import android.util.Log
import dev.gaelicthunder.spoolsync.data.cache.CachedFilamentDao
import dev.gaelicthunder.spoolsync.data.cache.CachedSpoolmanFilament
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadata
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadataDao
import dev.gaelicthunder.spoolsync.data.local.FilamentProfileDao
import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FilamentRepository(
    private val dao: FilamentProfileDao,
    private val cachedFilamentDao: CachedFilamentDao,
    private val cacheMetadataDao: CacheMetadataDao
) {
    companion object {
        private const val TAG = "FilamentRepository"
        private const val CACHE_KEY = "spoolman_filaments"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    val allProfiles: Flow<List<FilamentProfile>> = dao.getAllProfiles()
    val favoriteProfiles: Flow<List<FilamentProfile>> = dao.getFavoriteProfiles()

    suspend fun syncSpoolmanDbIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadata = cacheMetadataDao.get(CACHE_KEY)
            val now = System.currentTimeMillis()
            
            // Check if cache exists and is fresh
            if (metadata != null && (now - metadata.lastSync) < CACHE_EXPIRY_MS) {
                val count = cachedFilamentDao.getCount()
                if (count > 0) {
                    Log.d(TAG, "Cache is fresh ($count items), skipping sync")
                    return@withContext false
                }
            }
            
            Log.d(TAG, "Syncing SpoolmanDB...")
            val filaments = ApiClient.spoolmanDbApi.getFilamentsJson()
            Log.d(TAG, "Downloaded ${filaments.size} filaments")
            
            val cached = filaments.map {
                CachedSpoolmanFilament(
                    id = it.id,
                    manufacturer = it.manufacturer,
                    name = it.name,
                    material = it.material,
                    density = it.density,
                    weight = it.weight,
                    spoolWeight = it.spoolWeight,
                    diameter = it.diameter,
                    colorHex = it.colorHex,
                    extruderTemp = it.extruderTemp,
                    bedTemp = it.bedTemp
                )
            }
            
            cachedFilamentDao.clearAll()
            cachedFilamentDao.insertAll(cached)
            cacheMetadataDao.insert(CacheMetadata(CACHE_KEY, now))
            
            Log.d(TAG, "Sync complete: ${cached.size} filaments cached")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            false
        }
    }

    suspend fun getBrandsFromCache(): List<String> = withContext(Dispatchers.IO) {
        try {
            cachedFilamentDao.getAllBrands()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get brands", e)
            emptyList()
        }
    }

    suspend fun getMaterialsFromCache(): List<String> = withContext(Dispatchers.IO) {
        try {
            cachedFilamentDao.getAllMaterials()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get materials", e)
            emptyList()
        }
    }

    suspend fun searchCachedFilaments(
        query: String = "",
        brands: List<String> = emptyList(),
        materials: List<String> = emptyList()
    ): List<CachedSpoolmanFilament> = withContext(Dispatchers.IO) {
        try {
            cachedFilamentDao.search(
                searchQuery = query,
                brands = brands.ifEmpty { null },
                materials = materials.ifEmpty { null }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            emptyList()
        }
    }

    suspend fun importFromCache(cachedFilament: CachedSpoolmanFilament) = withContext(Dispatchers.IO) {
        val profile = FilamentProfile(
            name = cachedFilament.name,
            brand = cachedFilament.manufacturer,
            material = cachedFilament.material,
            colorHex = cachedFilament.colorHex,
            minTemp = cachedFilament.extruderTemp,
            maxTemp = cachedFilament.extruderTemp?.plus(10),
            bedTemp = cachedFilament.bedTemp,
            density = cachedFilament.density.toFloat(),
            diameter = cachedFilament.diameter.toFloat(),
            vendorId = cachedFilament.id,
            isFavorite = false,
            isCustom = false
        )
        dao.insert(profile)
    }

    suspend fun createCustom(profile: FilamentProfile) = withContext(Dispatchers.IO) {
        dao.insert(profile)
    }

    suspend fun toggleFavorite(id: Long, currentState: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, !currentState)
    }

    suspend fun deleteProfile(profile: FilamentProfile) = withContext(Dispatchers.IO) {
        dao.delete(profile)
    }
}
