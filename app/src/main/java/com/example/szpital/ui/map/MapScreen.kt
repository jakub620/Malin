package com.example.szpital.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.szpital.domain.model.PatientLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    location: PatientLocation,
    onBack: () -> Unit
) {
    var selectedFloor by remember { mutableStateOf(location.floor ?: 0) }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "📍 ${location.label}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    val floorText = if (location.floor != null && location.floor > 0)
                        "Piętro ${location.floor}" else "Parter"
                    Text(
                        text  = "Tag: ${location.qrText}  •  $floorText",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor             = MaterialTheme.colorScheme.primary,
                titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier          = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(location.label ?: location.qrText, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                    val floorLabel = when {
                        location.floor == null || location.floor == 0 -> "Parter"
                        else -> "Piętro ${location.floor}"
                    }
                    Text("$floorLabel  •  Tag: ${location.qrText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("%.5f, %.5f".format(location.latitude, location.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "🏢 Wybierz piętro gmachu:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    val floors = (0..6).toList()
                    items(floors) { f ->
                        val isSelected = selectedFloor == f
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFloor = f },
                            label = { Text(if (f == 0) "Parter" else "Piętro $f") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Wskazówka o zoomie
        Text(
            text     = "📡 Korytarze i pokoje załadowane z serwera CENAGIS  •  Użyj pinch zoomu",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )

        OsmMapView(
            modifier  = Modifier.weight(1f),
            location  = location,
            selectedFloor = selectedFloor
        )
    }
}

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    location: PatientLocation,
    selectedFloor: Int
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue    = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory  = { mapView },
        modifier = modifier,
        update   = { mv ->
            mv.overlays.clear()

            // Podklad mapowy z CENAGIS
            val arcGisTileSource = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                "ArcGisIndoor_$selectedFloor",
                0, 22, 256, ".png",
                arrayOf("https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_topo_indoor_all/MapServer/export")
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)

                    val totalSize = 20037508.34 * 2
                    val numTiles = 1 shl zoom
                    val tileSize = totalSize / numTiles

                    val minX = -20037508.34 + x * tileSize
                    val maxX = minX + tileSize

                    val maxY = 20037508.34 - y * tileSize
                    val minY = maxY - tileSize

                    val bbox = "$minX,$minY,$maxX,$maxY"

                    // Filtrowanie po wybranym pietrze
                    val layerDefs = """{"0":"poziom=$selectedFloor","1":"poziom=$selectedFloor","2":"poziom=$selectedFloor","3":"poziom=$selectedFloor","4":"poziom=$selectedFloor","5":"poziom=$selectedFloor"}"""
                    val encodedLayerDefs = java.net.URLEncoder.encode(layerDefs, "UTF-8")

                    return baseUrl + "?bbox=$bbox&bboxSR=3857&imageSR=3857&size=256,256&format=png&transparent=true&f=image&layers=show:0,1,2,3,4,5&layerDefs=$encodedLayerDefs"
                }
            }

            val tileProvider = org.osmdroid.tileprovider.MapTileProviderBasic(context, arcGisTileSource)
            val tilesOverlay = org.osmdroid.views.overlay.TilesOverlay(tileProvider, context).apply {
                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                loadingLineColor = android.graphics.Color.TRANSPARENT
            }
            mv.overlays.add(tilesOverlay)

            // Dodanie markera pacjenta
            val patientOnThisFloor = (location.floor ?: 0) == selectedFloor

            if (patientOnThisFloor) {
                Marker(mv).apply {
                    position = GeoPoint(location.latitude, location.longitude)
                    title    = "👤 PACJENT: ${location.label}"
                    snippet  = "Piętro $selectedFloor"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mv.overlays.add(this)
                }
            }

            // Centrowanie mapy
            val centerPoint = if (patientOnThisFloor) {
                GeoPoint(location.latitude, location.longitude)
            } else {
                GeoPoint(52.22075, 21.01025) // Środek gmachu
            }
            mv.controller.setZoom(19.5)
            mv.controller.animateTo(centerPoint)
            mv.invalidate()
        }
    )
}
