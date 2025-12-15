package dev.gaelicthunder.spoolsync.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.gaelicthunder.spoolsync.data.FilamentProfile

@JsonClass(generateAdapter = true)
data class SpoolmanFilament(
    val id: Int,
    val name: String,
    @Json(name = "vendor") val brand: BrandInfo?,
    val material: String?,
    @Json(name = "color_hex") val colorHex: String?,
    @Json(name = "spool_weight") val spoolWeight: Double?,
    @Json(name = "density") val density: Double?,
    @Json(name = "diameter") val diameter: Double?,
    @Json(name = "settings_extruder_temp") val extruderTemp: Int?,
    @Json(name = "settings_bed_temp") val bedTemp: Int?
) {
    fun toLocalProfile(): FilamentProfile? {
        val brandName = brand?.name ?: return null
        val materialName = material ?: return null
        
        return FilamentProfile(
            name = name,
            brand = brandName,
            material = materialName,
            colorHex = colorHex,
            minTemp = extruderTemp,
            maxTemp = extruderTemp?.plus(10),
            bedTemp = bedTemp,
            density = density ?: 1.24,
            diameter = diameter ?: 1.75,
            vendorId = id.toString(),
            isFavorite = false,
            isCustom = false
        )
    }
}

@JsonClass(generateAdapter = true)
data class BrandInfo(
    val id: Int,
    val name: String
)
