package dev.gaelicthunder.spoolsync.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import dev.gaelicthunder.spoolsync.auth.GoogleAuthManager
import dev.gaelicthunder.spoolsync.data.AppDatabase
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import dev.gaelicthunder.spoolsync.data.FilamentRepository
import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import dev.gaelicthunder.spoolsync.drive.DriveManager
import dev.gaelicthunder.spoolsync.service.BambuMqttClient
import dev.gaelicthunder.spoolsync.util.QRCodeGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpoolSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FilamentRepository
    private val gson = Gson()
    private var mqttClient: BambuMqttClient? = null
    private var authManager: GoogleAuthManager? = null
    private var signInLauncher: ActivityResultLauncher<android.content.Intent>? = null
    private var driveManager: DriveManager? = null

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("") 
    val searchQuery = _searchQuery.asStateFlow()

    private val _availableBrands = MutableStateFlow<List<String>>(emptyList())
    val availableBrands = _availableBrands.asStateFlow()

    private val _availableMaterials = MutableStateFlow<List<String>>(emptyList())
    val availableMaterials = _availableMaterials.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow<String?>(null)
    val selectedBrandFilter = _selectedBrandFilter.asStateFlow()

    private val _selectedMaterialFilter = MutableStateFlow<String?>(null)
    val selectedMaterialFilter = _selectedMaterialFilter.asStateFlow()

    private val _filamentColorsSwatches = MutableStateFlow<List<FilamentColorResult>>(emptyList())
    val filamentColorsSwatches = _filamentColorsSwatches.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

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
        loadMaterials()
    }

    fun setAuthManager(manager: GoogleAuthManager, launcher: ActivityResultLauncher<android.content.Intent>) {
        authManager = manager
        signInLauncher = launcher
    }

    fun onGoogleSignInSuccess(context: Context, account: GoogleSignInAccount) {
        _userProfile.value = UserProfile(
            displayName = account.displayName ?: "User",
            email = account.email ?: "",
            photoUrl = account.photoUrl?.toString() ?: ""
        )
        driveManager = DriveManager(context, account)
    }

    private fun loadBrands() {
        viewModelScope.launch {
            try {
                val brands = repository.getBrands()
                if (brands.isNotEmpty()) {
                    _availableBrands.value = brands
                } else {
                    val api = ApiClient.spoolmanDbApi
                    val brandsFromApi = api.getBrands()
                    _availableBrands.value = brandsFromApi.map { it.name }.sorted()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _availableBrands.value = getDefaultBrands()
            }
        }
    }

    private fun loadMaterials() {
        viewModelScope.launch {
            _availableMaterials.value = getDefaultMaterials()
        }
    }

    private fun getDefaultBrands(): List<String> {
        return listOf(
            "3D-Fuel", "3DXTech", "Anycubic", "Bambu Lab", "Colorfabb", 
            "Creality", "Elegoo", "eSUN", "Fiberlogy", "Fillamentum", 
            "FormFutura", "Geeetech", "Hatchbox", "MatterHackers", "Overture", 
            "PolyLite", "PolyMaker", "Prusament", "Push Plastic", "Reprapper",
            "Sunlu", "Ultimaker"
        ).sorted()
    }

    private fun getDefaultMaterials(): List<String> {
        return listOf(
            "PLA", "PLA+", "PETG", "ABS", "ASA", "TPU", "TPE",
            "NYLON", "PC", "PAHT-CF", "PVA", "HIPS", "PP"
        )
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBrandFilter(brand: String?) {
        _selectedBrandFilter.value = brand
    }

    fun setMaterialFilter(material: String?) {
        _selectedMaterialFilter.value = material
    }

    fun loadAllFilaments() {
        viewModelScope.launch {
            try {
                repository.loadAllFromSpoolmanDB()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchFilaments(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                repository.searchAndCache(query)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchFilamentColors(query: String = "") {
        viewModelScope.launch {
            try {
                val api = ApiClient.filamentColorsApi
                val allSwatches = mutableListOf<FilamentColorResult>()
                
                for (page in 1..5) {
                    try {
                        val response = api.getSwatches(page = page)
                        if (response.results.isEmpty()) break
                        
                        allSwatches.addAll(response.results.map {
                            FilamentColorResult(
                                name = it.colorName,
                                brand = it.manufacturer.name,
                                material = it.filamentType.name,
                                hexColor = "#${it.hexColor}",
                                imageFront = it.imageFront,
                                imageBack = it.imageBack,
                                amazonLink = it.amazonLink
                            )
                        })
                    } catch (e: Exception) {
                        break
                    }
                }
                
                _filamentColorsSwatches.value = allSwatches
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getFilamentColorImage(brand: String, name: String): String? {
        return try {
            val api = ApiClient.filamentColorsApi
            val response = api.getSwatches(page = 1, manufacturer = brand)
            response.results.find { 
                it.colorName.equals(name, ignoreCase = true) 
            }?.imageFront?.let { "https://filamentcolors.xyz$it" }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
            val currentFavorites = favoriteProfiles.value
            val isDuplicate = currentFavorites.any { it.id == profile.id }
            
            if (!isDuplicate || profile.isFavorite) {
                repository.toggleFavorite(profile.id, profile.isFavorite)
            }
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

    fun generateQRCode(profile: FilamentProfile): Bitmap? {
        val json = exportProfile(profile)
        return QRCodeGenerator.generateQRCode(json)
    }

    fun signInWithGoogle() {
        authManager?.let { manager ->
            signInLauncher?.launch(manager.getSignInIntent())
        }
    }

    fun signOut() {
        authManager?.signOut {
            _userProfile.value = null
            driveManager = null
        }
    }

    fun backupToDrive() {
        viewModelScope.launch {
            _backupStatus.value = "Backing up..."
            try {
                val profiles = favoriteProfiles.value + allProfiles.value.filter { it.isCustom }
                val json = gson.toJson(profiles)
                
                val fileId = driveManager?.uploadBackup("spoolsync_backup.json", json)
                
                _backupStatus.value = if (fileId != null) {
                    "Backup successful!"
                } else {
                    "Backup failed"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _backupStatus.value = "Error: ${e.message}"
            }
        }
    }

    fun restoreFromDrive() {
        viewModelScope.launch {
            _backupStatus.value = "Restoring..."
            try {
                val json = driveManager?.downloadBackup("spoolsync_backup.json")
                if (json != null) {
                    val profiles = gson.fromJson(json, Array<FilamentProfile>::class.java).toList()
                    profiles.forEach { repository.createCustom(it) }
                    _backupStatus.value = "Restore successful!"
                } else {
                    _backupStatus.value = "No backup found"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _backupStatus.value = "Error: ${e.message}"
            }
        }
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

data class FilamentColorResult(
    val name: String,
    val brand: String,
    val material: String,
    val hexColor: String,
    val imageFront: String?,
    val imageBack: String?,
    val amazonLink: String?
)

data class UserProfile(
    val displayName: String,
    val email: String,
    val photoUrl: String
)
