package com.example.szpital.data.repository

import com.example.szpital.common.ResultState
import com.example.szpital.data.api.ArcGisApiService
import com.example.szpital.data.local.LocalQrDatabase
import com.example.szpital.domain.model.PatientLocation
import com.example.szpital.domain.repository.LocationRepository
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

class LocationRepositoryImpl(
    private val apiService: ArcGisApiService
) : LocationRepository {

    private val crsFactory = CRSFactory()

    private val epsg2180 = crsFactory.createFromParameters(
        "EPSG:2180",
        "+proj=tmerc +lat_0=0 +lon_0=19 +k=0.9993 " +
        "+x_0=500000 +y_0=-5300000 +ellps=GRS80 " +
        "+towgs84=0,0,0,0,0,0,0 +units=m +no_defs"
    )

    private val wgs84 = crsFactory.createFromParameters(
        "EPSG:4326",
        "+proj=longlat +datum=WGS84 +no_defs"
    )

    private val transformFactory = CoordinateTransformFactory()

    fun convertToWgs84(x: Double, y: Double): Pair<Double, Double> {
        val transform   = transformFactory.createTransform(epsg2180, wgs84)
        val src         = ProjCoordinate(x, y)
        val dst         = ProjCoordinate()
        transform.transform(src, dst)
        return Pair(dst.y, dst.x)  // lat, lng
    }

    override suspend fun getLocationByQrCode(
        qrText: String
    ): ResultState<PatientLocation> {

        // Pobieranie danych z API
        try {
            val response = apiService.getLocationByQrCode(
                where = "qr_text='${qrText.trim()}'"
            )

            if (response.error == null) {
                val feature = response.features?.firstOrNull()
                val geometry = feature?.geometry

                if (feature != null && geometry != null) {
                    val (lat, lng) = convertToWgs84(geometry.x, geometry.y)
                    return ResultState.Success(
                        PatientLocation(
                            qrText     = qrText,
                            latitude   = lat,
                            longitude  = lng,
                            buildingId = feature.attributes?.buildingId,
                            floor      = feature.attributes?.poziom,
                            label      = qrText
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Blad API - przejscie do bazy offline
        }

        // Odczyt z bazy lokalnej
        val local = LocalQrDatabase.findByQrText(qrText)
        if (local != null) {
            val (lat, lng) = convertToWgs84(local.x, local.y)
            return ResultState.Success(
                PatientLocation(
                    qrText     = qrText,
                    latitude   = lat,
                    longitude  = lng,
                    buildingId = local.buildingId,
                    floor      = local.floor,
                    label      = "$qrText [offline]"
                )
            )
        }

        return ResultState.Error(
            Exception("Nie znaleziono kodu QR: \"$qrText\" (sprawdzono API i lokalną bazę)")
        )
    }

    fun getAllForFloor(floor: Int): List<PatientLocation> {
        return LocalQrDatabase.entries
            .filter { it.floor == floor }
            .map { entry ->
                val (lat, lng) = convertToWgs84(entry.x, entry.y)
                PatientLocation(
                    qrText     = entry.qrText,
                    latitude   = lat,
                    longitude  = lng,
                    buildingId = entry.buildingId,
                    floor      = entry.floor,
                    label      = entry.qrText
                )
            }
    }

    fun getAll(): List<PatientLocation> {
        return LocalQrDatabase.entries.map { entry ->
            val (lat, lng) = convertToWgs84(entry.x, entry.y)
            PatientLocation(
                qrText     = entry.qrText,
                latitude   = lat,
                longitude  = lng,
                buildingId = entry.buildingId,
                floor      = entry.floor,
                label      = entry.qrText
            )
        }
    }
}
