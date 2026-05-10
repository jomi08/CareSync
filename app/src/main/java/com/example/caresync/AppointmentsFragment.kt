package com.example.caresync

import adapter.AppointmentAdapter
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.FragmentAppointmentsBinding
import com.google.android.material.snackbar.Snackbar

class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppointmentViewModel
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

        viewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]

        setupRecyclerView()
        setupChips()
        observeAppointments(viewModel.upcomingAppointments)

        // FAB to add new appointment
        binding.fabAddAppointment.setOnClickListener {
            startActivity(Intent(requireContext(), AddAppointmentActivity::class.java))
        }
    }

    private fun setupChips() {
        binding.chipUpcoming.setOnClickListener {
            observeAppointments(viewModel.upcomingAppointments)
        }
        binding.chipAll.setOnClickListener {
            observeAppointments(viewModel.allAppointments)
        }
        binding.chipCompleted.setOnClickListener {
            observeAppointments(viewModel.getByStatus("Completed"))
        }
        binding.chipCancelled.setOnClickListener {
            observeAppointments(viewModel.getByStatus("Cancelled"))
        }
    }

    private fun observeAppointments(liveData: androidx.lifecycle.LiveData<List<Appointment>>) {
        liveData.observe(viewLifecycleOwner) { appointments ->
            adapter.submitList(appointments)

            if (appointments.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerAppointments.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerAppointments.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(
            onStatusChange = { appointment, newStatus ->
                // update status — Upcoming → Completed or Cancelled
                val updated = appointment.copy(status = newStatus)
                viewModel.update(updated)
                Snackbar.make(
                    binding.root,
                    "Marked as $newStatus",
                    Snackbar.LENGTH_SHORT
                ).show()
            },
            onDeleteClick = { appointment ->
                viewModel.delete(appointment)
                Snackbar.make(binding.root, "Appointment removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.insert(appointment) }
                    .setActionTextColor(requireContext().getColor(R.color.primary))
                    .show()
            }
        )
        binding.recyclerAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAppointments.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}