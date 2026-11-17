package co.edu.usco.rutahuila.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.Achievement
import co.edu.usco.rutahuila.databinding.ItemAchievementBinding

/** Muestra los logros en una cuadrícula. Los bloqueados se ven en gris. */
class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Achievement) {
            binding.tvEmoji.text = if (item.unlocked) item.emoji else LOCKED_EMOJI
            binding.tvTitle.setText(item.titleRes)
            binding.tvDescription.setText(item.descriptionRes)
            binding.root.alpha = if (item.unlocked) 1f else LOCKED_ALPHA
            binding.tvStatus.setText(
                if (item.unlocked) R.string.ach_unlocked else R.string.ach_locked
            )
        }
    }

    private companion object {
        const val LOCKED_EMOJI = "🔒"
        const val LOCKED_ALPHA = 0.5f

        val DIFF = object : DiffUtil.ItemCallback<Achievement>() {
            override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement) =
                oldItem.titleRes == newItem.titleRes

            override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement) =
                oldItem == newItem
        }
    }
}
