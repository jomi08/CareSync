package com.example.caresync

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caresync.databinding.ActivityPatientDetailBinding
import kotlinx.coroutines.launch

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Patient Details"

        val patientId = intent.getIntExtra("patient_id", -1)
        if (patientId == -1) { finish(); return }

        loadPatient(patientId)
    }

    private fun loadPatient(id: Int) {
        lifecycleScope.launch {
            val db = CareSyncDatabase.getDatabase(applicationContext)
            val patient = db.patientDao().getPatientById(id)

            runOnUiThread {
                binding.tvDetailName.text = patient.name
                binding.tvDetailAgeGender.text = "${patient.age} years • ${patient.gender}"
                binding.tvDetailPhone.text = patient.phone
                binding.tvDetailBlood.text = patient.bloodGroup
                binding.tvDetailDiagnosis.text = patient.diagnosis

                if (patient.imagePath.isNotEmpty()) {
                    Glide.with(this@PatientDetailActivity)
                        .load(Uri.parse(patient.imagePath))
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivDetailPhoto)
                }

                buildMedicationTable(patient.medication)


                binding.btnEdit.setOnClickListener {
                    val intent = Intent(this@PatientDetailActivity, AddPatientActivity::class.java)
                    intent.putExtra("patient_id", patient.id)
                    startActivity(intent)
                }

                binding.btnShare.setOnClickListener {
                    sharePatientSummary(patient)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        val patientId = intent.getIntExtra("patient_id", -1)
        if (patientId != -1) loadPatient(patientId)
    }

    private fun buildMedicationTable(medicationCsv: String) {
        // Clear old rows except header (index 0)
        val rowCount = binding.tableMedication.childCount
        if (rowCount > 1) binding.tableMedication.removeViews(1, rowCount - 1)

        val meds = medicationCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        meds.forEachIndexed { index, med ->
            val row = TableRow(this)
            row.setBackgroundColor(
                if (index % 2 == 0) Color.parseColor("#F8F9FA") else Color.WHITE
            )
            val numText = TextView(this).apply {
                text = "  ${index + 1}  "
                setPadding(8, 12, 8, 12)
                setTextColor(Color.parseColor("#6E6E73"))
            }
            val medText = TextView(this).apply {
                text = med
                setPadding(8, 12, 8, 12)
                setTextColor(Color.parseColor("#1C1C1E"))
            }
            val freqText = TextView(this).apply {
                text = "Twice daily"
                setPadding(8, 12, 8, 12)
                setTextColor(Color.parseColor("#1A73E8"))
            }
            row.addView(numText)
            row.addView(medText)
            row.addView(freqText)
            binding.tableMedication.addView(row)
        }
    }

    private fun sharePatientSummary(patient: Patient) {
        val summary = """
            📋 CareSync — Patient Report
            ━━━━━━━━━━━━━━━━━━━━
            Name       : ${patient.name}
            Age        : ${patient.age} years
            Gender     : ${patient.gender}
            Blood Group: ${patient.bloodGroup}
            Phone      : ${patient.phone}
            ━━━━━━━━━━━━━━━━━━━━
            Diagnosis  : ${patient.diagnosis}
            Medications: ${patient.medication}
            ━━━━━━━━━━━━━━━━━━━━
            Shared via CareSync
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Patient Report — ${patient.name}")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        startActivity(Intent.createChooser(shareIntent, "Share patient report via..."))
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}