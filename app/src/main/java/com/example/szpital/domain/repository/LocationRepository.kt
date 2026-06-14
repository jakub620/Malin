package com.example.szpital.domain.repository

import com.example.szpital.common.ResultState
import com.example.szpital.domain.model.PatientLocation

// Interfejs repozytorium
interface LocationRepository {
    suspend fun getLocationByQrCode(qrText: String): ResultState<PatientLocation>
}
