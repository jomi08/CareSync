package com.example.caresync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Patient::class, Doctor::class],
    version = 3,
    exportSchema = false
)
abstract class CareSyncDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun doctorDao(): DoctorDao

    companion object {
        @Volatile
        private var INSTANCE: CareSyncDatabase? = null

        fun getDatabase(context: Context): CareSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CareSyncDatabase::class.java,
                    "caresync_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}