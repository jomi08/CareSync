package com.example.caresync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.caresync.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: CareSyncDatabase

    // tracks which panel is showing
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = CareSyncDatabase.getDatabase(this)

        // toggle between login and register
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
        // active tab styling
        binding.tvToggleLogin.setBackgroundResource(R.color.primary)
        binding.tvToggleLogin.setTextColor(getColor(R.color.white))
        binding.tvToggleRegister.setBackgroundResource(R.color.background)
        binding.tvToggleRegister.setTextColor(getColor(R.color.primary))
    }

    private fun switchToRegister() {
        isLoginMode = false
        binding.layoutLogin.visibility = View.GONE
        binding.layoutRegister.visibility = View.VISIBLE
        // active tab styling
        binding.tvToggleRegister.setBackgroundResource(R.color.primary)
        binding.tvToggleRegister.setTextColor(getColor(R.color.white))
        binding.tvToggleLogin.setBackgroundResource(R.color.background)
        binding.tvToggleLogin.setTextColor(getColor(R.color.primary))
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    private fun attemptLogin() {
        val token = binding.etToken.text.toString().trim().uppercase()
        val password = binding.etLoginPassword.text.toString().trim()

        if (token.isEmpty()) {
            binding.tilToken.error = "Enter your token ID"
            return
        }
        if (password.isEmpty()) {
            binding.tilLoginPassword.error = "Enter your password"
            return
        }

        // clear errors
        binding.tilToken.error = null
        binding.tilLoginPassword.error = null

        lifecycleScope.launch {
            val doctor = db.doctorDao().getDoctorByToken(token)

            runOnUiThread {
                when {
                    doctor == null -> {
                        // token not found in DB
                        binding.tilToken.error = "Token not found. Please register first."
                    }
                    doctor.password != password -> {
                        // token found but password wrong
                        binding.tilLoginPassword.error = "Incorrect password"
                    }
                    else -> {
                        // ✅ success — save to SharedPreferences for session
                        saveSession(doctor)
                        navigateToDashboard()
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

        // validation
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

        // clear errors
        binding.tilRegName.error = null
        binding.tilRegPassword.error = null
        binding.tilRegConfirm.error = null

        lifecycleScope.launch {
            // generate a unique CS-XXXX token
            val token = generateUniqueToken()

            val doctor = Doctor(
                name     = name,
                password = password,
                token    = token
            )

            db.doctorDao().insertDoctor(doctor)

            runOnUiThread {
                // show the token in a dialog — user MUST note it down
                showTokenDialog(name, token)
            }
        }
    }

    // ─── Token generation ─────────────────────────────────────────────────────

    private suspend fun generateUniqueToken(): String {
        // keep generating until we get one that doesn't exist
        while (true) {
            val number = (1000..9999).random()
            val token = "CS-$number"
            if (db.doctorDao().checkTokenExists(token) == null) {
                return token  // unique — safe to use
            }
            // if it exists, loop runs again with a new number
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
            .setCancelable(false) // force them to tap OK
            .setPositiveButton("I've saved it — Login now") { _, _ ->
                // auto-fill the token in login form
                switchToLogin()
                binding.etToken.setText(token)
                binding.etLoginPassword.requestFocus()
            }
            .show()
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    private fun saveSession(doctor: Doctor) {
        // save token + name to SharedPreferences
        // DashboardActivity reads these to show the greeting
        getSharedPreferences("caresync_prefs", MODE_PRIVATE)
            .edit()
            .putString("logged_in_token", doctor.token)
            .putString("logged_in_name", doctor.name)
            .putInt("logged_in_id", doctor.id)
            .apply()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish() // remove LoginActivity from back stack
    }
}