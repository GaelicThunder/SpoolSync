package dev.gaelicthunder.spoolsync.data

import dev.gaelicthunder.spoolsync.data.local.FilamentProfileDao
import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FilamentRepository(private val dao: FilamentProfileDao) {

    val allProfiles: Flow<List<FilamentProfile>> = dao.getAllProfiles()
    val favoriteProfiles: Flow<List<FilamentProfile>> = dao.getFavoriteProfiles()

    suspend fun searchAndCache(query: String) = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            val response = api.getFilaments()
            val results = response.mapNotNull { it.toLocalProfile() }
                .filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.brand.contains(query, ignoreCase = true) ||
                    it.material.contains(query, ignoreCase = true)
                }
            dao.insertAll(results)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadAllFromSpoolmanDB() = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            val response = api.getFilaments()
            val results = response.mapNotNull { it.toLocalProfile() }
            dao.insertAll(results)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getBrands(): List<String> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            val response = api.getBrands()
            response.map { it.name }.sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
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
