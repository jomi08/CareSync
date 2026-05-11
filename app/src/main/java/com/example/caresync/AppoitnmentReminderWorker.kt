package com.example.caresync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AppointmentReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val appointmentId = inputData.getInt("appointmentId", -1)
        val patientName   = inputData.getString("patientName") ?: return Result.failure()
        val date          = inputData.getString("date") ?: return Result.failure()
        val time          = inputData.getString("time") ?: return Result.failure()
        val reason        = inputData.getString("reason") ?: return Result.failure()

        if (appointmentId == -1) return Result.failure()

        NotificationHelper.showNotification(
            context      = context,
            appointmentId = appointmentId,
            patientName   = patientName,
            date          = date,
            time          = time,
            reason        = reason
        )

        return Result.success()
    }
}