package com.example.szpital.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szpital.data.api.AssignTagRequest
import com.example.szpital.data.api.CreatePatientRequest
import com.example.szpital.data.api.HospitalRetrofitClient
import com.example.szpital.data.api.PatientResponse
import com.example.szpital.data.api.TagResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PatientsUiState {
    object Loading : PatientsUiState()
    data class Success(val patients: List<PatientResponse>) : PatientsUiState()
    data class Error(val message: String) : PatientsUiState()
}

class PatientsViewModel : ViewModel() {

    private val api = HospitalRetrofitClient.apiService

    private val _uiState = MutableStateFlow<PatientsUiState>(PatientsUiState.Loading)
    val uiState: StateFlow<PatientsUiState> = _uiState.asStateFlow()

    private val _freeTags = MutableStateFlow<List<TagResponse>>(emptyList())
    val freeTags: StateFlow<List<TagResponse>> = _freeTags.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { loadPatients() }

    fun loadPatients() {
        viewModelScope.launch {
            _uiState.value = PatientsUiState.Loading
            try {
                _uiState.value = PatientsUiState.Success(api.getPatients())
            } catch (e: Exception) {
                _uiState.value = PatientsUiState.Error(
                    "Błąd połączenia z serwerem: ${e.message}"
                )
            }
        }
    }

    fun loadFreeTags() {
        viewModelScope.launch {
            try {
                _freeTags.value = api.getFreeTags()
            } catch (_: Exception) {}
        }
    }

    fun createPatient(name: String, age: Int?, ward: String?, notes: String?) {
        viewModelScope.launch {
            try {
                api.createPatient(CreatePatientRequest(name, age, ward, notes))
                loadPatients()
            } catch (e: Exception) {
                _actionError.value = "Błąd dodawania: ${e.message}"
            }
        }
    }

    fun deletePatient(id: Int) {
        viewModelScope.launch {
            try {
                api.deletePatient(id)
                loadPatients()
            } catch (e: Exception) {
                _actionError.value = "Błąd usuwania: ${e.message}"
            }
        }
    }

    fun assignTag(patientId: Int, tagId: String) {
        viewModelScope.launch {
            try {
                api.assignTag(patientId, AssignTagRequest(tagId))
                loadPatients()
                loadFreeTags()
            } catch (e: Exception) {
                _actionError.value = "Błąd przypisania tagu: ${e.message}"
            }
        }
    }

    fun unassignTag(patientId: Int) {
        viewModelScope.launch {
            try {
                api.unassignTag(patientId)
                loadPatients()
                loadFreeTags()
            } catch (e: Exception) {
                _actionError.value = "Błąd odpinania tagu: ${e.message}"
            }
        }
    }

    fun clearError() { _actionError.value = null }
}
