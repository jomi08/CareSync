package com.example.caresync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.caresync.databinding.ActivityAddAppointmentBinding
import java.util.Calendar

class AddAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAppointmentBinding
    private lateinit var appointmentViewModel: AppointmentViewModel
    private lateinit var patientViewModel: PatientViewModel

    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedPatientId = -1
    private var patientList: List<Patient> = emptyList()

    // track whether we've already scheduled the reminder for this save
    private var reminderScheduled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Appointment"

        appointmentViewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        patientViewModel = ViewModelProvider(this)[PatientViewModel::class.java]

        patientViewModel.allPatients.observe(this) { patients ->
            patientList = patients
            val names = patients.map { it.name }
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.actvPatient.setAdapter(adapter)
        }

        binding.actvPatient.setOnItemClickListener { _, _, position, _ ->
            selectedPatientId = patientList[position].id
        }

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = "$day ${getMonthName(month)} $year"
                    binding.btnPickDate.text = selectedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnPickTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    selectedTime = "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
                    binding.btnPickTime.text = selectedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        }

        binding.btnSaveAppointment.setOnClickListener {
            saveAppointment()
        }
    }

    private fun saveAppointment() {
        val patientName = binding.actvPatient.text.toString().trim()
        val reason = binding.etReason.text.toString().trim()

        if (patientName.isEmpty()) {
            binding.tilPatient.error = "Select a patient"
            return
        }
        if (selectedPatientId == -1) {
            binding.tilPatient.error = "Please pick from the dropdown"
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please pick a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please pick a time", Toast.LENGTH_SHORT).show()
            return
        }
        if (reason.isEmpty()) {
            binding.tilReason.error = "Enter appointment reason"
            return
        }

        val prefs = getSharedPreferences("caresync_prefs", MODE_PRIVATE)
        val doctorId = prefs.getInt("logged_in_id", 0)

        val appointment = Appointment(
            patientId = selectedPatientId,
            patientName = patientName,
            doctorId = doctorId,
            date = selectedDate,
            time = selectedTime,
            reason = reason,
            status = "Upcoming"
        )

        // Insert the appointment
        appointmentViewModel.insert(appointment)

        // Watch allAppointments once to get the auto-generated ID
        // then schedule the reminder and stop watching
        reminderScheduled = false
        appointmentViewModel.allAppointments.observe(this) { appointments ->
            if (!reminderScheduled) {
                val inserted = appointments.lastOrNull {
                    it.patientName == patientName &&
                            it.date == selectedDate &&
                            it.time == selectedTime &&
                            it.reason == reason
                }
                if (inserted != null) {
                    reminderScheduled = true
                    NotificationScheduler.scheduleReminder(this, inserted)
                }
            }
        }

        Toast.makeText(this, "Appointment saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getMonthName(month: Int): String {
        val months = listOf(
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
        )
        return months[month]
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}