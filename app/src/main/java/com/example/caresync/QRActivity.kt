package com.example.caresync

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.caresync.databinding.ActivityQrBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

class QRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Patient QR Code"

        val patientId = intent.getIntExtra("patient_id", -1)
        if (patientId == -1) { finish(); return }

        loadAndGenerateQR(patientId)
    }

    private fun loadAndGenerateQR(id: Int) {
        lifecycleScope.launch {
            val db      = CareSyncDatabase.getDatabase(applicationContext)
            val patient = db.patientDao().getPatientById(id)

            runOnUiThread {
                // content encoded into QR
                val qrContent = """
                    CareSync Patient
                    ID: ${patient.patientCode}
                    Name: ${patient.name}
                    Age: ${patient.age}
                    Gender: ${patient.gender}
                    Blood: ${patient.bloodGroup}
                    Phone: ${patient.phone}
                    Diagnosis: ${patient.diagnosis}
                """.trimIndent()

                binding.tvQrPatientName.text = patient.name
                binding.tvQrPatientCode.text = patient.patientCode
                binding.tvQrSubtitle.text =
                    "${patient.age} yrs • ${patient.gender} • ${patient.bloodGroup}"

                generateQRCode(qrContent)

                binding.btnShareQr.setOnClickListener {
                    shareQRInfo(patient)
                }
            }
        }
    }

    private fun generateQRCode(content: String) {
        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                600,
                600
            )
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not generate QR", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun shareQRInfo(patient: Patient) {
        val text = """
            🏥 CareSync Patient QR Info
            ━━━━━━━━━━━━━━━━━━━━
            Patient ID : ${patient.patientCode}
            Name       : ${patient.name}
            Age        : ${patient.age} years
            Gender     : ${patient.gender}
            Blood Group: ${patient.bloodGroup}
            Phone      : ${patient.phone}
            Diagnosis  : ${patient.diagnosis}
            ━━━━━━━━━━━━━━━━━━━━
            Shared via CareSync
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share patient info via..."))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}