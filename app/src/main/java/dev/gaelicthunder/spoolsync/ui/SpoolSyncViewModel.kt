package dev.gaelicthunder.spoolsync.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import dev.gaelicthunder.spoolsync.auth.GoogleAuthManager
import dev.gaelicthunder.spoolsync.data.AppDatabase
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import dev.gaelicthunder.spoolsync.data.FilamentRepository
import dev.gaelicthunder.spoolsync.data.cache.CachedSpoolmanFilament
import dev.gaelicthunder.spoolsync.data.remote.ApiClient
import dev.gaelicthunder.spoolsync.drive.DriveManager
import dev.gaelicthunder.spoolsync.service.BambuMqttClient
import dev.gaelicthunder.spoolsync.util.QRCodeGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpoolSyncViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SpoolSyncViewModel"
        private const val COLOR_PAGE_SIZE = 10
    }

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

    private val _selectedBrands = MutableStateFlow<Set<String>>(emptySet())
    val selectedBrands = _selectedBrands.asStateFlow()

    private val _selectedMaterials = MutableStateFlow<Set<String>>(emptySet())
    val selectedMaterials = _selectedMaterials.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CachedSpoolmanFilament>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _filamentColorsSwatches = MutableStateFlow<List<FilamentColorResult>>(emptyList())
    val filamentColorsSwatches = _filamentColorsSwatches.asStateFlow()

    private val _colorBrowserPage = MutableStateFlow(0)
    private val _hasMoreColors = MutableStateFlow(true)
    val hasMoreColors = _hasMoreColors.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

    val allProfiles: StateFlow<List<FilamentProfile>>
    val favoriteProfiles: StateFlow<List<FilamentProfile>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = FilamentRepository(
            db.filamentProfileDao(),
            db.cachedFilamentDao(),
            db.cacheMetadataDao()
        )

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

        syncAndLoadData()
    }

    private fun syncAndLoadData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val synced = repository.syncSpoolmanDbIfNeeded()
                if (synced) {
                    Log.d(TAG, "SpoolmanDB synced successfully")
                }
                loadBrandsAndMaterials()
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun loadBrandsAndMaterials() {
        viewModelScope.launch {
            try {
                val brands = repository.getBrandsFromCache()
                val materials = repository.getMaterialsFromCache()
                
                _availableBrands.value = brands
                _availableMaterials.value = materials
                
                Log.d(TAG, "Loaded ${brands.size} brands and ${materials.size} materials")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load brands/materials", e)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    fun toggleBrandFilter(brand: String) {
        _selectedBrands.value = if (_selectedBrands.value.contains(brand)) {
            _selectedBrands.value - brand
        } else {
            _selectedBrands.value + brand
        }
    }

    fun toggleMaterialFilter(material: String) {
        _selectedMaterials.value = if (_selectedMaterials.value.contains(material)) {
            _selectedMaterials.value - material
        } else {
            _selectedMaterials.value + material
        }
    }

    fun clearBrandFilters() {
        _selectedBrands.value = emptySet()
    }

    fun clearMaterialFilters() {
        _selectedMaterials.value = emptySet()
    }

    fun clearAllFilters() {
        _selectedBrands.value = emptySet()
        _selectedMaterials.value = emptySet()
        _searchQuery.value = ""
        performSearch()
    }

    fun applyFilters() {
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch {
            try {
                val results = repository.searchCachedFilaments(
                    query = _searchQuery.value,
                    brands = _selectedBrands.value.toList(),
                    materials = _selectedMaterials.value.toList()
                )
                _searchResults.value = results
                Log.d(TAG, "Search returned ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
            }
        }
    }

    fun importFilament(cached: CachedSpoolmanFilament) {
        viewModelScope.launch {
            try {
                repository.importFromCache(cached)
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
            }
        }
    }

    fun loadNextColorPage() {
        viewModelScope.launch {
            try {
                val page = _colorBrowserPage.value + 1
                val api = ApiClient.filamentColorsApi
                val response = api.getSwatches(page = page)
                
                if (response.results.isEmpty()) {
                    _hasMoreColors.value = false
                    return@launch
                }
                
                val newSwatches = response.results.map {
                    FilamentColorResult(
                        name = it.colorName,
                        brand = it.manufacturer.name,
                        material = it.filamentType.name,
                        hexColor = "#${it.hexColor}",
                        imageFront = it.imageFront?.let { "https://filamentcolors.xyz$it" },
                        imageBack = it.imageBack?.let { "https://filamentcolors.xyz$it" },
                        amazonLink = it.amazonLink
                    )
                }
                
                _filamentColorsSwatches.value += newSwatches
                _colorBrowserPage.value = page
                
                if (response.results.size < COLOR_PAGE_SIZE) {
                    _hasMoreColors.value = false
                }
                
                Log.d(TAG, "Loaded page $page with ${newSwatches.size} colors")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load color page", e)
                _hasMoreColors.value = false
            }
        }
    }

    fun resetColorBrowser() {
        _filamentColorsSwatches.value = emptyList()
        _colorBrowserPage.value = 0
        _hasMoreColors.value = true
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
