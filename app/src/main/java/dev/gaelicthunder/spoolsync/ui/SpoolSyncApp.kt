package dev.gaelicthunder.spoolsync.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolSyncApp(
    viewModel: SpoolSyncViewModel,
    onFilamentClick: (Long) -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val favoriteProfiles by viewModel.favoriteProfiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val availableBrands by viewModel.availableBrands.collectAsState()
    val availableMaterials by viewModel.availableMaterials.collectAsState()
    val selectedBrand by viewModel.selectedBrandFilter.collectAsState()
    val selectedMaterial by viewModel.selectedMaterialFilter.collectAsState()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SpoolSync",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        label = { Text("Scan QR/NFC") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        label = { Text("Color Browser") },
                        selected = false,
                        onClick = {
                            viewModel.searchFilamentColors()
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            showSettingsDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create custom")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        Text(
                            text = "SpoolSync",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = connectionStatus,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { showFiltersDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search SpoolmanDB") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.searchFilaments(searchQuery) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.searchFilaments(searchQuery) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )

                if (selectedBrand != null || selectedMaterial != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedBrand?.let { brand ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setBrandFilter(null) },
                                label = { Text(brand) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                            )
                        }
                        selectedMaterial?.let { material ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setMaterialFilter(null) },
                                label = { Text(material) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredProfiles = allProfiles.filter {
                    (selectedBrand == null || it.brand == selectedBrand) &&
                    (selectedMaterial == null || it.material == selectedMaterial)
                }

                if (favoriteProfiles.isEmpty() && filteredProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search for filaments or create a custom profile",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (favoriteProfiles.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(favoriteProfiles, key = { it.id }) { profile ->
                                FilamentCard(
                                    profile = profile,
                                    onClick = { onFilamentClick(profile.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(profile) },
                                    onShare = {
                                        val json = viewModel.exportProfile(profile)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "SpoolSync: ${profile.name}")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share profile"))
                                    },
                                    onDelete = if (profile.isCustom) {
                                        { viewModel.deleteProfile(profile) }
                                    } else null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }

                        val nonFavorites = filteredProfiles.filter { !it.isFavorite }
                        if (nonFavorites.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Search Results",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(nonFavorites, key = { it.id }) { profile ->
                                FilamentCard(
                                    profile = profile,
                                    onClick = { onFilamentClick(profile.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(profile) },
                                    onShare = {
                                        val json = viewModel.exportProfile(profile)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "SpoolSync: ${profile.name}")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share profile"))
                                    },
                                    onDelete = null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateFilamentDialog(
            brands = availableBrands,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, brand, material, color, minTemp, maxTemp, bedTemp ->
                viewModel.createCustom(name, brand, material, color, minTemp, maxTemp, bedTemp)
                showCreateDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            connectionStatus = connectionStatus,
            onDismiss = { showSettingsDialog = false },
            onConnect = { ip, serial, code ->
                viewModel.connectPrinter(ip, serial, code)
            },
            onDisconnect = {
                viewModel.disconnectPrinter()
            }
        )
    }

    if (showFiltersDialog) {
        FiltersDialog(
            brands = availableBrands,
            materials = availableMaterials,
            selectedBrand = selectedBrand,
            selectedMaterial = selectedMaterial,
            onBrandSelected = { viewModel.setBrandFilter(it) },
            onMaterialSelected = { viewModel.setMaterialFilter(it) },
            onDismiss = { showFiltersDialog = false }
        )
    }
}

@Composable
fun FilamentCard(
    profile: FilamentProfile,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profile.colorHex?.let { hex ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        },
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${profile.brand} · ${profile.material}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nozzle: ${profile.minTemp}°C-${profile.maxTemp}°C${profile.bedTemp?.let { " · Bed: ${it}°C" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (profile.isCustom) {
                    Text(
                        text = "Custom Profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (profile.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (profile.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (profile.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
