package com.example.caresync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.caresync.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: CareSyncDatabase

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = CareSyncDatabase.getDatabase(this)

        binding.tvToggleLogin.setOnClickListener { switchToLogin() }
        binding.tvToggleRegister.setOnClickListener { switchToRegister() }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnRegister.setOnClickListener { attemptRegister() }
    }

    // ─── Toggle UI ────────────────────────────────────────────────────────────

    private fun switchToLogin() {
        isLoginMode = true
        binding.layoutLogin.visibility = View.VISIBLE
        binding.layoutRegister.visibility = View.GONE
        binding.tvToggleLogin.setBackgroundResource(R.color.primary)
        binding.tvToggleLogin.setTextColor(getColor(R.color.white))
        binding.tvToggleRegister.setBackgroundResource(R.color.background)
        binding.tvToggleRegister.setTextColor(getColor(R.color.primary))
    }

    private fun switchToRegister() {
        isLoginMode = false
        binding.layoutLogin.visibility = View.GONE
        binding.layoutRegister.visibility = View.VISIBLE
        binding.tvToggleRegister.setBackgroundResource(R.color.primary)
        binding.tvToggleRegister.setTextColor(getColor(R.color.white))
        binding.tvToggleLogin.setBackgroundResource(R.color.background)
        binding.tvToggleLogin.setTextColor(getColor(R.color.primary))
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    private fun attemptLogin() {
        val token    = binding.etToken.text.toString().trim().uppercase()
        val password = binding.etLoginPassword.text.toString().trim()

        if (token.isEmpty()) {
            binding.tilToken.error = "Enter your token ID"
            return
        }
        if (password.isEmpty()) {
            binding.tilLoginPassword.error = "Enter your password"
            return
        }

        binding.tilToken.error = null
        binding.tilLoginPassword.error = null

        lifecycleScope.launch {
            val doctor = db.doctorDao().getDoctorByToken(token)

            runOnUiThread {
                when {
                    doctor == null -> {
                        binding.tilToken.error = "Token not found. Please register first."
                    }
                    doctor.password != password -> {
                        binding.tilLoginPassword.error = "Incorrect password"
                    }
                    else -> {
                        // save session then check if profile needs filling
                        saveSession(doctor)
                        checkAndPromptProfile(doctor)
                    }
                }
            }
        }
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    private fun attemptRegister() {
        val name     = binding.etRegName.text.toString().trim()
        val password = binding.etRegPassword.text.toString().trim()
        val confirm  = binding.etRegConfirm.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilRegName.error = "Enter your name"
            return
        }
        if (password.isEmpty()) {
            binding.tilRegPassword.error = "Create a password"
            return
        }
        if (password.length < 4) {
            binding.tilRegPassword.error = "Minimum 4 characters"
            return
        }
        if (confirm != password) {
            binding.tilRegConfirm.error = "Passwords do not match"
            return
        }

        binding.tilRegName.error = null
        binding.tilRegPassword.error = null
        binding.tilRegConfirm.error = null

        lifecycleScope.launch {
            val token = generateUniqueToken()

            val doctor = Doctor(
                name     = name,
                password = password,
                token    = token
            )

            db.doctorDao().insertDoctor(doctor)

            runOnUiThread {
                showTokenDialog(name, token)
            }
        }
    }

    // ─── Token generation ─────────────────────────────────────────────────────

    private suspend fun generateUniqueToken(): String {
        while (true) {
            val number = (1000..9999).random()
            val token  = "CS-$number"
            if (db.doctorDao().checkTokenExists(token) == null) {
                return token
            }
        }
    }

    // ─── Token reveal dialog ──────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun showTokenDialog(name: String, token: String) {
        AlertDialog.Builder(this)
            .setTitle("Registration Successful!")
            .setMessage(
                "Welcome, Dr. $name!\n\n" +
                        "Your login token is:\n\n" +
                        "🔑  $token\n\n" +
                        "Please save this token. " +
                        "You will need it every time you log in. " +
                        "It cannot be recovered if lost."
            )
            .setCancelable(false)
            .setPositiveButton("I've saved it — Login now") { _, _ ->
                switchToLogin()
                binding.etToken.setText(token)
                binding.etLoginPassword.requestFocus()
            }
            .show()
    }

    // ─── Profile prompt on first login ────────────────────────────────────────

    private fun checkAndPromptProfile(doctor: Doctor) {
        // if all key profile fields are empty = first time login
        // show a friendly dialog asking to fill profile
        val isProfileEmpty = doctor.specialization.isEmpty() &&
                doctor.department.isEmpty() &&
                doctor.hospital.isEmpty()

        if (isProfileEmpty) {
            AlertDialog.Builder(this)
                .setTitle("Complete Your Profile 👨‍⚕️")
                .setMessage(
                    "Welcome, Dr. ${doctor.name}!\n\n" +
                            "Would you like to fill in your profile details?\n" +
                            "(Specialization, Department, Hospital etc.)\n\n" +
                            "You can always update this later from the Profile tab."
                )
                .setCancelable(false)
                .setPositiveButton("Fill Now") { _, _ ->
                    // go directly to EditProfileActivity
                    val intent = Intent(this, EditProfileActivity::class.java)
                    intent.putExtra("doctor_id", doctor.id)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Later") { _, _ ->
                    // skip profile, go straight to main app
                    navigateToDashboard()
                }
                .show()
        } else {
            // profile already filled — go straight to dashboard
            navigateToDashboard()
        }
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    private fun saveSession(doctor: Doctor) {
        getSharedPreferences("caresync_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            // splash screen reads this to skip login next time
            .putString("logged_in_token", doctor.token)
            .putString("logged_in_name", doctor.name)
            .putInt("logged_in_id", doctor.id)
            .apply()
    }

    private fun navigateToDashboard() {
        // goes to MainActivity which hosts bottom nav + all fragments
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        // clears back stack so back button exits app
        // instead of returning to login screen
        startActivity(intent)
        finish()
    }
}