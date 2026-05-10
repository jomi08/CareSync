package com.example.caresync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val password: String,
    val token: String,
    val specialization: String = "",
    val department: String = "",
    val experience: String = "",
    val qualification: String = "",
    val hospital: String = "",
    val phone: String = "",
    val bio: String = "",
    val profileImagePath: String = ""
    // ↑ NEW — stores URI of chosen profile photo
)