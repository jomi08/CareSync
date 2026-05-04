package com.example.caresync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val doctorId: Int = 0,        // ✅ NEW — links patient to a doctor
    val name: String,
    val age: Int,
    val gender: String,
    val bloodGroup: String,
    val phone: String,
    val diagnosis: String,
    val medication: String,
    val imagePath: String = ""
)