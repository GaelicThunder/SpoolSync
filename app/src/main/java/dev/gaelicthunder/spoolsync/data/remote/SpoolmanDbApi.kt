package dev.gaelicthunder.spoolsync.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming

interface SpoolmanDbApi {
    @GET("filaments.json")
    @Streaming
    suspend fun getFilamentsJson(): Response<ResponseBody>
    
    @GET("materials.json")
    @Streaming
    suspend fun getMaterialsJson(): Response<ResponseBody>
}
