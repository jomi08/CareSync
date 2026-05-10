package com.example.caresync

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.caresync.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity()
            .getSharedPreferences("caresync_prefs", Context.MODE_PRIVATE)

        // read saved dark mode preference
        // false = light mode by default
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        // set switch to match saved preference
        // setChecked without triggering listener
        binding.switchDarkMode.isChecked = isDarkMode

        // listen for toggle change
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            // save the preference
            prefs.edit()
                .putBoolean("dark_mode", isChecked)
                .apply()

            // apply the theme immediately
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
                // ↑ force dark mode regardless of system setting
            } else {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
                // ↑ force light mode
            }
            // Android automatically recreates all activities
            // when night mode changes — no manual restart needed
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}