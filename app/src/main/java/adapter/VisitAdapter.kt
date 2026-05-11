package adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caresync.Visit
import com.example.caresync.databinding.ItemVisitBinding

class VisitAdapter(
    private val onDeleteClick: (Visit) -> Unit
) : ListAdapter<Visit, VisitAdapter.VisitViewHolder>(DiffCallback) {

    inner class VisitViewHolder(private val binding: ItemVisitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(visit: Visit, position: Int) {

            // visit number badge
            binding.tvVisitNumber.text = "Visit #${position + 1}"

            // date
            binding.tvVisitDate.text = "📅 ${visit.date}"

            // diagnosis — hide if empty
            if (visit.diagnosis.isNotEmpty()) {
                binding.tvVisitDiagnosis.text = "🩺 ${visit.diagnosis}"
                binding.tvVisitDiagnosis.visibility = View.VISIBLE
            } else {
                binding.tvVisitDiagnosis.visibility = View.GONE
            }

            // prescription — hide if empty
            if (visit.prescription.isNotEmpty()) {
                binding.tvVisitPrescription.text = "💊 ${visit.prescription}"
                binding.tvVisitPrescription.visibility = View.VISIBLE
            } else {
                binding.tvVisitPrescription.visibility = View.GONE
            }

            // notes — hide if empty
            if (visit.notes.isNotEmpty()) {
                binding.tvVisitNotes.text = "📝 ${visit.notes}"
                binding.tvVisitNotes.visibility = View.VISIBLE
            } else {
                binding.tvVisitNotes.visibility = View.GONE
            }

            binding.btnDeleteVisit.setOnClickListener {
                onDeleteClick(visit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemVisitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VisitViewHolder(binding)
    }

    // pass position so visit number shows correctly
    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Visit>() {
        override fun areItemsTheSame(a: Visit, b: Visit) = a.id == b.id
        override fun areContentsTheSame(a: Visit, b: Visit) = a == b
    }
}