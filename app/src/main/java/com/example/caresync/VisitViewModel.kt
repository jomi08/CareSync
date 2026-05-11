package com.example.caresync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class VisitViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VisitRepository(
        CareSyncDatabase.getDatabase(app).visitDao()
    )

    fun getVisitsForPatient(patientId: Int): LiveData<List<Visit>> =
        repo.getVisitsForPatient(patientId)

    fun insertVisit(visit: Visit) = viewModelScope.launch {
        repo.insertVisit(visit)
    }

    fun deleteVisit(visit: Visit) = viewModelScope.launch {
        repo.deleteVisit(visit)
    }
}