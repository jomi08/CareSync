package com.example.caresync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.OnBackPressedCallback
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caresync.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var db: CareSyncDatabase
    private var doctorId: Int = -1
    private var currentDoctor: Doctor? = null

    // stores the URI of whichever image was chosen (gallery or camera)
    private var selectedImageUri: Uri? = null
    // stores the URI of the camera output file
    private var cameraImageUri: Uri? = null

    // ── Image picker launcher (gallery) ──────────────────────────────────────
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.ivProfilePreview)
        }
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfilePreview)
            }
        }
    }

    // ── Permission launcher ───────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openImagePicker()
        else Toast.makeText(this, "Permission needed to pick photos", Toast.LENGTH_SHORT).show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        db = CareSyncDatabase.getDatabase(this)
        doctorId = intent.getIntExtra("doctor_id", -1)

        if (doctorId == -1) {
            val prefs = getSharedPreferences("caresync_prefs", MODE_PRIVATE)
            doctorId = prefs.getInt("logged_in_id", -1)
        }

        loadDoctorProfile()
        setupBackPress()

        binding.btnPickFromGallery.setOnClickListener { checkGalleryPermissionAndOpen() }
        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndOpen() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
    }

    // ── Load existing profile ─────────────────────────────────────────────────

    private fun loadDoctorProfile() {
        lifecycleScope.launch {
            val doctor = db.doctorDao().getDoctorById(doctorId)
            currentDoctor = doctor

            runOnUiThread {
                doctor?.let { d ->
                    binding.etSpecialization.setText(d.specialization)
                    binding.etDepartment.setText(d.department)
                    binding.etExperience.setText(d.experience)
                    binding.etQualification.setText(d.qualification)
                    binding.etHospital.setText(d.hospital)
                    binding.etPhone.setText(d.phone)
                    binding.etBio.setText(d.bio)

                    if (d.profileImagePath.isNotEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(Uri.parse(d.profileImagePath))
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(binding.ivProfilePreview)
                        selectedImageUri = Uri.parse(d.profileImagePath)
                    }
                }
            }
        }
    }

    // ── Gallery permission + open ─────────────────────────────────────────────

    private fun checkGalleryPermissionAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            openImagePicker()
        else
            permissionLauncher.launch(permission)
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    // ── Camera permission + open ──────────────────────────────────────────────

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)
            openCamera()
        else
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }

        if (intent.resolveActivity(packageManager) != null)
            cameraLauncher.launch(intent)
        else
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
    }

    private fun createImageFile(): File? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("DOCTOR_${timestamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // ── Save profile ──────────────────────────────────────────────────────────

    private fun saveProfile() {
        val specialization = binding.etSpecialization.text.toString().trim()
        val department     = binding.etDepartment.text.toString().trim()
        val experience     = binding.etExperience.text.toString().trim()
        val qualification  = binding.etQualification.text.toString().trim()
        val hospital       = binding.etHospital.text.toString().trim()
        val phone          = binding.etPhone.text.toString().trim()
        val bio            = binding.etBio.text.toString().trim()

        val doctor = currentDoctor ?: return

        val updatedDoctor = doctor.copy(
            specialization   = specialization,
            department       = department,
            experience       = experience,
            qualification    = qualification,
            hospital         = hospital,
            phone            = phone,
            bio              = bio,
            profileImagePath = selectedImageUri?.toString() ?: doctor.profileImagePath
        )

        lifecycleScope.launch {
            db.doctorDao().updateDoctor(updatedDoctor)
            runOnUiThread {
                Toast.makeText(this@EditProfileActivity,
                    "Profile saved!", Toast.LENGTH_SHORT).show()
                goToMain()
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    // ← FIXED: back button now always goes to MainActivity
    // instead of finish() which caused blank screen when
    // back stack was empty (after FLAG_ACTIVITY_CLEAR_TASK)
    override fun onSupportNavigateUp(): Boolean {
        goToMain()
        return true
    }

    // also handle physical back button
    // Modern back press handling using OnBackPressedDispatcher
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToMain()
            }
        })
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}