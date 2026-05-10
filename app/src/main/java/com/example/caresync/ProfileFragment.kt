package com.example.caresync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caresync.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: CareSyncDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = CareSyncDatabase.getDatabase(requireContext())

        val prefs = requireActivity()
            .getSharedPreferences("caresync_prefs", Context.MODE_PRIVATE)
        val doctorId = prefs.getInt("logged_in_id", -1)

        loadProfile(doctorId)

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra("doctor_id", doctorId)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    prefs.edit().clear().apply()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadProfile(doctorId: Int) {
        lifecycleScope.launch {
            val doctor = db.doctorDao().getDoctorById(doctorId)

            requireActivity().runOnUiThread {
                doctor?.let { d ->

                    binding.tvDoctorName.text = "Dr. ${d.name}"
                    binding.tvToken.text = d.token

                    // load profile image if saved, else show default icon
                    if (d.profileImagePath.isNotEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(Uri.parse(d.profileImagePath))
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(binding.ivDoctorPhoto)
                    } else {
                        binding.ivDoctorPhoto.setImageResource(R.drawable.ic_person)
                    }

                    binding.tvSpecializationValue.text =
                        d.specialization.ifEmpty { "Not updated" }
                    binding.tvDepartmentValue.text =
                        d.department.ifEmpty { "Not updated" }
                    binding.tvExperienceValue.text =
                        d.experience.ifEmpty { "Not updated" }
                    binding.tvQualificationValue.text =
                        d.qualification.ifEmpty { "Not updated" }
                    binding.tvHospitalValue.text =
                        d.hospital.ifEmpty { "Not updated" }
                    binding.tvPhoneValue.text =
                        d.phone.ifEmpty { "Not updated" }
                    binding.tvBioValue.text =
                        d.bio.ifEmpty { "Not updated" }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // reload every time this tab becomes visible
        // so photo and edits show up immediately after returning
        val prefs = requireActivity()
            .getSharedPreferences("caresync_prefs", Context.MODE_PRIVATE)
        loadProfile(prefs.getInt("logged_in_id", -1))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}