package adapter



import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.caresync.R
import com.example.caresync.Patient
import com.example.caresync.databinding.ItemPatientBinding

class PatientAdapter(
    private val onPatientClick: (Patient) -> Unit,
    private val onDeleteClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(DiffCallback) {

    inner class PatientViewHolder(private val binding: ItemPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: Patient) {
            binding.tvPatientName.text = patient.name
            binding.tvAgeGender.text = "${patient.age} yrs • ${patient.gender}"
            binding.tvDiagnosis.text = patient.diagnosis
            binding.tvBloodGroup.text = patient.bloodGroup

            if (patient.imagePath.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(Uri.parse(patient.imagePath))
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivPatientImage)
            } else {
                binding.ivPatientImage.setImageResource(R.drawable.ic_person)
            }

            binding.root.setOnClickListener { onPatientClick(patient) }
            binding.root.setOnLongClickListener {
                onDeleteClick(patient)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(old: Patient, new: Patient) = old.id == new.id
        override fun areContentsTheSame(old: Patient, new: Patient) = old == new
    }
}