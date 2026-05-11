package com.example.caresync

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AppointmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppointmentRepository

    val allAppointments: LiveData<List<Appointment>>
    val upcomingAppointments: LiveData<List<Appointment>>
    val upcomingCount: LiveData<Int>

    private val _searchQuery = MutableLiveData("")
    private val _statusFilter = MutableLiveData("Upcoming")

    val filteredAppointments: LiveData<List<Appointment>> =
        MediatorLiveData<List<Appointment>>().apply {
            var currentQuery = ""
            var currentStatus = "Upcoming"
            var currentSource: LiveData<List<Appointment>>? = null

            fun refresh() {
                currentSource?.let { removeSource(it) }
                val newSource = repository.searchAppointments(currentQuery, currentStatus)
                currentSource = newSource
                addSource(newSource) { value = it }
            }

            addSource(_searchQuery) { q ->
                currentQuery = q
                refresh()
            }
            addSource(_statusFilter) { s ->
                currentStatus = s
                refresh()
            }
        }

    init {
        val dao = CareSyncDatabase.getDatabase(application).appointmentDao()
        repository = AppointmentRepository(dao)
        allAppointments = repository.allAppointments
        upcomingAppointments = repository.upcomingAppointments
        upcomingCount = repository.upcomingCount

        // seed so filteredAppointments emits immediately on launch
        _searchQuery.value = ""
        _statusFilter.value = "Upcoming"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _statusFilter.value = "Upcoming"
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