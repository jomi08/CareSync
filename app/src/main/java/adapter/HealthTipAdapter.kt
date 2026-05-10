package adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caresync.HealthItem
import com.example.caresync.databinding.ItemHealthTipBinding

class HealthTipAdapter(
    private val onReadMoreClick: (HealthItem) -> Unit
) : ListAdapter<HealthItem, HealthTipAdapter.TipViewHolder>(DiffCallback) {

    inner class TipViewHolder(private val binding: ItemHealthTipBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(tip: HealthItem) {
            binding.tvTipTitle.text = tip.Title
            binding.tvTipDescription.text = tip.MyHFDescription

            binding.btnReadMore.setOnClickListener {
                onReadMoreClick(tip)
                // opens WebViewActivity with the article URL
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = ItemHealthTipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HealthItem>() {
        override fun areItemsTheSame(old: HealthItem, new: HealthItem) =
            old.Title == new.Title
        override fun areContentsTheSame(old: HealthItem, new: HealthItem) =
            old == new
    }
}