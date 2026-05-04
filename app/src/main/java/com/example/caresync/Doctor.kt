package com.example.caresync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val password: String,
    // CS-XXXX format token generated at registration
    // stored here so we can look up by token at login
    val token: String
)