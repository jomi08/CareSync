package com.example.caresync

import adapter.OnboardPage
import adapter.OnboardingAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.caresync.databinding.ActivityOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    // all onboarding slides defined here
    private val pages = listOf(
        OnboardPage(
            emoji = "🏥",
            title = "Welcome to CareSync",
            description = "Your complete patient management solution. Manage records, appointments and more — all in one place."
        ),
        OnboardPage(
            emoji = "👨‍⚕️",
            title = "Doctor Profiles",
            description = "Register as a doctor with a unique token. Your profile stores your specialization, department and hospital details."
        ),
        OnboardPage(
            emoji = "📋",
            title = "Patient Records",
            description = "Add and manage patient records with diagnosis, medications, blood group and profile photos. Filter by age group instantly."
        ),
        OnboardPage(
            emoji = "📅",
            title = "Appointments",
            description = "Schedule appointments for your patients. Track upcoming, completed and cancelled visits all in one screen."
        ),
        OnboardPage(
            emoji = "🔒",
            title = "Safe & Offline",
            description = "All your data is stored securely on your device. No internet required — CareSync works fully offline."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        // connect dots indicator to ViewPager2
        TabLayoutMediator(binding.dotsIndicator, binding.viewPager) { _, _ ->
            // empty — TabLayoutMediator just needs to exist
            // dot_selector drawable handles the active/inactive look
        }.attach()

        // listen for page changes to update button text
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // last page → show "Get Started"
                    // other pages → show "Next"
                    if (position == pages.size - 1) {
                        binding.btnNext.text = "Get Started"
                    } else {
                        binding.btnNext.text = "Next"
                    }

                    // hide skip on last page — no point skipping
                    // when you're already at the end
                    binding.tvSkip.visibility = if (position == pages.size - 1) {
                        android.view.View.INVISIBLE
                    } else {
                        android.view.View.VISIBLE
                    }
                }
            }
        )
    }

    private fun setupButtons() {

        // Next button
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem

            if (currentItem < pages.size - 1) {
                // not last page → go to next slide
                binding.viewPager.currentItem = currentItem + 1
            } else {
                // last page → onboarding done
                finishOnboarding()
            }
        }

        // Skip button → jump straight to end
        binding.tvSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        // save that onboarding is done
        // so it never shows again
        getSharedPreferences("caresync_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_done", true)
            .apply()

        // go to login screen
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}