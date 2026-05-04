package com.example.caresync

import androidx.lifecycle.LiveData

class PatientRepository(private val dao: PatientDao) {

    fun getAllPatients(doctorId: Int): LiveData<List<Patient>> {
        return dao.getAllPatients(doctorId)
    }

    fun searchPatients(doctorId: Int, query: String): LiveData<List<Patient>> {
        return dao.searchPatients(doctorId, query)
    }

    // ✅ NEW
    fun getPatientsByAgeRange(
        doctorId: Int,
        minAge: Int,
        maxAge: Int
    ): LiveData<List<Patient>> {
        return dao.getPatientsByAgeRange(doctorId, minAge, maxAge)
    }

    suspend fun insert(patient: Patient) = dao.insertPatient(patient)

    suspend fun delete(patient: Patient) = dao.deletePatient(patient)

    suspend fun update(patient: Patient) = dao.updatePatient(patient)

    suspend fun getPatientById(id: Int): Patient = dao.getPatientById(id)
}