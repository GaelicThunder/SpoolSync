package dev.gaelicthunder.spoolsync.data.remote

import retrofit2.http.GET
import retrofit2.http.Streaming

interface SpoolmanDbApi {
    @GET("filaments.json")
    @Streaming
    suspend fun getFilamentsJson(): List<SpoolmanFilament>
    
    @GET("materials.json")
    @Streaming
    suspend fun getMaterialsJson(): List<SpoolmanMaterial>
}
