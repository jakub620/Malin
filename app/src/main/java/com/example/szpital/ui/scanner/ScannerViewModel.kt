package com.example.szpital.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szpital.common.ResultState
import com.example.szpital.data.api.RetrofitClient
import com.example.szpital.data.repository.LocationRepositoryImpl
import com.example.szpital.domain.model.PatientLocation
import com.example.szpital.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    // Inicjalizacja repozytorium
    private val repository: LocationRepository = LocationRepositoryImpl(
        RetrofitClient.apiService
    )

    private val _locationState = MutableStateFlow<ResultState<PatientLocation>?>(null)
    val locationState: StateFlow<ResultState<PatientLocation>?> = _locationState.asStateFlow()

    // Wywołaj API z kodem QR odczytanym ze skanera
    fun lookupLocation(rawQrText: String) {
        if (rawQrText.isBlank()) return
        
        // Wyciągamy ID jeśli zeskanowano pełny URL (np. "https://qrcode.pw.edu.pl/iLCTL")
        val extractedId = if (rawQrText.contains("/")) {
            rawQrText.substringAfterLast("/")
        } else {
            rawQrText
        }

        viewModelScope.launch {
            _locationState.value = ResultState.Loading
            _locationState.value = repository.getLocationByQrCode(extractedId)
        }
    }

    fun reset() {
        _locationState.value = null
    }
}
