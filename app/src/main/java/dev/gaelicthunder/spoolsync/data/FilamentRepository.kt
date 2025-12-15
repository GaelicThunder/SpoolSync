package dev.gaelicthunder.spoolsync.data

import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import dev.gaelicthunder.spoolsync.data.remote.SpoolmanFilament
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FilamentRepository(private val dao: FilamentProfileDao) {

    val allProfiles = dao.getAllProfiles()
    val favoriteProfiles = dao.getFavoriteProfiles()

    suspend fun syncSpoolmanDb(): List<FilamentProfile> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            val remote = api.getFilaments()
            remote.mapNotNull { it.toLocalProfile() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getBrands(): List<String> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.spoolmanDbApi
            val remote = api.getFilaments()
            remote.map { it.manufacturer }.distinct().sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchAndCache(query: String) {
        val allRemote = syncSpoolmanDb()
        val filtered = if (query.isBlank()) {
            allRemote.take(50)
        } else {
            allRemote.filter {
                it.brand.contains(query, ignoreCase = true) ||
                it.material.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
            }
        }
        dao.clearNonCustomProfiles()
        filtered.forEach { dao.insert(it) }
    }

    suspend fun createCustom(profile: FilamentProfile): Long {
        return dao.insert(profile.copy(isCustom = true, isFavorite = true))
    }

    suspend fun toggleFavorite(id: Long, current: Boolean) {
        dao.setFavorite(id, !current)
    }

    suspend fun deleteProfile(profile: FilamentProfile) {
        dao.delete(profile)
    }
}

private fun SpoolmanFilament.toLocalProfile(): FilamentProfile? {
    val hexColor = colorHex?.let { "#$it" } ?: return null
    val min = extruderTemp ?: defaultTemps(material).first
    val max = (extruderTemp?.plus(10)) ?: defaultTemps(material).second
    return FilamentProfile(
        name = name,
        brand = manufacturer,
        material = material,
        colorHex = hexColor,
        minTemp = min,
        maxTemp = max,
        bedTemp = bedTemp,
        density = density,
        diameter = diameter,
        vendorId = id,
        isFavorite = false,
        isCustom = false
    )
}

private fun defaultTemps(material: String): Pair<Int, Int> = when (material.uppercase()) {
    "PLA" -> 200 to 220
    "PETG" -> 230 to 250
    "ABS" -> 240 to 260
    "TPU" -> 220 to 240
    "NYLON" -> 250 to 270
    "ASA" -> 240 to 260
    else -> 200 to 230
}
