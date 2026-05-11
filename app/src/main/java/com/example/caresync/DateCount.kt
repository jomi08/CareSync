package com.example.caresync

import androidx.room.ColumnInfo

data class DateCount(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "count") val count: Int
)