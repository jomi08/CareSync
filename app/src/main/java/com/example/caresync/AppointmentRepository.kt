package com.example.caresync

import androidx.lifecycle.LiveData

class AppointmentRepository(private val dao: AppointmentDao) {

    val allAppointments: LiveData<List<Appointment>> = dao.getAllAppointments()
    val upcomingAppointments: LiveData<List<Appointment>> = dao.getUpcomingAppointments()
    val upcomingCount: LiveData<Int> = dao.getUpcomingCount()

    // analytics
    val totalCount: LiveData<Int> = dao.getTotalCount()
    val completedCount: LiveData<Int> = dao.getCompletedCount()
    val cancelledCount: LiveData<Int> = dao.getCancelledCount()
    val upcomingCountAnalytics: LiveData<Int> = dao.getUpcomingCountAnalytics()
    val appointmentsPerDay: LiveData<List<DateCount>> = dao.getAppointmentsPerDay()
    val topPatients: LiveData<List<PatientCount>> = dao.getTopPatients()

    suspend fun insert(appointment: Appointment) = dao.insertAppointment(appointment)
    suspend fun delete(appointment: Appointment) = dao.deleteAppointment(appointment)
    suspend fun update(appointment: Appointment) = dao.updateAppointment(appointment)

    fun getForPatient(patientId: Int) = dao.getAppointmentsForPatient(patientId)
    fun getByStatus(status: String) = dao.getAppointmentsByStatus(status)
    fun searchAppointments(query: String, status: String) =
        dao.searchAppointments(query, status)
}