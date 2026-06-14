package com.example.szpital.domain.model

// Model lokalizacji pacjenta
// Wspolrzedne GPS
data class PatientLocation(
    val qrText: String,       // tekst z kodu QR (np. "iLCTL")
    val latitude: Double,     // WGS84 – szerokość geograficzna
    val longitude: Double,    // WGS84 – długość geograficzna
    val buildingId: Int? = null,  // nr budynku z API
    val floor: Int? = null,       // piętro (poziom) z API
    val label: String? = null
)
