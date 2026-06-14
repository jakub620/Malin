package com.example.szpital.ui

// Model danych jednego pacjenta
data class Patient(
    val id: String,           // = zawartość kodu QR na piersi pacjenta
    val name: String,
    val room: String,
    val ward: String,
    val latitude: Double,     // współrzędne GPS (teren szpitala)
    val longitude: Double,
    val condition: String = "stabilny"  // dobry / stabilny / krytyczny
)

// Prosta baza danych pacjentów (w produkcji: API szpitalny)
object PatientDatabase {

    // Centrum Szpitala Klinicznego w Warszawie (ul. Banacha 1a)
    private const val BASE_LAT = 52.2046
    private const val BASE_LNG = 20.9893

    val patients = listOf(
        Patient(
            id        = "PAC001",
            name      = "Jan Kowalski",
            room      = "Sala 101",
            ward      = "Kardiologia",
            latitude  = BASE_LAT + 0.0002,
            longitude = BASE_LNG - 0.0003,
            condition = "dobry"
        ),
        Patient(
            id        = "PAC002",
            name      = "Maria Nowak",
            room      = "Sala 203",
            ward      = "Neurologia",
            latitude  = BASE_LAT + 0.0005,
            longitude = BASE_LNG + 0.0002,
            condition = "stabilny"
        ),
        Patient(
            id        = "PAC003",
            name      = "Piotr Wiśniewski",
            room      = "Sala 105",
            ward      = "Chirurgia",
            latitude  = BASE_LAT - 0.0003,
            longitude = BASE_LNG + 0.0004,
            condition = "stabilny"
        ),
        Patient(
            id        = "PAC004",
            name      = "Anna Kowalczyk",
            room      = "Sala 312",
            ward      = "Ortopedia",
            latitude  = BASE_LAT + 0.0007,
            longitude = BASE_LNG - 0.0001,
            condition = "dobry"
        ),
        Patient(
            id        = "PAC005",
            name      = "Tomasz Lewandowski",
            room      = "OIOM 2",
            ward      = "Intensywna Opieka Medyczna",
            latitude  = BASE_LAT - 0.0001,
            longitude = BASE_LNG - 0.0005,
            condition = "krytyczny"
        )
    )

    fun findById(id: String): Patient? =
        patients.find { it.id.equals(id.trim(), ignoreCase = true) }
}
