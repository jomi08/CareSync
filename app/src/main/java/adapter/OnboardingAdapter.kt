package adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caresync.databinding.ItemOnboardBinding

// data class for each onboarding slide
data class OnboardPage(
    val emoji: String,
    val title: String,
    val description: String
)

class OnboardingAdapter(
    private val pages: List<OnboardPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardViewHolder>() {

    inner class OnboardViewHolder(private val binding: ItemOnboardBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardPage) {
            binding.tvOnboardEmoji.text = page.emoji
            binding.tvOnboardTitle.text = page.title
            binding.tvOnboardDescription.text = page.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardViewHolder {
        val binding = ItemOnboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OnboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount() = pages.size
}