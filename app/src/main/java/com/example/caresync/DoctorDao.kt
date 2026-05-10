package com.example.caresync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DoctorDao {

    @Insert
    suspend fun insertDoctor(doctor: Doctor)

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Query("SELECT * FROM doctors WHERE token = :token LIMIT 1")
    suspend fun getDoctorByToken(token: String): Doctor?

    @Query("SELECT * FROM doctors WHERE token = :token LIMIT 1")
    suspend fun checkTokenExists(token: String): Doctor?

    @Query("SELECT COUNT(*) FROM doctors")
    suspend fun getDoctorCount(): Int

    // fetch doctor by ID — used in profile screen
    @Query("SELECT * FROM doctors WHERE id = :id")
    suspend fun getDoctorById(id: Int): Doctor?
}