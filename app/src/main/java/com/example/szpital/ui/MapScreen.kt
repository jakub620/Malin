package com.example.szpital.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    patientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val patient = remember(patientId) { PatientDatabase.findById(patientId) }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(
                    text = patient?.name ?: "Nieznany pacjent",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Wróć"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor     = MaterialTheme.colorScheme.primary,
                titleContentColor  = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        if (patient != null) {
            PatientInfoCard(patient = patient)
        } else {
            UnknownPatientBanner(patientId = patientId)
        }

        OsmMapView(
            modifier = Modifier.weight(1f),
            patient  = patient
        )
    }
}

@Composable
fun PatientInfoCard(patient: Patient) {
    val conditionColor = when (patient.condition) {
        "krytyczny" -> Color(0xFFD32F2F)
        "dobry"     -> Color(0xFF388E3C)
        else        -> Color(0xFFF57C00)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier            = Modifier.padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "${patient.ward}  •  ${patient.room}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "ID: ${patient.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Badge stanu pacjenta
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = conditionColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text     = patient.condition.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color    = conditionColor
                )
            }
        }
    }
}

@Composable
fun UnknownPatientBanner(patientId: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = "Pacjent \"$patientId\" nie znaleziony w systemie",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    patient: Patient?
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Tworzymy MapView raz i zapamiętujemy go
    val mapView = remember {
        // Konfiguracja OSMDroid przed stworzeniem MapView
        Configuration.getInstance().apply {
            userAgentValue    = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
            // Domyślne centrum: Szpital Kliniczny Warszawa
            controller.setCenter(GeoPoint(52.2046, 20.9893))
        }
    }

    // DisposableEffect: zarządzanie cyklem życia MapView
    
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                else                       -> { /* ignoruj pozostałe zdarzenia */ }
            }
        }
        lifecycle.addObserver(observer)

        // Sprzątanie gdy composable opuszcza drzewo
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // AndroidView: osadzenie tradycyjnego View w Compose
    
    AndroidView(
        factory  = { mapView },
        modifier = modifier,
        update   = { mv ->
            // Wywoływane za każdym razem gdy zmienia się pacjent
            mv.overlays.clear()

            patient?.let { p ->
                val geoPoint = GeoPoint(p.latitude, p.longitude)

                // Marker na pozycji pacjenta
                val marker = Marker(mv).apply {
                    position = geoPoint
                    title    = p.name
                    snippet  = "${p.ward} • ${p.room} • Stan: ${p.condition}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                mv.overlays.add(marker)

                // Animowane przesunięcie mapy do pacjenta
                mv.controller.animateTo(geoPoint)
                mv.controller.setZoom(18.5)
            }

            mv.invalidate()
        }
    )
}
