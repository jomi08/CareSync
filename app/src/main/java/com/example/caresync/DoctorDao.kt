package com.example.caresync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DoctorDao {

    @Insert
    suspend fun insertDoctor(doctor: Doctor)

    // used at login — find doctor by their token
    @Query("SELECT * FROM doctors WHERE token = :token LIMIT 1")
    suspend fun getDoctorByToken(token: String): Doctor?

    // used at registration — make sure token is unique
    // (extremely unlikely to collide but good practice)
    @Query("SELECT * FROM doctors WHERE token = :token LIMIT 1")
    suspend fun checkTokenExists(token: String): Doctor?

    // check if any doctor is registered yet
    @Query("SELECT COUNT(*) FROM doctors")
    suspend fun getDoctorCount(): Int
}