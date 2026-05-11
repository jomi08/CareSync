package com.example.caresync

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.caresync.databinding.ActivityAddVisitBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddVisitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVisitBinding
    private lateinit var db: CareSyncDatabase
    private var patientId: Int = -1
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Visit Record"

        db = CareSyncDatabase.getDatabase(this)
        patientId = intent.getIntExtra("patient_id", -1)

        val prefs = getSharedPreferences("caresync_prefs", MODE_PRIVATE)
        doctorId = prefs.getInt("logged_in_id", -1)

        // show today's date by default
        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        binding.tvVisitDate.text = "Date: $today"

        binding.btnSaveVisit.setOnClickListener { saveVisit() }
    }

    private fun saveVisit() {
        val diagnosis    = binding.etVisitDiagnosis.text.toString().trim()
        val prescription = binding.etVisitPrescription.text.toString().trim()
        val notes        = binding.etVisitNotes.text.toString().trim()

        if (diagnosis.isEmpty()) {
            binding.tilVisitDiagnosis.error = "Enter diagnosis"
            return
        }
        binding.tilVisitDiagnosis.error = null

        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val visit = Visit(
            patientId    = patientId,
            date         = today,
            diagnosis    = diagnosis,
            prescription = prescription,
            notes        = notes,
            doctorId     = doctorId
        )

        lifecycleScope.launch {
            db.visitDao().insertVisit(visit)
            runOnUiThread {
                Toast.makeText(this@AddVisitActivity,
                    "Visit recorded!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}