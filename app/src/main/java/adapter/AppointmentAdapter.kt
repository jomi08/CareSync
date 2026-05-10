package adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caresync.Appointment
import com.example.caresync.databinding.ItemAppointmentBinding

class AppointmentAdapter(
    private val onStatusChange: (Appointment, String) -> Unit,
    private val onDeleteClick: (Appointment) -> Unit
) : ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(DiffCallback) {

    inner class AppointmentViewHolder(private val binding: ItemAppointmentBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.tvPatientName.text = appointment.patientName
            binding.tvDateTime.text = "📅 ${appointment.date}  🕐 ${appointment.time}"
            binding.tvReason.text = "Reason: ${appointment.reason}"
            binding.tvStatus.text = appointment.status

            // change badge color based on status
            val badgeColor = when (appointment.status) {
                "Upcoming"  -> Color.parseColor("#1A73E8")  // blue
                "Completed" -> Color.parseColor("#34A853")  // green
                "Cancelled" -> Color.parseColor("#EA4335")  // red
                else        -> Color.GRAY
            }
            binding.tvStatus.setBackgroundColor(badgeColor)

            // show/hide buttons based on status
            // only upcoming appointments can be marked complete or cancelled
            val isUpcoming = appointment.status == "Upcoming"
            binding.btnComplete.isEnabled = isUpcoming
            binding.btnCancel.isEnabled = isUpcoming
            binding.btnComplete.alpha = if (isUpcoming) 1f else 0.4f
            binding.btnCancel.alpha = if (isUpcoming) 1f else 0.4f

            binding.btnComplete.setOnClickListener {
                onStatusChange(appointment, "Completed")
            }

            binding.btnCancel.setOnClickListener {
                onStatusChange(appointment, "Cancelled")
            }

            // long press to delete
            binding.root.setOnLongClickListener {
                onDeleteClick(appointment)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(old: Appointment, new: Appointment) = old.id == new.id
        override fun areContentsTheSame(old: Appointment, new: Appointment) = old == new
    }
}