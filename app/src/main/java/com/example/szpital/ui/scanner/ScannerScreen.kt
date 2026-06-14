package com.example.szpital.ui.scanner

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.szpital.common.ResultState
import com.example.szpital.domain.model.PatientLocation
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onLocationFound: (PatientLocation) -> Unit
) {
    val viewModel: ScannerViewModel = viewModel()
    val locationState by viewModel.locationState.collectAsState()
    var manualInput by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val qrText = result.contents
        if (qrText != null) {
            viewModel.lookupLocation(qrText)
        }
    }

    LaunchedEffect(locationState) {
        if (locationState is ResultState.Success) {
            val location = (locationState as ResultState.Success<PatientLocation>).data
            onLocationFound(location)
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🏥 MALIN – Skaner QR",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape   = RoundedCornerShape(16.dp),
                onClick = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Skieruj aparat na kod QR pacjenta")
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        }
                    )
                }
            ) {
                Icon(
                    imageVector        = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text     = "Skanuj kod QR aparatem",
                    style    = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()
            Text(
                text  = "lub wpisz kod ręcznie",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value         = manualInput,
                onValueChange = { manualInput = it },
                label         = { Text("Kod QR (np. iLCTL)") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            Button(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                enabled   = manualInput.isNotBlank() && locationState !is ResultState.Loading,
                onClick   = { viewModel.lookupLocation(manualInput.trim()) }
            ) {
                Text("Szukaj lokalizacji")
            }

            when (val state = locationState) {
                is ResultState.Loading -> {
                    CircularProgressIndicator()
                    Text(
                        text  = "Pobieranie lokalizacji z API ArcGIS…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                is ResultState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = "❌ ${state.throwable.message}",
                            modifier = Modifier.padding(16.dp),
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> { /* idle lub success (handled by LaunchedEffect) */ }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "Jak to działa?",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "1. Skanuj kod QR z opaski/karteczki pacjenta\n" +
                                "2. App wywołuje API ArcGIS (EPSG:2180)\n" +
                                "3. proj4j konwertuje do WGS84\n" +
                                "4. Mapa OpenStreetMap pokazuje lokalizację",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
