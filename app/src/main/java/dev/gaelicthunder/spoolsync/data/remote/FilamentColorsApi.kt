package dev.gaelicthunder.spoolsync.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface FilamentColorsApi {
    @GET("/api/swatch/")
    suspend fun getSwatches(
        @Query("page") page: Int = 1,
        @Query("manufacturer") manufacturer: String? = null,
        @Query("type") type: String? = null
    ): FilamentColorsResponse
}

@JsonClass(generateAdapter = true)
data class FilamentColorsResponse(
    @Json(name = "count") val count: Int,
    @Json(name = "next") val next: String?,
    @Json(name = "previous") val previous: String?,
    @Json(name = "results") val results: List<FilamentColorsSwatch>
)

@JsonClass(generateAdapter = true)
data class FilamentColorsSwatch(
    @Json(name = "id") val id: Int,
    @Json(name = "color_name") val colorName: String,
    @Json(name = "manufacturer") val manufacturer: FilamentManufacturer,
    @Json(name = "filament_type") val filamentType: FilamentType,
    @Json(name = "color_parent") val colorParent: String?,
    @Json(name = "hex_color") val hexColor: String,
    @Json(name = "image_front") val imageFront: String?,
    @Json(name = "image_back") val imageBack: String?,
    @Json(name = "image_other") val imageOther: String?,
    @Json(name = "amazon_purchase_link") val amazonLink: String?,
    @Json(name = "manufacturer_purchase_link") val manufacturerLink: String?
)

@JsonClass(generateAdapter = true)
data class FilamentManufacturer(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class FilamentType(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)
