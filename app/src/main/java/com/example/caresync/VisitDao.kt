package com.example.caresync

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface VisitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit)

    // ASC = oldest visit at top, newest at bottom
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY id ASC")
    fun getVisitsForPatient(patientId: Int): LiveData<List<Visit>>

    @Delete
    suspend fun deleteVisit(visit: Visit)

    @Query("SELECT COUNT(*) FROM visits WHERE patientId = :patientId")
    suspend fun getVisitCount(patientId: Int): Int
}