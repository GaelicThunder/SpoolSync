package dev.gaelicthunder.spoolsync.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_filaments")
data class CachedSpoolmanFilament(
    @PrimaryKey val id: String,
    val manufacturer: String,
    val name: String,
    val material: String,
    val density: Double,
    val weight: Double,
    val spoolWeight: Double?,
    val diameter: Double,
    val colorHex: String?,
    val extruderTemp: Int?,
    val bedTemp: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)
