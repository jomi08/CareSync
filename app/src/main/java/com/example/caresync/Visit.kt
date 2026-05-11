package com.example.caresync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val patientId: Int,          // links to Patient.id
    val date: String,            // e.g. "10 May 2026"
    val diagnosis: String,
    val prescription: String,
    val notes: String,
    val doctorId: Int
)