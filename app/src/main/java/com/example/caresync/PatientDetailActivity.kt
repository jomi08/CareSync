package com.example.caresync

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.caresync.databinding.ActivityPatientDetailBinding
import adapter.VisitAdapter
import kotlinx.coroutines.launch

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private lateinit var visitViewModel: VisitViewModel
    private lateinit var visitAdapter: VisitAdapter
    private var patientId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Patient Details"

        patientId = intent.getIntExtra("patient_id", -1)
        if (patientId == -1) { finish(); return }

        // ── IMPORTANT: setupVisitHistory() called ONCE in onCreate ────────────
        // never call this inside loadPatient() or onResume()
        // otherwise multiple observers stack up and only last visit shows
        setupVisitHistory()
        loadPatient(patientId)
    }

    // ── Visit history ─────────────────────────────────────────────────────────

    private fun setupVisitHistory() {
        visitViewModel = ViewModelProvider(this)[VisitViewModel::class.java]

        visitAdapter = VisitAdapter { visit ->
            AlertDialog.Builder(this)
                .setTitle("Delete Visit")
                .setMessage(
                    "Delete Visit #${visit.id} from ${visit.date}?\n\n" +
                            "This cannot be undone."
                )
                .setPositiveButton("Delete") { _, _ ->
                    visitViewModel.deleteVisit(visit)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.recyclerVisits.layoutManager = LinearLayoutManager(this)
        binding.recyclerVisits.adapter = visitAdapter

        // ── Single observer registered here — sees ALL visits always ──────────
        // Room LiveData emits full list every time ANY visit is added/deleted
        // DiffUtil in ListAdapter handles smooth UI updates
        visitViewModel.getVisitsForPatient(patientId).observe(this) { visits ->
            if (visits.isEmpty()) {
                binding.tvNoVisits.visibility     = View.VISIBLE
                binding.recyclerVisits.visibility = View.GONE
            } else {
                binding.tvNoVisits.visibility     = View.GONE
                binding.recyclerVisits.visibility = View.VISIBLE
                // submitList compares old vs new list and only redraws changes
                // all existing visits stay, new ones appear at bottom (ASC order)
                visitAdapter.submitList(visits)
            }
        }

        binding.btnAddVisit.setOnClickListener {
            val intent = Intent(this, AddVisitActivity::class.java)
            intent.putExtra("patient_id", patientId)
            startActivity(intent)
        }
    }

    // ── Load patient info ─────────────────────────────────────────────────────

    private fun loadPatient(id: Int) {
        lifecycleScope.launch {
            val db      = CareSyncDatabase.getDatabase(applicationContext)
            val patient = db.patientDao().getPatientById(id)

            runOnUiThread {
                binding.tvDetailName.text      = patient.name
                binding.tvDetailAgeGender.text = "${patient.age} years • ${patient.gender}"
                binding.tvDetailPhone.text     = patient.phone
                binding.tvDetailBlood.text     = patient.bloodGroup
                binding.tvDetailDiagnosis.text = patient.diagnosis

                // patient code — auto-assign if old patient has none
                if (patient.patientCode.isNotEmpty()) {
                    binding.tvPatientCode.text = patient.patientCode
                } else {
                    lifecycleScope.launch {
                        val count   = db.patientDao().getPatientCount()
                        val newCode = "PT-%03d".format(count)
                        db.patientDao().updatePatient(
                            patient.copy(patientCode = newCode)
                        )
                        runOnUiThread {
                            binding.tvPatientCode.text = newCode
                        }
                    }
                }

                // photo
                if (patient.imagePath.isNotEmpty()) {
                    Glide.with(this@PatientDetailActivity)
                        .load(Uri.parse(patient.imagePath))
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivDetailPhoto)
                }

                buildMedicationTable(patient.medication)

                binding.btnEdit.setOnClickListener {
                    startActivity(
                        Intent(this@PatientDetailActivity, AddPatientActivity::class.java)
                            .putExtra("patient_id", patient.id)
                    )
                }

                binding.btnViewQr.setOnClickListener {
                    startActivity(
                        Intent(this@PatientDetailActivity, QRActivity::class.java)
                            .putExtra("patient_id", patient.id)
                    )
                }

                binding.btnShare.setOnClickListener {
                    sharePatientSummary(patient)
                }
            }
        }
    }

    // ── onResume only reloads patient INFO ────────────────────────────────────
    // visits are handled by LiveData in setupVisitHistory() automatically

    override fun onResume() {
        super.onResume()
        if (patientId != -1) loadPatient(patientId)
    }

    // ── Medication table ──────────────────────────────────────────────────────

    private fun buildMedicationTable(medicationCsv: String) {
        val rowCount = binding.tableMedication.childCount
        if (rowCount > 1) binding.tableMedication.removeViews(1, rowCount - 1)

        val meds = medicationCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        meds.forEachIndexed { index, med ->
            val row = TableRow(this)
            row.setBackgroundColor(
                if (index % 2 == 0) Color.parseColor("#F8F9FA")
                else Color.WHITE
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

    // ── Share ─────────────────────────────────────────────────────────────────

    private fun sharePatientSummary(patient: Patient) {
        val summary = """
            📋 CareSync — Patient Report
            ━━━━━━━━━━━━━━━━━━━━
            Patient ID : ${patient.patientCode}
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}