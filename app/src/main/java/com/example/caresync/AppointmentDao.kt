package com.example.caresync

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppointmentDao {

    @Insert
    suspend fun insertAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    // get all appointments ordered by date
    @Query("SELECT * FROM appointments ORDER BY date ASC, time ASC")
    fun getAllAppointments(): LiveData<List<Appointment>>

    // get appointments for a specific patient
    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY date ASC")
    fun getAppointmentsForPatient(patientId: Int): LiveData<List<Appointment>>

    // get upcoming appointments only
    @Query("SELECT * FROM appointments WHERE status = 'Upcoming' ORDER BY date ASC")
    fun getUpcomingAppointments(): LiveData<List<Appointment>>

    // get appointments by status
    @Query("SELECT * FROM appointments WHERE status = :status ORDER BY date ASC")
    fun getAppointmentsByStatus(status: String): LiveData<List<Appointment>>

    // count upcoming for badge
    @Query("SELECT COUNT(*) FROM appointments WHERE status = 'Upcoming'")
    fun getUpcomingCount(): LiveData<Int>
}