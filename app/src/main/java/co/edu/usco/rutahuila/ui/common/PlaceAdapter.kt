package co.edu.usco.rutahuila.ui.common

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceUi
import co.edu.usco.rutahuila.databinding.ItemPlaceBinding


class PlaceAdapter(
    private val onPlaceClick: (Place) -> Unit
) : ListAdapter<PlaceUi, PlaceAdapter.PlaceViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(
        private val binding: ItemPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceUi) {
            val place = item.place
            val context = binding.root.context
            val color = ContextCompat.getColor(context, place.category.colorRes)

            binding.tvEmoji.text = place.emoji
            binding.tvEmoji.backgroundTintList =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 50))

            binding.tvName.text = place.name
            binding.tvLocation.text = PlaceFormatter.location(context, place)

            binding.tvCategory.setText(place.category.labelRes)
            binding.tvCategory.backgroundTintList = ColorStateList.valueOf(color)

            binding.ivFavorite.visibility = if (item.isFavorite) View.VISIBLE else View.GONE
            binding.tvVisited.visibility = if (item.isVisited) View.VISIBLE else View.GONE
            binding.tvInTrip.visibility = if (item.isInTrip) View.VISIBLE else View.GONE

            if (item.rating > 0f) {
                binding.tvRating.visibility = View.VISIBLE
                binding.tvRating.text = PlaceFormatter.rating(context, item.rating)
            } else {
                binding.tvRating.visibility = View.GONE
            }

            binding.root.setOnClickListener { onPlaceClick(place) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<PlaceUi>() {
            override fun areItemsTheSame(oldItem: PlaceUi, newItem: PlaceUi) =
                oldItem.place.id == newItem.place.id

            override fun areContentsTheSame(oldItem: PlaceUi, newItem: PlaceUi) =
                oldItem == newItem
        }
    }
}
