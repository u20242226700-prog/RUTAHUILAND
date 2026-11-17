package co.edu.usco.rutahuila.ui.trip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.FragmentTripBinding
import co.edu.usco.rutahuila.ui.common.DetailAwareFragment
import co.edu.usco.rutahuila.ui.main.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.ceil


class TripFragment : DetailAwareFragment() {

    private var _binding: FragmentTripBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TripAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupList()
        binding.btnGoExplore.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_explore)
        }
        binding.btnTripDate.setOnClickListener { showDatePicker() }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_trip)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share_trip -> {
                    shareTrip()
                    true
                }
                R.id.action_clear_trip -> {
                    confirmClearTrip()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupList() {
        adapter = TripAdapter { place -> openDetail(place) }
        binding.rvTrip.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTrip.adapter = adapter
        ItemTouchHelper(touchCallback).attachToRecyclerView(binding.rvTrip)
    }

    private val touchCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            adapter.moveItem(from, to)
            PlaceRepository.moveInTrip(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            removeStop(position)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            // Al soltar, se vuelven a numerar las paradas
            recyclerView.post { refresh() }
        }
    }

    private fun removeStop(position: Int) {
        val place = adapter.itemAt(position).place
        val oldIndex = PlaceRepository.removeFromTrip(place.id)
        refresh()
        (activity as? MainActivity)?.updateTripBadge()

        Snackbar.make(binding.root, getString(R.string.snackbar_trip_removed, place.name), Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                PlaceRepository.insertIntoTrip(place.id, oldIndex)
                refresh()
                (activity as? MainActivity)?.updateTripBadge()
            }
            .show()
    }

    override fun refresh() {
        val b = _binding ?: return
        val places = PlaceRepository.tripPlaces()
        adapter.submit(places.map { PlaceRepository.toUi(it) })

        val isEmpty = places.isEmpty()
        b.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.tripContent.visibility = if (isEmpty) View.GONE else View.VISIBLE
        b.toolbar.menu.findItem(R.id.action_share_trip)?.isVisible = !isEmpty
        b.toolbar.menu.findItem(R.id.action_clear_trip)?.isVisible = !isEmpty

        if (!isEmpty) {
            val stops = places.size
            val farthest = places.maxOf { it.distanceKm }
            val days = ceil(stops / STOPS_PER_DAY).toInt()
            val visited = places.count { PlaceRepository.isVisited(it.id) }

            b.tvStops.text = stops.toString()
            b.tvFarthest.text = getString(R.string.trip_km_format, farthest)
            b.tvDays.text = days.toString()
            b.tvProgress.text = getString(R.string.trip_progress, visited, stops)
            b.progressTrip.max = stops
            b.progressTrip.setProgressCompat(visited, true)
            b.btnTripDate.text = tripDateText()
        }
    }

    // =====================================================================
    // Fecha del viaje (calendario + cuenta regresiva)
    // =====================================================================

    /** Abre el calendario de Material para elegir la fecha del viaje. */
    private fun showDatePicker() {
        val saved = PlaceRepository.tripDateMillis
        val initial = if (saved != PlaceRepository.NO_DATE) saved else MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.trip_date_picker_title)
            .setSelection(initial)
            .build()

        picker.addOnPositiveButtonClickListener { selectedMillis ->
            PlaceRepository.tripDateMillis = selectedMillis
            refresh()
        }
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    /** Ejemplo: "📅 15 de noviembre de 2026 · faltan 5 días". */
    private fun tripDateText(): String {
        val millis = PlaceRepository.tripDateMillis
        if (millis == PlaceRepository.NO_DATE) return getString(R.string.trip_date_choose)

        // El calendario entrega la fecha en horario UTC
        val tripDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), tripDate).toInt()
        val dateText = tripDate.format(DATE_FORMAT)

        val countdown = when {
            daysLeft < 0 -> getString(R.string.trip_date_past)
            daysLeft == 0 -> getString(R.string.trip_date_today)
            daysLeft == 1 -> getString(R.string.trip_date_tomorrow)
            else -> getString(R.string.trip_date_days_left, daysLeft)
        }
        return getString(R.string.trip_date_format, dateText, countdown)
    }

    private fun shareTrip() {
        val places = PlaceRepository.tripPlaces()
        if (places.isEmpty()) return

        val stopsText = places.mapIndexed { index, place ->
            "${index + 1}. ${place.emoji} ${place.name} (${place.municipality})"
        }.joinToString("\n")
        val text = getString(R.string.trip_share_text, PlaceRepository.userName, stopsText)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.trip_share_chooser)))
    }

    private fun confirmClearTrip() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.trip_clear_title)
            .setMessage(R.string.trip_clear_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                PlaceRepository.clearTrip()
                refresh()
                (activity as? MainActivity)?.updateTripBadge()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        /** Se estima que se pueden visitar 2 lugares por día. */
        const val STOPS_PER_DAY = 2.0

        const val DATE_PICKER_TAG = "trip_date_picker"

        /** Fecha larga en el idioma del celular: "15 de noviembre de 2026". */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    }
}
