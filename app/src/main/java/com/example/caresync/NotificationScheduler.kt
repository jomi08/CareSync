package com.example.caresync

import android.content.Context
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    // Schedule a reminder 1 hour before the appointment
    fun scheduleReminder(context: Context, appointment: Appointment) {
        val appointmentTimeMs = parseAppointmentTime(appointment.date, appointment.time)
            ?: return

        val reminderTimeMs = appointmentTimeMs - TimeUnit.HOURS.toMillis(1)
        val delay = reminderTimeMs - System.currentTimeMillis()

        if (delay <= 0) return  // appointment is in the past or too soon

        val data = workDataOf(
            "appointmentId" to appointment.id,
            "patientName"   to appointment.patientName,
            "date"          to appointment.date,
            "time"          to appointment.time,
            "reason"        to appointment.reason
        )

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("appointment_${appointment.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "appointment_${appointment.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    // Cancel reminder when appointment is deleted or cancelled
    fun cancelReminder(context: Context, appointmentId: Int) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("appointment_$appointmentId")
    }

    // Parses "15 May 2026" + "10:30 AM" → epoch millis
    private fun parseAppointmentTime(date: String, time: String): Long? {
        return try {
            val format = SimpleDateFormat("d MMM yyyy h:mm a", Locale.ENGLISH)
            format.parse("$date $time")?.time
        } catch (e: Exception) {
            null
        }
    }
}