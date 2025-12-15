package dev.gaelicthunder.spoolsync.data.remote

import retrofit2.http.GET

interface SpoolmanDbApi {
    
    @GET("/api/v1/filament")
    suspend fun getFilaments(): List<SpoolmanFilament>
    
    @GET("/api/v1/vendor")
    suspend fun getBrands(): List<BrandInfo>
}
