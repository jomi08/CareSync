package com.example.caresync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PatientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PatientRepository
    private val doctorId: Int
    val allPatients: LiveData<List<Patient>>

    init {
        val dao = CareSyncDatabase.getDatabase(application).patientDao()
        repository = PatientRepository(dao)

        val prefs = application.getSharedPreferences("caresync_prefs", Application.MODE_PRIVATE)
        doctorId = prefs.getInt("logged_in_id", 0)

        allPatients = repository.getAllPatients(doctorId)
    }

    fun insert(patient: Patient) = viewModelScope.launch {
        repository.insert(patient.copy(doctorId = doctorId))
    }

    fun delete(patient: Patient) = viewModelScope.launch {
        repository.delete(patient)
    }

    fun update(patient: Patient) = viewModelScope.launch {
        repository.update(patient)
    }

    fun searchPatients(query: String): LiveData<List<Patient>> {
        return repository.searchPatients(doctorId, query)
    }

    // ✅ NEW — age group filter
    fun getPatientsByAge(minAge: Int, maxAge: Int): LiveData<List<Patient>> {
        return repository.getPatientsByAgeRange(doctorId, minAge, maxAge)
    }
}