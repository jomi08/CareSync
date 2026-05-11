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

    @Query("SELECT * FROM appointments ORDER BY date ASC, time ASC")
    fun getAllAppointments(): LiveData<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY date ASC")
    fun getAppointmentsForPatient(patientId: Int): LiveData<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE status = 'Upcoming' ORDER BY date ASC")
    fun getUpcomingAppointments(): LiveData<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE status = :status ORDER BY date ASC")
    fun getAppointmentsByStatus(status: String): LiveData<List<Appointment>>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = 'Upcoming'")
    fun getUpcomingCount(): LiveData<Int>

    @Query("""
        SELECT * FROM appointments
        WHERE (:query = '' OR patientName LIKE '%' || :query || '%'
               OR reason LIKE '%' || :query || '%')
        AND (:status = '' OR status = :status)
        ORDER BY date ASC, time ASC
    """)
    fun searchAppointments(
        query: String,
        status: String
    ): LiveData<List<Appointment>>

    // ── Analytics queries ──────────────────────────────────────

    @Query("SELECT COUNT(*) FROM appointments")
    fun getTotalCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = 'Completed'")
    fun getCompletedCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = 'Cancelled'")
    fun getCancelledCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = 'Upcoming'")
    fun getUpcomingCountAnalytics(): LiveData<Int>

    // returns count per date e.g. [("15 May 2026", 3), ("16 May 2026", 2)]
    @Query("""
        SELECT date, COUNT(*) as count
        FROM appointments
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getAppointmentsPerDay(): LiveData<List<DateCount>>

    // top 5 most frequent patients
    @Query("""
        SELECT patientName, COUNT(*) as count
        FROM appointments
        GROUP BY patientName
        ORDER BY count DESC
        LIMIT 5
    """)
    fun getTopPatients(): LiveData<List<PatientCount>>
}