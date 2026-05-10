package com.example.caresync

import androidx.lifecycle.LiveData

class AppointmentRepository(private val dao: AppointmentDao) {

    val allAppointments: LiveData<List<Appointment>> = dao.getAllAppointments()
    val upcomingAppointments: LiveData<List<Appointment>> = dao.getUpcomingAppointments()
    val upcomingCount: LiveData<Int> = dao.getUpcomingCount()

    suspend fun insert(appointment: Appointment) = dao.insertAppointment(appointment)
    suspend fun delete(appointment: Appointment) = dao.deleteAppointment(appointment)
    suspend fun update(appointment: Appointment) = dao.updateAppointment(appointment)

    fun getForPatient(patientId: Int) = dao.getAppointmentsForPatient(patientId)
    fun getByStatus(status: String) = dao.getAppointmentsByStatus(status)
}