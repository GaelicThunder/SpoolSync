package dev.gaelicthunder.spoolsync.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolSyncApp(
    viewModel: SpoolSyncViewModel,
    onFilamentClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToColorBrowser: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val favoriteProfiles by viewModel.favoriteProfiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val availableBrands by viewModel.availableBrands.collectAsState()
    val availableMaterials by viewModel.availableMaterials.collectAsState()
    val selectedBrand by viewModel.selectedBrandFilter.collectAsState()
    val selectedMaterial by viewModel.selectedMaterialFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf(Section.HOME) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (userProfile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = userProfile!!.photoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = userProfile!!.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = userProfile!!.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.signInWithGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 16.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentSection == Section.HOME,
                    onClick = {
                        currentSection = Section.HOME
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = currentSection == Section.FAVORITES,
                    onClick = {
                        currentSection = Section.FAVORITES
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text("Scan QR/NFC") },
                    selected = false,
                    onClick = {
                        onNavigateToScanner()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    label = { Text("Color Browser") },
                    selected = false,
                    onClick = {
                        onNavigateToColorBrowser()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (userProfile != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        label = { Text("Backup to Drive") },
                        selected = false,
                        onClick = {
                            viewModel.backupToDrive()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                        label = { Text("Sign Out") },
                        selected = false,
                        onClick = {
                            viewModel.signOut()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        onNavigateToSettings()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            Text(
                                text = when (currentSection) {
                                    Section.HOME -> "SpoolSync"
                                    Section.FAVORITES -> "Favorites"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (connectionStatus != "Disconnected") {
                                Badge(
                                    containerColor = if (connectionStatus == "Connected") 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = connectionStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = { showFiltersDialog = true }) {
                                Badge(
                                    containerColor = if (selectedBrand != null || selectedMaterial != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = { Text("Search SpoolmanDB") },
                        placeholder = { Text("Brand, material, color...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { 
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchFilaments(searchQuery)
                                }
                            }
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = selectedBrand != null || selectedMaterial != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
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
                                    trailingIcon = { 
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    }
                                )
                            }
                            selectedMaterial?.let { material ->
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.setMaterialFilter(null) },
                                    label = { Text(material) },
                                    trailingIcon = { 
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                val displayProfiles = remember(currentSection, allProfiles, favoriteProfiles, selectedBrand, selectedMaterial) {
                    when (currentSection) {
                        Section.HOME -> {
                            allProfiles.filter { profile ->
                                (selectedBrand == null || profile.brand == selectedBrand) &&
                                (selectedMaterial == null || profile.material == selectedMaterial)
                            }
                        }
                        Section.FAVORITES -> favoriteProfiles
                    }
                }

                if (displayProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = when (currentSection) {
                                    Section.HOME -> Icons.Default.Search
                                    Section.FAVORITES -> Icons.Default.FavoriteBorder
                                },
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (currentSection) {
                                    Section.HOME -> "Search for filaments or create a custom profile"
                                    Section.FAVORITES -> "No favorites yet\nAdd filaments to your favorites to see them here"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(
                            items = displayProfiles,
                            key = { "profile_${it.id}_${it.isFavorite}" }
                        ) { profile ->
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

    if (showFiltersDialog) {
        FiltersDialog(
            brands = availableBrands,
            materials = availableMaterials,
            selectedBrand = selectedBrand,
            selectedMaterial = selectedMaterial,
            onBrandSelected = { 
                viewModel.setBrandFilter(it)
                if (it != null) viewModel.loadAllFilaments()
            },
            onMaterialSelected = { 
                viewModel.setMaterialFilter(it)
                if (it != null) viewModel.loadAllFilaments()
            },
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profile.colorHex?.let { hex ->
                Surface(
                    modifier = Modifier
                        .size(56.dp),
                    color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {}
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
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
                        Icon(
                            Icons.Default.Share, 
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

enum class Section {
    HOME, FAVORITES
}
