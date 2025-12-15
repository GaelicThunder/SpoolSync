package dev.gaelicthunder.spoolsync.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaelicthunder.spoolsync.data.FilamentProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentDetailScreen(
    filamentId: Long,
    viewModel: SpoolSyncViewModel,
    onBack: () -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val favoriteProfiles by viewModel.favoriteProfiles.collectAsState()
    val context = LocalContext.current

    val profile = remember(filamentId, allProfiles, favoriteProfiles) {
        (allProfiles + favoriteProfiles).find { it.id == filamentId }
    }

    var showQRCode by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showAmsDialog by remember { mutableStateOf(false) }

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Profile not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(profile) }) {
                        Icon(
                            imageVector = if (profile.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (profile.isFavorite) "Unfavorite" else "Favorite",
                            tint = if (profile.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val json = viewModel.exportProfile(profile)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "SpoolSync: ${profile.name}")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share profile"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            profile.colorHex?.let { hex ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color.Gray
                    },
                    shape = MaterialTheme.shapes.large
                ) {}
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "Filament Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Brand", profile.brand)
            DetailRow("Material", profile.material)
            profile.colorHex?.let { DetailRow("Color", it) }
            DetailRow("Min Temperature", "${profile.minTemp}°C")
            DetailRow("Max Temperature", "${profile.maxTemp}°C")
            profile.bedTemp?.let { DetailRow("Bed Temperature", "${it}°C") }
            DetailRow("Density", "${profile.density} g/cm³")
            DetailRow("Diameter", "${profile.diameter} mm")
            if (profile.vendorId.isNotEmpty()) {
                DetailRow("Vendor ID", profile.vendorId)
            }
            if (profile.isCustom) {
                DetailRow("Type", "Custom Profile")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAmsDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync to AMS")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    qrCodeBitmap = viewModel.generateQRCode(profile)
                    showQRCode = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate QR Code")
            }

            if (profile.isCustom) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.deleteProfile(profile)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Profile")
                }
            }
        }
    }

    if (showQRCode && qrCodeBitmap != null) {
        QRCodeDialog(
            bitmap = qrCodeBitmap!!,
            profileName = profile.name,
            onDismiss = { showQRCode = false }
        )
    }

    if (showAmsDialog) {
        AmsSyncDialog(
            profile = profile,
            onSync = { amsId, trayId ->
                viewModel.syncFilamentToAms(profile, amsId, trayId)
                showAmsDialog = false
            },
            onDismiss = { showAmsDialog = false }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
    Divider()
}

@Composable
fun QRCodeDialog(
    bitmap: Bitmap,
    profileName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OpenPrintTag QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.size(280.dp),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Scan this code to import the filament profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AmsSyncDialog(
    profile: FilamentProfile,
    onSync: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amsId by remember { mutableStateOf("0") }
    var trayId by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync to AMS") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select AMS and tray to sync ${profile.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = amsId,
                    onValueChange = { amsId = it },
                    label = { Text("AMS ID (0-3)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trayId,
                    onValueChange = { trayId = it },
                    label = { Text("Tray ID (0-3)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ams = amsId.toIntOrNull() ?: 0
                    val tray = trayId.toIntOrNull() ?: 0
                    onSync(ams, tray)
                },
                enabled = amsId.toIntOrNull() in 0..3 && trayId.toIntOrNull() in 0..3
            ) {
                Text("Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
