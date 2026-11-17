package co.edu.usco.rutahuila.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.databinding.ItemOnboardingPageBinding

/** Contenido de una página del tutorial. */
data class OnboardingPage(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int
)

/** Le entrega las páginas al ViewPager2 (funciona igual que el adaptador de una lista). */
class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.tvEmoji.text = page.emoji
            binding.tvTitle.setText(page.titleRes)
            binding.tvText.setText(page.textRes)
        }
    }
}
