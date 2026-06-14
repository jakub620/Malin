package com.example.szpital.data.api

import com.google.gson.annotations.SerializedName

// Przykład: GET .../query?where=qr_text='iLCTL'&outFields=*&f=pjson
// Zwraca geometrię w EPSG:2180 (PUWG 1992 / CS92)

data class ArcGisResponse(
    @SerializedName("features")         val features: List<Feature>?      = null,
    @SerializedName("error")            val error: ArcGisError?            = null,
    @SerializedName("geometryType")     val geometryType: String?          = null,
    @SerializedName("spatialReference") val spatialReference: SpatialRef?  = null
)

data class Feature(
    @SerializedName("attributes") val attributes: Attributes?,
    @SerializedName("geometry")   val geometry: Geometry?
)

// Atrybuty z bazy ArcGIS (SION2_Geoopisy / sion_topo_qrcode)
data class Attributes(
    @SerializedName("id")          val id: Int?,
    @SerializedName("qr_id")      val qrId: Int?,
    @SerializedName("qr_text")    val qrText: String?,
    @SerializedName("building_id") val buildingId: Int?,   // nr budynku PW
    @SerializedName("poziom")     val poziom: Int?,        // piętro
    @SerializedName("floor_id")   val floorId: Int?
)

// Geometry w EPSG:2180 (współrzędne w metrach)
data class Geometry(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double
)

data class SpatialRef(
    @SerializedName("wkid")       val wkid: Int?    = null,
    @SerializedName("latestWkid") val latestWkid: Int? = null,
    @SerializedName("wkt")        val wkt: String?  = null
)

data class ArcGisError(
    @SerializedName("code")    val code: Int,
    @SerializedName("message") val message: String
)
