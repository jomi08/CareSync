package com.example.caresync

import androidx.room.ColumnInfo

data class PatientCount(
    @ColumnInfo(name = "patientName") val patientName: String,
    @ColumnInfo(name = "count") val count: Int
)