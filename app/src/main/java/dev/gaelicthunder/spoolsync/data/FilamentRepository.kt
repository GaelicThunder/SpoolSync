package dev.gaelicthunder.spoolsync.data

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gaelicthunder.spoolsync.data.cache.CachedFilamentDao
import dev.gaelicthunder.spoolsync.data.cache.CachedSpoolmanFilament
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadata
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadataDao
import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import dev.gaelicthunder.spoolsync.data.remote.SpoolmanFilament
import dev.gaelicthunder.spoolsync.data.remote.SpoolmanMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FilamentRepository(
    private val profileDao: FilamentProfileDao,
    private val cachedFilamentDao: CachedFilamentDao,
    private val cacheMetadataDao: CacheMetadataDao
) {
    companion object {
        private const val TAG = "FilamentRepository"
        private const val CACHE_KEY_SPOOLMAN = "spoolman_db"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L
    }

    val allProfiles: Flow<List<FilamentProfile>> = profileDao.getAllProfiles()
    val favoriteProfiles: Flow<List<FilamentProfile>> = profileDao.getFavoriteProfiles()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun syncSpoolmanDbIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadata = cacheMetadataDao.getMetadata(CACHE_KEY_SPOOLMAN)
            val now = System.currentTimeMillis()
            
            if (metadata == null || (now - metadata.lastUpdated) > CACHE_EXPIRY_MS) {
                Log.d(TAG, "Syncing SpoolmanDB...")
                return@withContext syncSpoolmanDb()
            } else {
                Log.d(TAG, "Cache still valid, skipping sync")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return@withContext false
        }
    }

    private suspend fun syncSpoolmanDb(): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            
            val filamentsResponse = api.getFilamentsJson()
            if (!filamentsResponse.isSuccessful) {
                Log.e(TAG, "Failed to fetch filaments: HTTP ${filamentsResponse.code()}")
                return@withContext false
            }
            
            val filamentsJson = filamentsResponse.body()?.string()
            if (filamentsJson == null) {
                Log.e(TAG, "Empty response body for filaments")
                return@withContext false
            }
            
            val filamentsType = Types.newParameterizedType(List::class.java, SpoolmanFilament::class.java)
            val filamentsAdapter: JsonAdapter<List<SpoolmanFilament>> = moshi.adapter(filamentsType)
            val filaments = filamentsAdapter.fromJson(filamentsJson)
            
            if (filaments == null || filaments.isEmpty()) {
                Log.e(TAG, "Failed to parse filaments or empty list")
                return@withContext false
            }
            
            Log.d(TAG, "Downloaded ${filaments.size} filaments")
            
            val materialsResponse = api.getMaterialsJson()
            if (!materialsResponse.isSuccessful) {
                Log.e(TAG, "Failed to fetch materials: HTTP ${materialsResponse.code()}")
                return@withContext false
            }
            
            val materialsJson = materialsResponse.body()?.string()
            if (materialsJson == null) {
                Log.e(TAG, "Empty response body for materials")
                return@withContext false
            }
            
            val materialsType = Types.newParameterizedType(List::class.java, SpoolmanMaterial::class.java)
            val materialsAdapter: JsonAdapter<List<SpoolmanMaterial>> = moshi.adapter(materialsType)
            val materials = materialsAdapter.fromJson(materialsJson)
            
            if (materials == null) {
                Log.e(TAG, "Failed to parse materials")
                return@withContext false
            }
            
            Log.d(TAG, "Downloaded ${materials.size} materials")
            
            val materialMap = materials.associateBy { it.id }
            
            cachedFilamentDao.clearAll()
            
            val cachedFilaments = filaments.map { filament ->
                val material = materialMap[filament.material_id]
                CachedSpoolmanFilament(
                    id = filament.id,
                    name = filament.name,
                    manufacturer = filament.manufacturer ?: "Unknown",
                    material = material?.name ?: "Unknown",
                    density = filament.density,
                    diameter = filament.diameter,
                    extruderTemp = filament.extruder_temp,
                    bedTemp = filament.bed_temp,
                    colorHex = filament.color_hex,
                    articleNumber = filament.article_number,
                    settings = filament.settings_extruder_temp?.let { "extruder:$it" } ?: ""
                )
            }
            
            cachedFilamentDao.insertAll(cachedFilaments)
            
            cacheMetadataDao.insert(
                CacheMetadata(
                    key = CACHE_KEY_SPOOLMAN,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            
            Log.d(TAG, "Successfully cached ${cachedFilaments.size} filaments")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return@withContext false
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
        query: String,
        brands: List<String>,
        materials: List<String>
    ): List<CachedSpoolmanFilament> = withContext(Dispatchers.IO) {
        try {
            cachedFilamentDao.searchFilaments(query, brands, materials)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            emptyList()
        }
    }

    suspend fun importFromCache(cached: CachedSpoolmanFilament) = withContext(Dispatchers.IO) {
        val profile = FilamentProfile(
            name = cached.name,
            brand = cached.manufacturer,
            material = cached.material,
            colorHex = cached.colorHex,
            minTemp = cached.extruderTemp,
            maxTemp = cached.extruderTemp?.plus(10),
            bedTemp = cached.bedTemp,
            density = cached.density.toFloat(),
            diameter = cached.diameter.toFloat(),
            vendorId = cached.id,
            isFavorite = true,
            isCustom = false
        )
        profileDao.insert(profile)
    }

    suspend fun createCustom(profile: FilamentProfile) = withContext(Dispatchers.IO) {
        profileDao.insert(profile.copy(isCustom = true, isFavorite = true))
    }

    suspend fun toggleFavorite(id: Long, currentState: Boolean) = withContext(Dispatchers.IO) {
        profileDao.updateFavorite(id, !currentState)
    }

    suspend fun deleteProfile(profile: FilamentProfile) = withContext(Dispatchers.IO) {
        profileDao.delete(profile)
    }
}
