package com.example.szpital.data.api

import com.google.gson.annotations.SerializedName


data class PatientResponse(
    @SerializedName("id")         val id: Int,
    @SerializedName("name")       val name: String,
    @SerializedName("age")        val age: Int?        = null,
    @SerializedName("ward")       val ward: String?    = null,
    @SerializedName("notes")      val notes: String?   = null,
    @SerializedName("tag_id")     val tagId: String?   = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("location")   val location: LocationResponse? = null
)

data class LocationResponse(
    @SerializedName("floor")          val floor: Int?    = null,
    @SerializedName("location_label") val locationLabel: String? = null,
    @SerializedName("camera_id")      val cameraId: String? = null,
    @SerializedName("timestamp")      val timestamp: String? = null,
    @SerializedName("lat")            val lat: Double? = null,
    @SerializedName("lng")            val lng: Double? = null
)

data class TagResponse(
    @SerializedName("id")          val id: String,
    @SerializedName("patient_id")  val patientId: Int?    = null,
    @SerializedName("assigned_at") val assignedAt: String? = null,
    @SerializedName("is_free")     val isFree: Boolean
)

data class CreatePatientRequest(
    @SerializedName("name")  val name: String,
    @SerializedName("age")   val age: Int?    = null,
    @SerializedName("ward")  val ward: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AssignTagRequest(
    @SerializedName("tag_id") val tagId: String
)

data class LocationUpdateRequest(
    @SerializedName("tag_id")         val tagId: String,
    @SerializedName("floor")          val floor: Int?    = null,
    @SerializedName("location_label") val locationLabel: String? = null,
    @SerializedName("camera_id")      val cameraId: String? = null,
    @SerializedName("lat")            val lat: Double?   = null,
    @SerializedName("lng")            val lng: Double?   = null
)

data class LocationUpdateResponse(
    @SerializedName("ok")         val ok: Boolean,
    @SerializedName("patient_id") val patientId: Int?
)
