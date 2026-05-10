package com.example.caresync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Patient::class, Appointment::class, Doctor::class],
    version = 6,
    exportSchema = false
)
abstract class CareSyncDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun doctorDao(): DoctorDao

    companion object {
        @Volatile
        private var INSTANCE: CareSyncDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS appointments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        patientName TEXT NOT NULL,
                        doctorId INTEGER NOT NULL DEFAULT 0,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'Upcoming'
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS doctors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        password TEXT NOT NULL,
                        token TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE doctors ADD COLUMN specialization TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN department TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN experience TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN qualification TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN hospital TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE doctors ADD COLUMN bio TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE doctors ADD COLUMN profileImagePath TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // ── NEW: fixes broken appointments table on devices
        // that had an incomplete migration leave an empty table ──────────────
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // drop the broken empty table
                database.execSQL("DROP TABLE IF EXISTS `appointments`")
                // recreate it correctly with all required columns
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `appointments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `patientId` INTEGER NOT NULL,
                        `patientName` TEXT NOT NULL,
                        `doctorId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `time` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): CareSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CareSyncDatabase::class.java,
                    "caresync_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6  // ← added
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}