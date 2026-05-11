package com.example.caresync

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AnalyticsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AnalyticsViewModel::class.java]

        setupBarChart()
        observeStats()
    }

    private fun observeStats() {

        viewModel.totalCount.observe(viewLifecycleOwner) {
            binding.tvTotal.text = it.toString()
        }

        viewModel.upcomingCount.observe(viewLifecycleOwner) {
            binding.tvUpcoming.text = it.toString()
        }

        viewModel.completedCount.observe(viewLifecycleOwner) {
            binding.tvCompleted.text = it.toString()
        }

        viewModel.cancelledCount.observe(viewLifecycleOwner) {
            binding.tvCancelled.text = it.toString()
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { total ->
            viewModel.completedCount.observe(viewLifecycleOwner) { completed ->
                val rate = if (total > 0) (completed * 100) / total else 0
                binding.progressCompletion.progress = rate
                binding.tvCompletionRate.text = "$rate% of appointments completed"
            }
        }

        viewModel.appointmentsPerDay.observe(viewLifecycleOwner) { dateCounts ->
            if (dateCounts.isEmpty()) return@observe

            val entries = dateCounts.mapIndexed { index, dc ->
                BarEntry(index.toFloat(), dc.count.toFloat())
            }

            // Shorten labels to "15 May" instead of "15 May 2026"
            // so they fit without overlapping
            val labels = dateCounts.map { dc ->
                val parts = dc.date.split(" ")
                if (parts.size >= 2) "${parts[0]} ${parts[1]}" else dc.date
            }

            val dataSet = BarDataSet(entries, "Appointments").apply {
                color = Color.parseColor("#1A73E8")
                valueTextColor = Color.GRAY
                valueTextSize = 10f
            }

            binding.barChart.apply {
                data = BarData(dataSet).apply {
                    barWidth = 0.5f
                }
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.labelCount = labels.size
                // Extra bottom offset so labels aren't clipped
                setExtraOffsets(8f, 8f, 8f, 24f)
                animateY(600)
                invalidate()
            }
        }

        viewModel.topPatients.observe(viewLifecycleOwner) { patients ->
            binding.rvTopPatients.layoutManager = LinearLayoutManager(requireContext())
            binding.rvTopPatients.adapter = TopPatientsAdapter(patients)
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setNoDataText("No appointment data yet")

            // Extra bottom padding so rotated labels fully show
            setExtraOffsets(8f, 8f, 8f, 24f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 11f
                // Reduce rotation so labels are more readable
                labelRotationAngle = -45f
                // Avoid label crowding
                isGranularityEnabled = true
                setCenterAxisLabels(false)
                yOffset = 8f  // push labels down away from bars
            }

            axisLeft.apply {
                granularity = 1f
                setDrawGridLines(true)
                axisMinimum = 0f
                textSize = 11f
            }

            axisRight.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}