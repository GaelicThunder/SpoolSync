package dev.gaelicthunder.spoolsync.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.gaelicthunder.spoolsync.data.cache.CachedFilamentDao
import dev.gaelicthunder.spoolsync.data.cache.CachedSpoolmanFilament
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadata
import dev.gaelicthunder.spoolsync.data.cache.CacheMetadataDao
import dev.gaelicthunder.spoolsync.data.local.FilamentProfileDao

@Database(
    entities = [
        FilamentProfile::class,
        CachedSpoolmanFilament::class,
        CacheMetadata::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun filamentProfileDao(): FilamentProfileDao
    abstract fun cachedFilamentDao(): CachedFilamentDao
    abstract fun cacheMetadataDao(): CacheMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spoolsync_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
