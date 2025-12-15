package dev.gaelicthunder.spoolsync.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

interface SpoolmanDbApi {
    @GET("/SpoolmanDB/filaments.json")
    suspend fun getFilaments(): List<SpoolmanFilament>
}

@JsonClass(generateAdapter = true)
data class SpoolmanFilament(
    @Json(name = "id") val id: String,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "name") val name: String,
    @Json(name = "material") val material: String,
    @Json(name = "density") val density: Float,
    @Json(name = "diameter") val diameter: Float,
    @Json(name = "weight") val weight: Int?,
    @Json(name = "spool_weight") val spoolWeight: Float?,
    @Json(name = "color_hex") val colorHex: String?,
    @Json(name = "color_name") val colorName: String?,
    @Json(name = "extruder_temp") val extruderTemp: Int?,
    @Json(name = "bed_temp") val bedTemp: Int?
)
