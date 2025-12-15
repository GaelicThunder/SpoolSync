package dev.gaelicthunder.spoolsync.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.gaelicthunder.spoolsync.data.FilamentProfile

@JsonClass(generateAdapter = true)
data class SpoolmanFilament(
    val id: String,
    val manufacturer: String,
    val name: String,
    val material: String,
    val density: Double,
    val weight: Double,
    @Json(name = "spool_weight") val spoolWeight: Double?,
    @Json(name = "spool_type") val spoolType: String?,
    val diameter: Double,
    @Json(name = "color_hex") val colorHex: String?,
    @Json(name = "color_hexes") val colorHexes: List<String>?,
    @Json(name = "extruder_temp") val extruderTemp: Int?,
    @Json(name = "bed_temp") val bedTemp: Int?,
    val finish: String?,
    @Json(name = "multi_color_direction") val multiColorDirection: String?,
    val pattern: String?,
    val translucent: Boolean = false,
    val glow: Boolean = false
) {
    fun toLocalProfile(): FilamentProfile {
        return FilamentProfile(
            name = name,
            brand = manufacturer,
            material = material,
            colorHex = colorHex,
            minTemp = extruderTemp,
            maxTemp = extruderTemp?.plus(10),
            bedTemp = bedTemp,
            density = density.toFloat(),
            diameter = diameter.toFloat(),
            vendorId = id,
            isFavorite = false,
            isCustom = false
        )
    }
}

@JsonClass(generateAdapter = true)
data class SpoolmanMaterial(
    val material: String,
    val density: Double,
    @Json(name = "extruder_temp") val extruderTemp: Int?,
    @Json(name = "bed_temp") val bedTemp: Int?
)
