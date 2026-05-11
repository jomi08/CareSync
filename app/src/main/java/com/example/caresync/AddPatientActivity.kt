package com.example.caresync

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caresync.databinding.ActivityAddPatientBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddPatientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPatientBinding
    private lateinit var viewModel: PatientViewModel
    private lateinit var db: CareSyncDatabase
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var isEditMode = false
    private var existingPatient: Patient? = null

    // tracks which field the mic is currently recording for
    private var currentVoiceField: VoiceField = VoiceField.DIAGNOSIS

    enum class VoiceField { DIAGNOSIS, MEDICATION }

    // ── Image picker ──────────────────────────────────────────────────────────
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).circleCrop().into(binding.ivPatientPhoto)
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                selectedImageUri = uri
                Glide.with(this).load(uri).circleCrop().into(binding.ivPatientPhoto)
            }
        }
    }

    // ── Voice input result ────────────────────────────────────────────────────
    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull() ?: return@registerForActivityResult

            // append to whichever field triggered the mic
            when (currentVoiceField) {
                VoiceField.DIAGNOSIS -> {
                    val existing = binding.etDiagnosis.text.toString().trim()
                    binding.etDiagnosis.setText(
                        if (existing.isEmpty()) spokenText
                        else "$existing, $spokenText"
                    )
                }
                VoiceField.MEDICATION -> {
                    val existing = binding.etMedication.text.toString().trim()
                    binding.etMedication.setText(
                        if (existing.isEmpty()) spokenText
                        else "$existing, $spokenText"
                    )
                }
            }
        }
    }

    // ── Permission ────────────────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted  = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES]
            ?: permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
            ?: false
        if (!cameraGranted && !storageGranted) {
            Toast.makeText(this, "Permissions needed for photos",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this)[PatientViewModel::class.java]
        db = CareSyncDatabase.getDatabase(this)

        setupDropdowns()
        checkEditMode()

        binding.btnPickImage.setOnClickListener { checkStorageAndOpenGallery() }
        binding.btnTakePhoto.setOnClickListener { checkCameraAndOpen() }
        binding.btnSave.setOnClickListener { saveOrUpdatePatient() }

        // ── Mic buttons ───────────────────────────────────────────────────────
        binding.fabMicDiagnosis.setOnClickListener {
            currentVoiceField = VoiceField.DIAGNOSIS
            startVoiceInput("Speak the diagnosis")
        }

        binding.fabMicMedication.setOnClickListener {
            currentVoiceField = VoiceField.MEDICATION
            startVoiceInput("Speak the medication")
        }
    }

    // ── Voice input launcher ──────────────────────────────────────────────────

    private fun startVoiceInput(prompt: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }

        // check if speech recognition is available
        if (intent.resolveActivity(packageManager) != null) {
            voiceLauncher.launch(intent)
        } else {
            Toast.makeText(this,
                "Speech recognition not available on this device",
                Toast.LENGTH_SHORT).show()
        }
    }

    // ── Edit mode ─────────────────────────────────────────────────────────────

    private fun checkEditMode() {
        val patientId = intent.getIntExtra("patient_id", -1)
        if (patientId != -1) {
            isEditMode = true
            supportActionBar?.title = "Edit Patient"
            binding.btnSave.text = "Update Patient"

            lifecycleScope.launch {
                val patient = db.patientDao().getPatientById(patientId)
                existingPatient = patient
                runOnUiThread { populateFields(patient) }
            }
        } else {
            supportActionBar?.title = "Add Patient"
        }
    }

    private fun populateFields(patient: Patient) {
        binding.etName.setText(patient.name)
        binding.etAge.setText(patient.age.toString())
        binding.actvGender.setText(patient.gender, false)
        binding.actvBloodGroup.setText(patient.bloodGroup, false)
        binding.etPhone.setText(patient.phone)
        binding.etDiagnosis.setText(patient.diagnosis)
        binding.etMedication.setText(patient.medication)

        if (patient.imagePath.isNotEmpty()) {
            selectedImageUri = Uri.parse(patient.imagePath)
            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(binding.ivPatientPhoto)
        }
    }

    // ── Dropdowns ─────────────────────────────────────────────────────────────

    private fun setupDropdowns() {
        val genders = listOf("Male", "Female", "Other")
        binding.actvGender.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        )
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        binding.actvBloodGroup.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        )
    }

    // ── Gallery ───────────────────────────────────────────────────────────────

    private fun checkStorageAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(arrayOf(permission))
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun checkCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile() ?: run {
            Toast.makeText(this, "Could not create image file",
                Toast.LENGTH_SHORT).show()
            return
        }
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        val timestamp  = SimpleDateFormat("yyyyMMdd_HHmmss",
            Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("PATIENT_${timestamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // ── Generate patient code ─────────────────────────────────────────────────

    private suspend fun generatePatientCode(): String {
        var code: String
        var count = db.patientDao().getPatientCount()
        do {
            count++
            code = "PT-%03d".format(count)
        } while (db.patientDao().getPatientByCode(code) != null)
        return code
    }

    // ── Save or update ────────────────────────────────────────────────────────

    private fun saveOrUpdatePatient() {
        val name       = binding.etName.text.toString().trim()
        val ageStr     = binding.etAge.text.toString().trim()
        val gender     = binding.actvGender.text.toString().trim()
        val blood      = binding.actvBloodGroup.text.toString().trim()
        val phone      = binding.etPhone.text.toString().trim()
        val diagnosis  = binding.etDiagnosis.text.toString().trim()
        val medication = binding.etMedication.text.toString().trim()

        if (name.isEmpty())       { binding.tilName.error = "Required"; return }
        if (ageStr.isEmpty())     { binding.tilAge.error = "Required"; return }
        if (gender.isEmpty())     { binding.tilGender.error = "Select gender"; return }
        if (blood.isEmpty())      { binding.tilBlood.error = "Select blood group"; return }
        if (phone.isEmpty())      { binding.tilPhone.error = "Required"; return }
        if (diagnosis.isEmpty())  { binding.tilDiagnosis.error = "Required"; return }
        if (medication.isEmpty()) { binding.tilMedication.error = "Required"; return }

        binding.tilName.error       = null
        binding.tilAge.error        = null
        binding.tilGender.error     = null
        binding.tilBlood.error      = null
        binding.tilPhone.error      = null
        binding.tilDiagnosis.error  = null
        binding.tilMedication.error = null

        val imagePath = selectedImageUri?.toString() ?: ""
        val prefs     = getSharedPreferences("caresync_prefs", MODE_PRIVATE)
        val doctorId  = prefs.getInt("logged_in_id", 0)

        lifecycleScope.launch {
            if (isEditMode && existingPatient != null) {
                val updated = existingPatient!!.copy(
                    name       = name,
                    age        = ageStr.toInt(),
                    gender     = gender,
                    bloodGroup = blood,
                    phone      = phone,
                    diagnosis  = diagnosis,
                    medication = medication,
                    imagePath  = imagePath
                )
                viewModel.update(updated)
                runOnUiThread {
                    Toast.makeText(this@AddPatientActivity,
                        "$name updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                val code = generatePatientCode()
                val patient = Patient(
                    patientCode = code,
                    name        = name,
                    age         = ageStr.toInt(),
                    gender      = gender,
                    bloodGroup  = blood,
                    phone       = phone,
                    diagnosis   = diagnosis,
                    medication  = medication,
                    imagePath   = imagePath,
                    doctorId    = doctorId
                )
                viewModel.insert(patient)
                runOnUiThread {
                    Toast.makeText(this@AddPatientActivity,
                        "$name added! ID: $code", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}