package com.example.szpital.data.api

/**
 * Predefiniowane lokalizacje kamer w Gmachu Głównym PW.
 * Każda lokalizacja ma przypisane współrzędne WGS84 oraz piętro.
 * Operator wybiera tę lokalizację raz przy uruchomieniu ekranu kamery.
data class CameraLocation(
    val label: String,     // wyświetlana nazwa
    val floor: Int,        // numer piętra (0 = parter)
    val lat:   Double,     // szerokość geograficzna WGS84
    val lng:   Double      // długość geograficzna WGS84
)

val CAMERA_LOCATIONS = listOf(
    CameraLocation("Parter – Wejście główne (od ul. Pl. Politechniki)",    0,  52.22062, 21.01007),
    CameraLocation("Parter – Hol główny (korytarz centralny)",             0,  52.22075, 21.01025),
    CameraLocation("Parter – Wejście boczne (skrzydło wschodnie)",         0,  52.22080, 21.01055),
    CameraLocation("Piętro 1 – Klatka schodowa A (środkowa)",             1,  52.22070, 21.01018),
    CameraLocation("Piętro 1 – Klatka schodowa B (wschodnia)",            1,  52.22073, 21.01050),
    CameraLocation("Piętro 1 – Korytarz zachodni",                        1,  52.22068, 21.00995),
    CameraLocation("Piętro 2 – Klatka schodowa A (środkowa)",             2,  52.22070, 21.01018),
    CameraLocation("Piętro 2 – Klatka schodowa B (wschodnia)",            2,  52.22073, 21.01050),
    CameraLocation("Piętro 2 – Korytarz zachodni",                        2,  52.22068, 21.00995),
    CameraLocation("Piętro 3 – Klatka schodowa A (środkowa)",             3,  52.22070, 21.01018),
    CameraLocation("Piętro 3 – Korytarz wschodni",                        3,  52.22076, 21.01048),
    CameraLocation("Piętro 4 – Klatka schodowa A (środkowa)",             4,  52.22070, 21.01018),
    CameraLocation("Piętro 4 – Korytarz zachodni",                        4,  52.22068, 21.00995),
    CameraLocation("Piętro 5 – Klatka schodowa A (środkowa)",             5,  52.22070, 21.01018),
    CameraLocation("Piętro 6 – Klatka schodowa A (środkowa)",             6,  52.22070, 21.01018),
)
