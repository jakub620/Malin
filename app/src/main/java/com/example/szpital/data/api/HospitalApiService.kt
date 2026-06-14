package com.example.szpital.data.api

import retrofit2.http.*

// API dla backendu szpitalnego
interface HospitalApiService {

    // Pacjenci
    @GET("patients")
    suspend fun getPatients(): List<PatientResponse>

    @GET("patients/{id}")
    suspend fun getPatient(@Path("id") id: Int): PatientResponse

    @POST("patients")
    suspend fun createPatient(@Body data: CreatePatientRequest): PatientResponse

    @PUT("patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: Int,
        @Body data: CreatePatientRequest
    ): PatientResponse

    @DELETE("patients/{id}")
    suspend fun deletePatient(@Path("id") id: Int)

    // Tagi
    @GET("tags")
    suspend fun getTags(): List<TagResponse>

    @GET("tags/free")
    suspend fun getFreeTags(): List<TagResponse>

    @POST("patients/{id}/assign-tag")
    suspend fun assignTag(
        @Path("id") patientId: Int,
        @Body req: AssignTagRequest
    ): PatientResponse

    @POST("patients/{id}/unassign-tag")
    suspend fun unassignTag(@Path("id") patientId: Int): PatientResponse

    // Lokalizacja (kamera)
    @POST("location/update")
    suspend fun updateLocation(@Body req: LocationUpdateRequest): LocationUpdateResponse
}
