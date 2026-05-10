package com.example.caresync

import adapter.HealthTipAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.BottomSheetHealthTipsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HealthTipsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHealthTipsBinding? = null
    private val binding get() = _binding!!
    private lateinit var healthTipViewModel: HealthTipViewModel
    private lateinit var healthTipAdapter: HealthTipAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHealthTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        healthTipViewModel = ViewModelProvider(this)[HealthTipViewModel::class.java]

        healthTipAdapter = HealthTipAdapter { tip ->
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("url", tip.AccessibleVersion)
            intent.putExtra("title", tip.Title)
            startActivity(intent)
        }

        binding.recyclerHealthTips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHealthTips.adapter = healthTipAdapter

        binding.btnRefreshTips.setOnClickListener {
            healthTipViewModel.fetchNextTopics()
        }

        observeHealthTips()
        healthTipViewModel.fetchHealthTips()
    }

    private fun observeHealthTips() {
        healthTipViewModel.healthTipState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HealthTipState.Loading -> {
                    binding.tipsProgressBar.visibility = View.VISIBLE
                    binding.recyclerHealthTips.visibility = View.GONE
                    binding.tvTipsError.visibility = View.GONE
                }
                is HealthTipState.Success -> {
                    binding.tipsProgressBar.visibility = View.GONE
                    binding.tvTipsError.visibility = View.GONE
                    binding.recyclerHealthTips.visibility = View.VISIBLE
                    healthTipAdapter.submitList(state.tips.take(5))
                }
                is HealthTipState.Error -> {
                    binding.tipsProgressBar.visibility = View.GONE
                    binding.recyclerHealthTips.visibility = View.GONE
                    binding.tvTipsError.visibility = View.VISIBLE
                    binding.tvTipsError.text = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}