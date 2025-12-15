package dev.gaelicthunder.spoolsync.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface SpoolmanDbApi {
    
    @GET("/api/v1/filament")
    suspend fun getFilaments(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): List<SpoolmanFilament>
    
    @GET("/api/v1/vendor")
    suspend fun getBrands(): List<BrandInfo>
    
    @GET("/api/v1/filament")
    suspend fun searchFilaments(
        @Query("q") query: String,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): List<SpoolmanFilament>
}
