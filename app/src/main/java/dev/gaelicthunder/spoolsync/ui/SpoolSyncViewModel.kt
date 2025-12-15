package dev.gaelicthunder.spoolsync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dev.gaelicthunder.spoolsync.data.AppDatabase
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import dev.gaelicthunder.spoolsync.data.FilamentRepository
import dev.gaelicthunder.spoolsync.service.BambuMqttClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpoolSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FilamentRepository
    private val gson = Gson()
    private var mqttClient: BambuMqttClient? = null

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("") 
    val searchQuery = _searchQuery.asStateFlow()

    private val _availableBrands = MutableStateFlow<List<String>>(emptyList())
    val availableBrands = _availableBrands.asStateFlow()

    val allProfiles: StateFlow<List<FilamentProfile>>
    val favoriteProfiles: StateFlow<List<FilamentProfile>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = FilamentRepository(db.filamentProfileDao())

        allProfiles = repository.allProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        favoriteProfiles = repository.favoriteProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadBrands()
    }

    private fun loadBrands() {
        viewModelScope.launch {
            _availableBrands.value = repository.getBrands()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchFilaments(query: String) {
        viewModelScope.launch {
            try {
                repository.searchAndCache(query)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createCustom(
        name: String,
        brand: String,
        material: String,
        colorHex: String?,
        minTemp: Int?,
        maxTemp: Int?,
        bedTemp: Int?
    ) {
        viewModelScope.launch {
            val profile = FilamentProfile(
                name = name,
                brand = brand,
                material = material,
                colorHex = colorHex,
                minTemp = minTemp,
                maxTemp = maxTemp,
                bedTemp = bedTemp,
                isFavorite = true,
                isCustom = true
            )
            repository.createCustom(profile)
        }
    }

    fun toggleFavorite(profile: FilamentProfile) {
        viewModelScope.launch {
            repository.toggleFavorite(profile.id, profile.isFavorite)
        }
    }

    fun deleteProfile(profile: FilamentProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun exportProfile(profile: FilamentProfile): String {
        return gson.toJson(profile)
    }

    fun connectPrinter(ip: String, serial: String, accessCode: String) {
        _connectionStatus.value = "Connecting..."
        mqttClient = BambuMqttClient(getApplication(), ip, serial, accessCode)
        mqttClient?.connect(
            onConnected = { _connectionStatus.value = "Connected" },
            onError = { error -> _connectionStatus.value = "Error: $error" }
        )
    }

    fun syncFilamentToAms(filament: FilamentProfile, amsId: Int, trayId: Int) {
        mqttClient?.setAmsFilament(amsId, trayId, filament)
    }

    fun disconnectPrinter() {
        mqttClient?.disconnect()
        mqttClient = null
        _connectionStatus.value = "Disconnected"
    }

    override fun onCleared() {
        super.onCleared()
        disconnectPrinter()
    }
}
