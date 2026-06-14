package com.example.szpital.ui.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szpital.data.api.CAMERA_LOCATIONS
import com.example.szpital.data.api.CameraLocation
import com.example.szpital.data.api.HospitalRetrofitClient
import com.example.szpital.data.api.LocationUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanResult(
    val tagId:   String,
    val success: Boolean,
    val message: String
)

class CameraWatcherViewModel : ViewModel() {

    private val api = HospitalRetrofitClient.apiService

    // Wybrna lokalizacja kamery (operator wybiera raz przed startem)
    var selectedLocation: CameraLocation by mutableStateOf(CAMERA_LOCATIONS.first())

    // ID urządzenia (opcjonalne, identyfikuje kamerę w logach)
    var cameraId by mutableStateOf("kamera_1")

    val cooldownMs = 4000L  // 4 sekundy przerwy między skanowaniami

    private val _lastResult  = MutableStateFlow<ScanResult?>(null)
    val lastResult: StateFlow<ScanResult?> = _lastResult.asStateFlow()

    private val _scanCount = MutableStateFlow(0)
    val scanCount: StateFlow<Int> = _scanCount.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Cache – unikamy duplikatów w oknie cooldown
    private val recentTags = mutableMapOf<String, Long>()

    fun startScanning()  { _isScanning.value = true }
    fun stopScanning()   { _isScanning.value = false }

    fun handleScan(rawText: String) {
        if (!_isScanning.value) return

        // Wyciągnij ID z URL lub użyj tekstu bezpośrednio
        val tagId = if (rawText.contains("/")) rawText.substringAfterLast("/") else rawText

        // Sprawdź cooldown per-tag
        val now = System.currentTimeMillis()
        val lastSent = recentTags[tagId] ?: 0L
        if (now - lastSent < cooldownMs) return
        recentTags[tagId] = now

        val loc = selectedLocation

        viewModelScope.launch {
            try {
                val resp = api.updateLocation(
                    LocationUpdateRequest(
                        tagId         = tagId,
                        floor         = loc.floor,
                        locationLabel = loc.label,
                        cameraId      = cameraId.ifBlank { null },
                        lat           = loc.lat,
                        lng           = loc.lng
                    )
                )
                _scanCount.value++
                _lastResult.value = ScanResult(
                    tagId   = tagId,
                    success = true,
                    message = if (resp.patientId != null)
                        "Pacjent ID ${resp.patientId} → ${loc.label}"
                    else
                        "Tag bez przypisanego pacjenta – zeskanowano"
                )
            } catch (e: Exception) {
                _lastResult.value = ScanResult(
                    tagId   = tagId,
                    success = false,
                    message = "Błąd: ${e.message}"
                )
            }
        }
    }
}
