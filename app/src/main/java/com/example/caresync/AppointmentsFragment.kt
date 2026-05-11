package com.example.caresync

import adapter.AppointmentAdapter
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.FragmentAppointmentsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppointmentViewModel
    private lateinit var visitViewModel: VisitViewModel
    private lateinit var adapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel      = ViewModelProvider(this)[AppointmentViewModel::class.java]
        visitViewModel = ViewModelProvider(this)[VisitViewModel::class.java]

        setupRecyclerView()
        setupSearch()
        setupChips()
        observeFilteredAppointments()

        binding.fabAddAppointment.setOnClickListener {
            startActivity(Intent(requireContext(), AddAppointmentActivity::class.java))
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setSearchQuery(newText.orEmpty())
                    return true
                }
            })
        binding.searchView.setOnCloseListener {
            viewModel.setSearchQuery("")
            false
        }
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private fun setupChips() {
        binding.chipUpcoming.isChecked = true

        binding.chipUpcoming.setOnClickListener  { viewModel.setStatusFilter("Upcoming") }
        binding.chipAll.setOnClickListener       { viewModel.setStatusFilter("") }
        binding.chipCompleted.setOnClickListener { viewModel.setStatusFilter("Completed") }
        binding.chipCancelled.setOnClickListener { viewModel.setStatusFilter("Cancelled") }
    }

    // ── Observe appointments ──────────────────────────────────────────────────

    private fun observeFilteredAppointments() {
        viewModel.filteredAppointments.observe(viewLifecycleOwner) { appointments ->
            adapter.submitList(appointments)
            binding.tvEmpty.visibility =
                if (appointments.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerAppointments.visibility =
                if (appointments.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(
            onStatusChange = { appointment, newStatus ->
                when (newStatus) {

                    "Completed" -> {
                        // ── Auto-create visit record when marked complete ──────
                        AlertDialog.Builder(requireContext())
                            .setTitle("Mark as Completed?")
                            .setMessage(
                                "This will automatically create a visit record " +
                                        "for ${appointment.patientName}.\n\n" +
                                        "You can add more details from Patient Details."
                            )
                            .setPositiveButton("Complete & Add Visit") { _, _ ->
                                markCompleteAndCreateVisit(appointment)
                            }
                            .setNegativeButton("Just Complete") { _, _ ->
                                // mark complete without creating visit
                                viewModel.update(appointment.copy(status = "Completed"))
                                Snackbar.make(
                                    binding.root,
                                    "Marked as Completed",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                            .show()
                    }

                    "Cancelled" -> {
                        // cancel notification and update status
                        NotificationScheduler.cancelReminder(
                            requireContext(),
                            appointment.id
                        )
                        viewModel.update(appointment.copy(status = "Cancelled"))
                        Snackbar.make(
                            binding.root,
                            "Appointment cancelled",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        viewModel.update(appointment.copy(status = newStatus))
                        Snackbar.make(
                            binding.root,
                            "Marked as $newStatus",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            },

            onDeleteClick = { appointment ->
                // confirm before deleting
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Appointment")
                    .setMessage(
                        "Delete appointment for ${appointment.patientName} " +
                                "on ${appointment.date}?"
                    )
                    .setPositiveButton("Delete") { _, _ ->
                        NotificationScheduler.cancelReminder(
                            requireContext(),
                            appointment.id
                        )
                        viewModel.delete(appointment)
                        Snackbar.make(
                            binding.root,
                            "Appointment removed",
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("Undo") {
                                viewModel.insert(appointment)
                                NotificationScheduler.scheduleReminder(
                                    requireContext(),
                                    appointment
                                )
                            }
                            .setActionTextColor(
                                requireContext().getColor(R.color.primary)
                            )
                            .show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAppointments.adapter = adapter
    }

    // ── Auto create visit when appointment completed ───────────────────────────

    private fun markCompleteAndCreateVisit(appointment: Appointment) {
        lifecycleScope.launch {
            val db = CareSyncDatabase.getDatabase(requireContext())

            // get the doctor id from prefs
            val prefs    = requireActivity()
                .getSharedPreferences("caresync_prefs", android.content.Context.MODE_PRIVATE)
            val doctorId = prefs.getInt("logged_in_id", 0)

            // create visit record from appointment data
            val visit = Visit(
                patientId    = appointment.patientId,
                date         = appointment.date,
                diagnosis    = "Visit for: ${appointment.reason}",
                prescription = "",   // doctor can fill later from patient detail
                notes        = "Auto-created from appointment on ${appointment.date} " +
                        "at ${appointment.time}",
                doctorId     = doctorId
            )

            // save visit
            db.visitDao().insertVisit(visit)

            // update appointment status
            viewModel.update(appointment.copy(status = "Completed"))

            // cancel any pending notification
            NotificationScheduler.cancelReminder(
                requireContext(),
                appointment.id
            )

            // show success snackbar with option to view patient
            activity?.runOnUiThread {
                Snackbar.make(
                    binding.root,
                    "✅ Completed & visit recorded for ${appointment.patientName}",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("View Patient") {
                        // navigate to patient detail to add more info
                        val intent = Intent(
                            requireContext(),
                            PatientDetailActivity::class.java
                        )
                        intent.putExtra("patient_id", appointment.patientId)
                        startActivity(intent)
                    }
                    .setActionTextColor(requireContext().getColor(R.color.accent))
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}