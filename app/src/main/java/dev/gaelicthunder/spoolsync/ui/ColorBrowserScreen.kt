package dev.gaelicthunder.spoolsync.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorBrowserScreen(
    viewModel: SpoolSyncViewModel,
    onBack: () -> Unit
) {
    val swatches by viewModel.filamentColorsSwatches.collectAsState()
    val hasMoreColors by viewModel.hasMoreColors.collectAsState()
    var selectedSwatch by remember { mutableStateOf<FilamentColorResult?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resetColorBrowser()
        viewModel.loadNextColorPage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Browser") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (swatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading color swatches...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(swatches, key = { "${it.brand}_${it.name}_${it.hexColor}" }) { swatch ->
                        SwatchCard(
                            swatch = swatch,
                            onClick = { selectedSwatch = swatch }
                        )
                    }

                    if (hasMoreColors) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { viewModel.loadNextColorPage() },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load More Colors")
                                }
                            }
                        }
                    } else {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "All colors loaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSwatch?.let { swatch ->
        SwatchDetailDialog(
            swatch = swatch,
            onDismiss = { selectedSwatch = null },
            onOpenAmazon = {
                swatch.amazonLink?.let { link ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    context.startActivity(intent)
                }
            }
        )
    }
}

@Composable
fun SwatchCard(
    swatch: FilamentColorResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (swatch.imageFront != null) {
                AsyncImage(
                    model = swatch.imageFront,
                    contentDescription = swatch.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = try {
                        Color(android.graphics.Color.parseColor(swatch.hexColor))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                ) {}
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = swatch.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = swatch.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = swatch.material,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SwatchDetailDialog(
    swatch: FilamentColorResult,
    onDismiss: () -> Unit,
    onOpenAmazon: () -> Unit
) {
    val triadicColors = remember(swatch.hexColor) { calculateTriadicColors(swatch.hexColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = swatch.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (swatch.imageFront != null) {
                    AsyncImage(
                        model = swatch.imageFront,
                        contentDescription = "${swatch.name} front image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Brand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = swatch.brand,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = try {
                            Color(android.graphics.Color.parseColor(swatch.hexColor))
                        } catch (e: Exception) {
                            Color.Gray
                        },
                        shape = CircleShape,
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Material",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = swatch.material,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hex Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = swatch.hexColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Triadic Color Harmony",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    triadicColors.forEach { color ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = color,
                                shape = CircleShape,
                                tonalElevation = 2.dp,
                                shadowElevation = 1.dp
                            ) {}
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = colorToHex(color),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (swatch.amazonLink != null) {
                Button(onClick = onOpenAmazon) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Amazon")
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

fun calculateTriadicColors(hexColor: String): List<Color> {
    return try {
        val color = android.graphics.Color.parseColor(hexColor)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        
        val baseColor = Color(color)
        val triadic1 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hsv[0] + 120f) % 360f, hsv[1], hsv[2])))
        val triadic2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hsv[0] + 240f) % 360f, hsv[1], hsv[2])))
        
        listOf(baseColor, triadic1, triadic2)
    } catch (e: Exception) {
        listOf(Color.Gray, Color.Gray, Color.Gray)
    }
}

fun colorToHex(color: Color): String {
    val red = (color.red * 255).roundToInt()
    val green = (color.green * 255).roundToInt()
    val blue = (color.blue * 255).roundToInt()
    return "#%02X%02X%02X".format(red, green, blue)
}
