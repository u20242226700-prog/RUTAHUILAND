package co.edu.usco.rutahuila.ui.trip

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceUi
import co.edu.usco.rutahuila.databinding.ItemTripStopBinding
import co.edu.usco.rutahuila.ui.common.PlaceFormatter

/**
 * Adaptador de las paradas de la ruta.
 * Usa una lista mutable porque el usuario puede ARRASTRAR para reordenar.
 */
class TripAdapter(
    private val onPlaceClick: (Place) -> Unit
) : RecyclerView.Adapter<TripAdapter.StopViewHolder>() {

    private val items = mutableListOf<PlaceUi>()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newItems: List<PlaceUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): PlaceUi = items[position]

    /** Mueve visualmente una parada mientras el usuario la arrastra. */
    fun moveItem(from: Int, to: Int) {
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemTripStopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class StopViewHolder(
        private val binding: ItemTripStopBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceUi, position: Int) {
            val place = item.place
            val context = binding.root.context
            val color = ContextCompat.getColor(context, place.category.colorRes)

            binding.tvNumber.text = (position + 1).toString()
            binding.tvNumber.backgroundTintList = ColorStateList.valueOf(color)
            binding.tvEmoji.text = place.emoji
            binding.tvName.text = place.name
            binding.tvLocation.text = PlaceFormatter.location(context, place)
            binding.tvVisited.visibility = if (item.isVisited) View.VISIBLE else View.GONE

            // La línea que une las paradas no se dibuja en la última
            binding.connector.visibility =
                if (position == items.lastIndex) View.INVISIBLE else View.VISIBLE

            binding.root.setOnClickListener { onPlaceClick(place) }
        }
    }
}
