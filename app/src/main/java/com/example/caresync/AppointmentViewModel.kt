package com.example.caresync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class AppointmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppointmentRepository
    val allAppointments: LiveData<List<Appointment>>
    val upcomingAppointments: LiveData<List<Appointment>>
    val upcomingCount: LiveData<Int>

    init {
        val dao = CareSyncDatabase.getDatabase(application).appointmentDao()
        repository = AppointmentRepository(dao)
        allAppointments = repository.allAppointments
        upcomingAppointments = repository.upcomingAppointments
        upcomingCount = repository.upcomingCount
    }

    fun insert(appointment: Appointment) = viewModelScope.launch {
        repository.insert(appointment)
    }

    fun delete(appointment: Appointment) = viewModelScope.launch {
        repository.delete(appointment)
    }

    fun update(appointment: Appointment) = viewModelScope.launch {
        repository.update(appointment)
    }

    fun getForPatient(patientId: Int) = repository.getForPatient(patientId)

    fun getByStatus(status: String) = repository.getByStatus(status)
}