package com.example.caresync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caresync.databinding.ItemTopPatientBinding

class TopPatientsAdapter(
    private val patients: List<PatientCount>
) : RecyclerView.Adapter<TopPatientsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTopPatientBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopPatientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = patients[position]
        holder.binding.tvRank.text = "#${position + 1}"
        holder.binding.tvPatientName.text = item.patientName
        holder.binding.tvVisitCount.text =
            "${item.count} visit${if (item.count > 1) "s" else ""}"
    }

    override fun getItemCount() = patients.size
}