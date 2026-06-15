package com.example.szpital

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.szpital.data.api.PatientResponse
import com.example.szpital.domain.model.PatientLocation
import com.example.szpital.ui.camera.CameraWatcherScreen
import com.example.szpital.ui.map.MapScreen
import com.example.szpital.ui.patients.PatientListScreen

sealed class Screen {
    object PatientList : Screen()
    data class Map(val location: PatientLocation) : Screen()
    object CameraWatcher : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.PatientList) }

    when (val screen = currentScreen) {

        is Screen.PatientList -> PatientListScreen(
            onPatientClick = { patient ->
                val loc = patient.location
                val tagId = patient.tagId
                val localEntry = tagId?.let { com.example.szpital.data.local.LocalQrDatabase.findByQrText(it) }

                val lat: Double
                val lng: Double
                val floor: Int
                val label: String

                if (loc != null) {
                    lat = loc.lat ?: 52.22070
                    lng = loc.lng ?: 21.01018
                    floor = loc.floor ?: 1
                    label = "${patient.name} – ${loc.locationLabel ?: "Brak lokalizacji"}"
                } else if (localEntry != null) {
                    val coords = com.example.szpital.data.local.LocalQrDatabase.convertToWgs84(localEntry.x, localEntry.y)
                    lat = coords.first
                    lng = coords.second
                    floor = localEntry.floor
                    label = "${patient.name} – ${localEntry.qrText} (Gmach Główny)"
                } else {
                    lat = 52.22070
                    lng = 21.01018
                    floor = 1
                    label = "${patient.name} – Brak lokalizacji"
                }

                currentScreen = Screen.Map(
                    PatientLocation(
                        qrText     = tagId ?: patient.name,
                        latitude   = lat,
                        longitude  = lng,
                        buildingId = localEntry?.buildingId,
                        floor      = floor,
                        label      = label
                    )
                )
            },
            onCameraMode = { currentScreen = Screen.CameraWatcher }
        )

        // Mapa
        is Screen.Map -> MapScreen(
            location = screen.location,
            onBack   = { currentScreen = Screen.PatientList }
        )

        // Tryb kamery
        is Screen.CameraWatcher -> CameraWatcherScreen(
            onBack = { currentScreen = Screen.PatientList }
        )
    }
}