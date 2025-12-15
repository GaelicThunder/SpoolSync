package dev.gaelicthunder.spoolsync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFilamentDialog(
    brands: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?, Int?, Int?, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var showBrandDropdown by remember { mutableStateOf(false) }
    var material by remember { mutableStateOf("PLA") }
    var colorHex by remember { mutableStateOf("#FFFFFF") }
    var minTemp by remember { mutableStateOf("200") }
    var maxTemp by remember { mutableStateOf("220") }
    var bedTemp by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Filament") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = showBrandDropdown,
                    onExpandedChange = { showBrandDropdown = !showBrandDropdown }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBrandDropdown)
                        }
                    )
                    
                    if (brands.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = showBrandDropdown,
                            onDismissRequest = { showBrandDropdown = false }
                        ) {
                            brands.take(50).forEach { brandName ->
                                DropdownMenuItem(
                                    text = { Text(brandName) },
                                    onClick = {
                                        brand = brandName
                                        showBrandDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = material,
                    onValueChange = { material = it },
                    label = { Text("Material") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Color Hex") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = minTemp,
                        onValueChange = { minTemp = it },
                        label = { Text("Min °C") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = maxTemp,
                        onValueChange = { maxTemp = it },
                        label = { Text("Max °C") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bedTemp,
                    onValueChange = { bedTemp = it },
                    label = { Text("Bed °C") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        name,
                        brand,
                        material,
                        colorHex,
                        minTemp.toIntOrNull(),
                        maxTemp.toIntOrNull(),
                        bedTemp.toIntOrNull()
                    )
                },
                enabled = name.isNotBlank() && brand.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    connectionStatus: String,
    onDismiss: () -> Unit,
    onConnect: (String, String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var printerIp by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Printer Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Status: $connectionStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = printerIp,
                    onValueChange = { printerIp = it },
                    label = { Text("Printer IP") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Serial Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it },
                    label = { Text("Access Code") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                if (connectionStatus != "Disconnected") {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                }
                TextButton(
                    onClick = {
                        onConnect(printerIp, serialNumber, accessCode)
                    },
                    enabled = printerIp.isNotBlank() && serialNumber.isNotBlank() && accessCode.isNotBlank()
                ) {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersDialog(
    brands: List<String>,
    materials: List<String>,
    selectedBrand: String?,
    selectedMaterial: String?,
    onBrandSelected: (String?) -> Unit,
    onMaterialSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Filaments") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Brand",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    FilterChip(
                        selected = selectedBrand == null,
                        onClick = { onBrandSelected(null) },
                        label = { Text("All Brands") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(brands.take(20)) { brand ->
                    FilterChip(
                        selected = selectedBrand == brand,
                        onClick = { onBrandSelected(brand) },
                        label = { Text(brand) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Material",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    FilterChip(
                        selected = selectedMaterial == null,
                        onClick = { onMaterialSelected(null) },
                        label = { Text("All Materials") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(materials) { material ->
                    FilterChip(
                        selected = selectedMaterial == material,
                        onClick = { onMaterialSelected(material) },
                        label = { Text(material) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
