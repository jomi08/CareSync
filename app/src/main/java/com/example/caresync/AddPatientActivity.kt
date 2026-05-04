package com.example.caresync

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null


    private var isEditMode = false
    private var existingPatient: Patient? = null


    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).circleCrop().into(binding.ivPatientPhoto)
        }
    }


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


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES]
            ?: permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
            ?: false
        // User will tap the button again after granting
        if (!cameraGranted && !storageGranted) {
            Toast.makeText(this, "Permissions needed for photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this)[PatientViewModel::class.java]

        setupDropdowns()
        checkEditMode()

        binding.btnPickImage.setOnClickListener { checkStorageAndOpenGallery() }
        binding.btnTakePhoto.setOnClickListener { checkCameraAndOpen() }
        binding.btnSave.setOnClickListener { saveOrUpdatePatient() }
    }


    private fun checkEditMode() {
        val patientId = intent.getIntExtra("patient_id", -1)
        if (patientId != -1) {
            isEditMode = true
            supportActionBar?.title = "Edit Patient"
            binding.btnSave.text = "Update Patient"

            lifecycleScope.launch {
                val db = CareSyncDatabase.getDatabase(applicationContext)
                val patient = db.patientDao().getPatientById(patientId)
                existingPatient = patient
                populateFields(patient)
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


    private fun checkStorageAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(arrayOf(permission))
        }
    }


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
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show()
            return
        }
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )
        val intent = android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("PATIENT_${timestamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    private fun saveOrUpdatePatient() {
        val name       = binding.etName.text.toString().trim()
        val ageStr     = binding.etAge.text.toString().trim()
        val gender     = binding.actvGender.text.toString().trim()
        val blood      = binding.actvBloodGroup.text.toString().trim()
        val phone      = binding.etPhone.text.toString().trim()
        val diagnosis  = binding.etDiagnosis.text.toString().trim()
        val medication = binding.etMedication.text.toString().trim()


        if (name.isEmpty())      { binding.tilName.error = "Required"; return }
        if (ageStr.isEmpty())    { binding.tilAge.error = "Required"; return }
        if (gender.isEmpty())    { binding.tilGender.error = "Select gender"; return }
        if (blood.isEmpty())     { binding.tilBlood.error = "Select blood group"; return }
        if (phone.isEmpty())     { binding.tilPhone.error = "Required"; return }
        if (diagnosis.isEmpty()) { binding.tilDiagnosis.error = "Required"; return }
        if (medication.isEmpty()){ binding.tilMedication.error = "Required"; return }

        val imagePath = selectedImageUri?.toString() ?: ""

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
            Toast.makeText(this, "$name updated!", Toast.LENGTH_SHORT).show()
        } else {
            val patient = Patient(
                name       = name,
                age        = ageStr.toInt(),
                gender     = gender,
                bloodGroup = blood,
                phone      = phone,
                diagnosis  = diagnosis,
                medication = medication,
                imagePath  = imagePath
            )
            viewModel.insert(patient)
            Toast.makeText(this, "$name added successfully!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}