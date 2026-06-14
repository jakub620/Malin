package com.example.szpital.ui.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.szpital.data.api.CAMERA_LOCATIONS
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraWatcherScreen(
    onBack: () -> Unit,
    vm: CameraWatcherViewModel = viewModel()
) {
    val lastResult  by vm.lastResult.collectAsState()
    val scanCount   by vm.scanCount.collectAsState()
    val isScanning  by vm.isScanning.collectAsState()

    var locationDropdownExpanded by remember { mutableStateOf(false) }

    // ZXing launcher – uruchamiamy skanowanie w pętli
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents
        if (text != null) {
            vm.handleScan(text)
        } else {
            onBack()
        }
    }

    // Automatycznie uruchom kolejne skanowanie po cooldown
    LaunchedEffect(lastResult) {
        if (isScanning) {
            delay(vm.cooldownMs)
            scanLauncher.launch(buildScanOptions())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📷 Tryb Kamery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.stopScanning(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {

            // Ikona statusu
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                modifier    = Modifier.size(72.dp),
                tint        = if (isScanning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
            )
            Text(
                text  = if (isScanning) "Skanowanie aktywne" else "Skanowanie zatrzymane",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isScanning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
            )

            // Wybór lokalizacji kamery
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(
                        "📍 Lokalizacja tej kamery",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Wybierz gdzie stoi ten telefon / kamera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dropdown z listą predefiniowanych miejsc
                    ExposedDropdownMenuBox(
                        expanded  = locationDropdownExpanded,
                        onExpandedChange = { locationDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = vm.selectedLocation.label,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Lokalizacja") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(locationDropdownExpanded) },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded         = locationDropdownExpanded,
                            onDismissRequest = { locationDropdownExpanded = false }
                        ) {
                            CAMERA_LOCATIONS.forEach { loc ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(loc.label, fontWeight = FontWeight.Medium)
                                            Text(
                                                "Piętro ${loc.floor}  •  ${loc.lat.format(5)}, ${loc.lng.format(5)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        vm.selectedLocation = loc
                                        locationDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, null,
                                            tint = if (vm.selectedLocation == loc)
                                                Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline)
                                    }
                                )
                            }
                        }
                    }

                    // ID kamery (opcjonalne)
                    OutlinedTextField(
                        value         = vm.cameraId,
                        onValueChange = { vm.cameraId = it },
                        label         = { Text("ID kamery (opcjonalne)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }

            // Info o wybranej lokalizacji
            val loc = vm.selectedLocation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = Color(0xFF2E7D32))
                    Column {
                        Text(loc.label, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B5E20))
                        Text("Piętro: ${loc.floor}  •  lat ${loc.lat.format(5)}, lng ${loc.lng.format(5)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF388E3C))
                    }
                }
            }

            // Opis działania
            Text(
                text      = "Skieruj kamerę na opaskę QR pacjenta.\n" +
                            "Lokalizacja zostanie zaktualizowana automatycznie.",
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Ostatni wynik
            lastResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (result.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.success) Icons.Default.CheckCircle
                                          else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(result.tagId, fontWeight = FontWeight.Bold)
                            Text(result.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (result.success) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                    }
                }
            }

            // Licznik
            if (scanCount > 0) {
                Text(
                    "Wysłano $scanCount aktualizacji lokalizacji",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Start / Stop
            if (!isScanning) {
                Button(
                    onClick  = {
                        vm.startScanning()
                        scanLauncher.launch(buildScanOptions())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Camera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Uruchom skanowanie", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick  = { vm.stopScanning() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text("Zatrzymaj skanowanie")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)

private fun buildScanOptions() = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt("Skieruj na opaskę QR pacjenta")
    setBeepEnabled(true)
    setOrientationLocked(false)
    setCameraId(0)
}
