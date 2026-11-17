package co.edu.usco.rutahuila.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.FragmentFavoritesBinding
import co.edu.usco.rutahuila.ui.common.DetailAwareFragment
import co.edu.usco.rutahuila.ui.common.PlaceAdapter
import co.edu.usco.rutahuila.ui.main.MainActivity

/** PESTAÑA FAVORITOS: los lugares que el usuario marcó con estrella. */
class FavoritesFragment : DetailAwareFragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlaceAdapter { place -> openDetail(place) }
        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavorites.adapter = adapter

        binding.btnGoExplore.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_explore)
        }
    }

    override fun refresh() {
        val b = _binding ?: return
        val items = PlaceRepository.favoritePlaces().map { PlaceRepository.toUi(it) }
        adapter.submitList(items)

        b.toolbar.subtitle =
            resources.getQuantityString(R.plurals.favorites_count, items.size, items.size)

        val isEmpty = items.isEmpty()
        b.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
