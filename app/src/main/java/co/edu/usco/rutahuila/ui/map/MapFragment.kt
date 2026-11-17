package co.edu.usco.rutahuila.ui.map

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.FragmentMapBinding
import co.edu.usco.rutahuila.ui.common.DetailAwareFragment
import co.edu.usco.rutahuila.ui.common.PlaceFormatter
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.config.Configuration as OsmConfiguration

/**
 * PESTAÑA MAPA.
 * Muestra todos los lugares sobre un mapa de OpenStreetMap (librería osmdroid,
 * gratuita y sin llave). Cada lugar es un marcador con su emoji, y la ruta
 * del usuario se dibuja como una línea que une sus paradas en orden.
 */
class MapFragment : DetailAwareFragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    /** Lugar cuya tarjeta se está mostrando (o NO_SELECTION). */
    private var selectedPlaceId = NO_SELECTION

    /** Al tocar una zona vacía del mapa se oculta la tarjeta. */
    private val tapReceiver = object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
            hideCard()
            return true
        }

        override fun longPressHelper(p: GeoPoint?): Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // osmdroid exige esta configuración ANTES de crear el mapa:
        // dónde guardar las imágenes del mapa y cómo se identifica la app.
        val appContext = requireContext().applicationContext
        val osmConfig = OsmConfiguration.getInstance()
        osmConfig.load(appContext, appContext.getSharedPreferences(OSM_PREFS, Context.MODE_PRIVATE))
        osmConfig.userAgentValue = appContext.packageName

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap(binding.mapView)

        binding.fabCenter.setOnClickListener {
            binding.mapView.controller.animateTo(HUILA_CENTER, START_ZOOM, ANIMATION_MS)
        }
        binding.cardPlace.setOnClickListener {
            PlaceRepository.getById(selectedPlaceId)?.let { openDetail(it) }
        }
    }

    private fun setupMap(map: MapView) {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true) // Zoom con dos dedos
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.setMinZoomLevel(MIN_ZOOM)
        map.controller.setZoom(START_ZOOM)
        map.controller.setCenter(HUILA_CENTER)

        // En modo oscuro se invierten los colores del mapa
        if (isNightMode()) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
    }

    private fun isNightMode(): Boolean {
        val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMask == Configuration.UI_MODE_NIGHT_YES
    }

    /** Vuelve a dibujar la ruta y los marcadores con los datos actuales. */
    override fun refresh() {
        val b = _binding ?: return
        val map = b.mapView

        map.overlays.clear()
        map.overlays.add(MapEventsOverlay(tapReceiver))
        addTripLine(map)
        PlaceRepository.getAll().forEach { place -> addMarker(map, place) }
        map.invalidate()

        // Si el lugar seleccionado se editó o se borró, se actualiza la tarjeta
        val selected = PlaceRepository.getById(selectedPlaceId)
        if (selected == null) hideCard() else showCard(selected)
    }

    /** Línea que une las paradas de "Mi ruta" en el orden del usuario. */
    private fun addTripLine(map: MapView) {
        val stops = PlaceRepository.tripPlaces()
        if (stops.size < 2) return

        val line = Polyline(map)
        line.setPoints(stops.map { GeoPoint(it.latitude, it.longitude) })
        line.outlinePaint.color = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        line.outlinePaint.strokeWidth = LINE_WIDTH_DP * resources.displayMetrics.density
        line.setOnClickListener { _, _, _ -> false } // La línea no responde a toques
        map.overlays.add(line)
    }

    private fun addMarker(map: MapView, place: Place) {
        val color = ContextCompat.getColor(requireContext(), place.category.colorRes)
        val marker = Marker(map)
        marker.position = GeoPoint(place.latitude, place.longitude)
        marker.title = place.name
        marker.setIcon(MarkerIconFactory.create(requireContext(), place.emoji, color))
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.setOnMarkerClickListener { _, _ ->
            selectPlace(place)
            true
        }
        map.overlays.add(marker)
    }

    private fun selectPlace(place: Place) {
        selectedPlaceId = place.id
        showCard(place)
        binding.mapView.controller.animateTo(GeoPoint(place.latitude, place.longitude))
    }

    private fun showCard(place: Place) {
        val b = _binding ?: return
        b.tvCardEmoji.text = place.emoji
        b.tvCardName.text = place.name
        b.tvCardLocation.text = PlaceFormatter.location(requireContext(), place)
        b.cardPlace.visibility = View.VISIBLE
    }

    private fun hideCard() {
        selectedPlaceId = NO_SELECTION
        _binding?.cardPlace?.visibility = View.GONE
    }

    // El mapa necesita que se le avise cuando la pantalla se pausa o se reanuda

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.mapView?.onDetach()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val OSM_PREFS = "osmdroid_prefs"
        const val NO_SELECTION = -1
        const val MIN_ZOOM = 7.0
        const val START_ZOOM = 8.3
        const val ANIMATION_MS = 600L
        const val LINE_WIDTH_DP = 4f

        /** Centro aproximado del departamento del Huila. */
        val HUILA_CENTER = GeoPoint(2.55, -75.75)
    }
}
