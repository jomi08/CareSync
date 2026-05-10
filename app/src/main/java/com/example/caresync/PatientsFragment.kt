package com.example.caresync

import adapter.PatientAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.FragmentPatientsBinding
import com.google.android.material.snackbar.Snackbar

class PatientsFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!

    private lateinit var patientViewModel: PatientViewModel
    private lateinit var patientAdapter: PatientAdapter

    private var currentPatientList: List<Patient> = emptyList()
    private var currentQuery: String = ""
    private var currentObservedLiveData: LiveData<List<Patient>>? = null

    private val patientObserver = Observer<List<Patient>> { patients ->
        currentPatientList = patients ?: emptyList()
        patientAdapter.submitList(currentPatientList)
        binding.tvPatientCount.text = "${currentPatientList.size} patients registered"

        if (currentPatientList.isEmpty() && currentQuery.isNotEmpty()) {
            binding.tvEmpty.text = "No patients found for \"$currentQuery\""
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerPatients.visibility = View.GONE
            binding.fabShare.visibility = View.GONE
        } else {
            updateEmptyState(currentPatientList.isEmpty())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientViewModel = ViewModelProvider(this)[PatientViewModel::class.java]

        val prefs = requireActivity()
            .getSharedPreferences("caresync_prefs", Context.MODE_PRIVATE)
        val name  = prefs.getString("logged_in_name", "Doctor") ?: "Doctor"
        val token = prefs.getString("logged_in_token", "") ?: ""
        binding.tvGreeting.text = "Hello, Dr. $name 👋"

        binding.tvGreeting.setOnClickListener {
            if (token.isNotEmpty()) {
                val clipboard = requireActivity()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("CareSync Token", token)
                clipboard.setPrimaryClip(clip)
                Snackbar.make(binding.root, "Token copied!", Snackbar.LENGTH_SHORT).show()
            }
        }

        setupPatientRecyclerView()
        setupChips()
        setupSearch()
        switchObserver(patientViewModel.allPatients)

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddPatientActivity::class.java))
        }

        binding.fabShare.setOnClickListener {
            sharePatientList()
        }

        // ── Health Tips FAB → opens bottom sheet ──────────────────────────────
        binding.fabHealthTips.setOnClickListener {
            HealthTipsBottomSheet().show(parentFragmentManager, "HealthTipsBottomSheet")
        }
    }

    // ── Patient RecyclerView ──────────────────────────────────────────────────

    private fun setupPatientRecyclerView() {
        patientAdapter = PatientAdapter(
            onPatientClick = { patient ->
                val intent = Intent(requireContext(), PatientDetailActivity::class.java)
                intent.putExtra("patient_id", patient.id)
                startActivity(intent)
            },
            onDeleteClick = { patient ->
                patientViewModel.delete(patient)
                Snackbar.make(binding.root, "${patient.name} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { patientViewModel.insert(patient) }
                    .setActionTextColor(requireContext().getColor(R.color.primary))
                    .show()
            }
        )
        binding.recyclerPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPatients.adapter = patientAdapter
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            currentQuery = ""
            switchObserver(patientViewModel.allPatients)
        }
        binding.chipChildren.setOnClickListener {
            currentQuery = ""
            switchObserver(patientViewModel.getPatientsByAge(0, 12))
        }
        binding.chipAdults.setOnClickListener {
            currentQuery = ""
            switchObserver(patientViewModel.getPatientsByAge(13, 59))
        }
        binding.chipSenior.setOnClickListener {
            currentQuery = ""
            switchObserver(patientViewModel.getPatientsByAge(60, 120))
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    currentQuery = newText?.trim() ?: ""
                    if (currentQuery.isEmpty()) {
                        binding.chipAll.isChecked = true
                        switchObserver(patientViewModel.allPatients)
                    } else {
                        binding.chipGroup.clearCheck()
                        switchObserver(patientViewModel.searchPatients(currentQuery))
                    }
                    return true
                }
            })
    }

    // ── Observer switching ────────────────────────────────────────────────────

    private fun switchObserver(newLiveData: LiveData<List<Patient>>) {
        currentObservedLiveData?.removeObserver(patientObserver)
        currentObservedLiveData = newLiveData
        newLiveData.observe(viewLifecycleOwner, patientObserver)
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmpty.text = "No patients added yet"
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerPatients.visibility = View.GONE
            binding.fabShare.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerPatients.visibility = View.VISIBLE
            binding.fabShare.visibility = View.VISIBLE
        }
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    private fun sharePatientList() {
        if (currentPatientList.isEmpty()) return
        val sb = StringBuilder()
        sb.appendLine("📋 CareSync — Patient List")
        sb.appendLine("Total: ${currentPatientList.size}")
        currentPatientList.forEachIndexed { i, p ->
            sb.appendLine("${i + 1}. ${p.name} | ${p.age}y | ${p.diagnosis}")
        }
        sb.appendLine("Shared via CareSync")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Share via..."))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}