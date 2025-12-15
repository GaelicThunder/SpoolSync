package dev.gaelicthunder.spoolsync.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val SPOOLMAN_DB_BASE = "https://donkie.github.io"
    private const val FILAMENT_COLORS_BASE = "https://filamentcolors.xyz"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val spoolmanDbApi: SpoolmanDbApi by lazy {
        Retrofit.Builder()
            .baseUrl(SPOOLMAN_DB_BASE)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SpoolmanDbApi::class.java)
    }

    val filamentColorsApi: FilamentColorsApi by lazy {
        Retrofit.Builder()
            .baseUrl(FILAMENT_COLORS_BASE)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FilamentColorsApi::class.java)
    }
}
