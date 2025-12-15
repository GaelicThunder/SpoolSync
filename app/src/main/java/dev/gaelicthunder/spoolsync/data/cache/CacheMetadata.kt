package dev.gaelicthunder.spoolsync.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey val key: String,
    val lastSync: Long,
    val version: Int = 1
)
