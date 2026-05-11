package com.example.caresync

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkWhereToGo()
        }, 2000)
    }

    private fun checkWhereToGo() {
        val prefs = getSharedPreferences("caresync_prefs", MODE_PRIVATE)

        val onboardingDone = prefs.getBoolean("onboarding_done", false)
        val isLoggedIn     = prefs.getBoolean("is_logged_in", false)

        when {
            // first ever launch — show onboarding
            !onboardingDone -> {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }

            // onboarding done + logged in → go straight to app
            isLoggedIn -> {
                startActivity(Intent(this, MainActivity::class.java))
            }

            // onboarding done but not logged in → go to login
            else -> {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        finish()
    }
}