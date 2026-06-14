package com.example.szpital.data.api

import retrofit2.http.GET
import retrofit2.http.Query

// API dla serwera CENAGIS
interface ArcGisApiService {

    @GET("SION2_Geoopisy/sion_topo_qrcode/MapServer/0/query")
    suspend fun getLocationByQrCode(
        @Query("where")      where: String,          // np. "qr_text='iLCTL'"
        @Query("outFields")  outFields: String = "*",
        @Query("f")          format: String   = "pjson"
    ): ArcGisResponse
}
