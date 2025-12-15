package dev.gaelicthunder.spoolsync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filament_profiles")
data class FilamentProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val material: String,
    val colorHex: String?,
    val minTemp: Int?,
    val maxTemp: Int?,
    val bedTemp: Int?,
    val density: Float = 1.24f,
    val diameter: Float = 1.75f,
    val vendorId: String = "",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
) {
    fun getBambuColor(): String {
        val hex = colorHex?.replace("#", "") ?: "FFFFFFFF"
        return if (hex.length == 6) "${hex}FF" else hex
    }
}
