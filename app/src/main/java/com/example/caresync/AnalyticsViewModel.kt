package com.example.caresync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppointmentRepository

    val totalCount: LiveData<Int>
    val completedCount: LiveData<Int>
    val cancelledCount: LiveData<Int>
    val upcomingCount: LiveData<Int>
    val appointmentsPerDay: LiveData<List<DateCount>>
    val topPatients: LiveData<List<PatientCount>>

    init {
        val dao = CareSyncDatabase.getDatabase(application).appointmentDao()
        repository = AppointmentRepository(dao)
        totalCount = repository.totalCount
        completedCount = repository.completedCount
        cancelledCount = repository.cancelledCount
        upcomingCount = repository.upcomingCountAnalytics
        appointmentsPerDay = repository.appointmentsPerDay
        topPatients = repository.topPatients
    }
}