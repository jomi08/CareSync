package com.example.caresync

import androidx.lifecycle.LiveData

class VisitRepository(private val visitDao: VisitDao) {

    fun getVisitsForPatient(patientId: Int): LiveData<List<Visit>> =
        visitDao.getVisitsForPatient(patientId)

    suspend fun insertVisit(visit: Visit) = visitDao.insertVisit(visit)

    suspend fun deleteVisit(visit: Visit) = visitDao.deleteVisit(visit)

    suspend fun getVisitCount(patientId: Int): Int =
        visitDao.getVisitCount(patientId)
}