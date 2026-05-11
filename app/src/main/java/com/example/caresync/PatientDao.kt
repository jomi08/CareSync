package com.example.caresync

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PatientDao {

    @Insert
    suspend fun insertPatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    @Update
    suspend fun updatePatient(patient: Patient)

    @Query("SELECT * FROM patients WHERE doctorId = :doctorId ORDER BY name ASC")
    fun getAllPatients(doctorId: Int): LiveData<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Int): Patient

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCount(): Int

    // ── used to check if a patientCode already exists ─────────────────────
    @Query("SELECT * FROM patients WHERE patientCode = :code LIMIT 1")
    suspend fun getPatientByCode(code: String): Patient?

    @Query("""
        SELECT * FROM patients 
        WHERE doctorId = :doctorId
        AND (
            LOWER(name) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(diagnosis) LIKE '%' || LOWER(:query) || '%'
            OR phone LIKE '%' || :query || '%'
            OR patientCode LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun searchPatients(doctorId: Int, query: String): LiveData<List<Patient>>

    @Query("""
        SELECT * FROM patients
        WHERE doctorId = :doctorId
        AND age BETWEEN :minAge AND :maxAge
        ORDER BY name ASC
    """)
    fun getPatientsByAgeRange(
        doctorId: Int,
        minAge: Int,
        maxAge: Int
    ): LiveData<List<Patient>>
}