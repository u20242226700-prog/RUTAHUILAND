package co.edu.usco.rutahuila.ui.explore

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceFilter
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.data.SortOrder
import co.edu.usco.rutahuila.databinding.FragmentExploreBinding
import co.edu.usco.rutahuila.ui.common.DetailAwareFragment
import co.edu.usco.rutahuila.ui.common.PlaceAdapter
import co.edu.usco.rutahuila.ui.form.PlaceFormActivity
import co.edu.usco.rutahuila.util.IntentKeys
import co.edu.usco.rutahuila.util.LocationHelper


class ExploreFragment : DetailAwareFragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter

    private var currentFilter = PlaceFilter.ALL
    private var currentQuery = ""
    private var currentSort = SortOrder.NAME

    /** Abre el formulario de lugar nuevo y refresca la lista si se guardó. */
    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refresh()
            showMessage(getString(R.string.snackbar_place_created))
        }
    }

    /** Pide el permiso de ubicación y recibe la respuesta del usuario. */
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            loadLocation()
        } else {
            showMessage(getString(R.string.location_denied))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentFilter = arguments?.getString(IntentKeys.FILTER)
            ?.let { runCatching { PlaceFilter.valueOf(it) }.getOrNull() }
            ?: PlaceFilter.ALL

        setupToolbar()
        setupList()
        setupSearch()
        setupFilters()

        binding.fabSurprise.setOnClickListener { openDetail(PlaceRepository.randomPlace()) }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_explore)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_add) {
                formLauncher.launch(PlaceFormActivity.newIntent(requireContext()))
                true
            } else {
                onSortSelected(item)
            }
        }
    }

    private fun onSortSelected(item: MenuItem): Boolean {
        // "Más cerca de mí" necesita primero el permiso y la ubicación
        if (item.itemId == R.id.sort_near) {
            sortByNearMe()
            return true
        }
        currentSort = when (item.itemId) {
            R.id.sort_name -> SortOrder.NAME
            R.id.sort_distance -> SortOrder.DISTANCE
            R.id.sort_rating -> SortOrder.RATING
            else -> return false
        }
        item.isChecked = true
        refresh()
        binding.rvPlaces.scrollToPosition(0)
        return true
    }

    // =====================================================================
    // Ordenar por cercanía (GPS)
    // =====================================================================

    private fun sortByNearMe() {
        if (LocationHelper.hasPermission(requireContext())) {
            loadLocation()
        } else {
            // Abre el cuadro del sistema que le pide el permiso al usuario
            locationPermission.launch(LocationHelper.PERMISSIONS)
        }
    }

    private fun loadLocation() {
        showMessage(getString(R.string.location_searching))
        LocationHelper.requestLocation(requireContext()) { found ->
            if (_binding == null) return@requestLocation // La pantalla ya se cerró
            if (found) {
                currentSort = SortOrder.NEAR_ME
                binding.toolbar.menu.findItem(R.id.sort_near)?.isChecked = true
                refresh()
                binding.rvPlaces.scrollToPosition(0)
                showMessage(getString(R.string.location_found))
            } else {
                showMessage(getString(R.string.location_not_found))
            }
        }
    }

    private fun setupList() {
        adapter = PlaceAdapter { place -> openDetail(place) }
        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.adapter = adapter

        // El botón "Sorpresa" se encoge al bajar y se expande al subir
        binding.rvPlaces.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.fabSurprise.shrink() else if (dy < 0) binding.fabSurprise.extend()
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            currentQuery = text?.toString().orEmpty()
            refresh()
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilter.check(chipIdFor(currentFilter))
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = filterFor(checkedIds.firstOrNull())
            refresh()
        }
    }

    override fun refresh() {
        val b = _binding ?: return

        b.tvGreeting.text = getString(
            R.string.explore_greeting, PlaceRepository.avatar, PlaceRepository.userName
        )

        val items = PlaceRepository.search(currentFilter, currentQuery, currentSort)
            .map { PlaceRepository.toUi(it) }
        adapter.submitList(items)

        b.tvResultCount.text =
            resources.getQuantityString(R.plurals.places_count, items.size, items.size)

        val isEmpty = items.isEmpty()
        b.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvPlaces.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun snackbarAnchor(): View? = _binding?.fabSurprise

    private fun chipIdFor(filter: PlaceFilter): Int = when (filter) {
        PlaceFilter.ALL -> R.id.chipAll
        PlaceFilter.ADVENTURE -> R.id.chipAdventure
        PlaceFilter.CULTURE -> R.id.chipCulture
        PlaceFilter.NATURE -> R.id.chipNature
        PlaceFilter.MINE -> R.id.chipMine
    }

    private fun filterFor(chipId: Int?): PlaceFilter = when (chipId) {
        R.id.chipAdventure -> PlaceFilter.ADVENTURE
        R.id.chipCulture -> PlaceFilter.CULTURE
        R.id.chipNature -> PlaceFilter.NATURE
        R.id.chipMine -> PlaceFilter.MINE
        else -> PlaceFilter.ALL
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Forma recomendada de crear un Fragment con parámetros. */
        fun newInstance(filter: PlaceFilter) = ExploreFragment().apply {
            arguments = bundleOf(IntentKeys.FILTER to filter.name)
        }
    }
}
