package com.example.caresync


import androidx.room.Entity
import androidx.room.PrimaryKey

// This is our appointments table in Room DB
// Each appointment links to a patient via patientId
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val patientId: Int,          // links to Patient.id
    val patientName: String,     // stored here too for easy display
    val doctorId: Int = 0,       // links to logged in doctor
    val date: String,            // "15 May 2026"
    val time: String,            // "10:30 AM"
    val reason: String,          // why they're coming in
    val status: String = "Upcoming"  // Upcoming / Completed / Cancelled
)