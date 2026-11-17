package co.edu.usco.rutahuila.ui.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PhotoStorage
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceGuides
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.data.weather.WeatherRepository
import co.edu.usco.rutahuila.data.weather.WeatherResult
import co.edu.usco.rutahuila.databinding.ActivityDetailBinding
import co.edu.usco.rutahuila.ui.common.PlaceFormatter
import co.edu.usco.rutahuila.ui.form.PlaceFormActivity
import co.edu.usco.rutahuila.util.IntentKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt


class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var place: Place

    // Estado inicial, para saber qué cambió al volver
    private var initialFavorite = false
    private var initialInTrip = false
    private var initialVisited = false
    private var ratingChanged = false

    /** Abre el formulario de edición y recarga el lugar al volver. */
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) onPlaceEdited()
    }

    /** Abre la app de cámara. Devuelve true si el usuario tomó la foto. */
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) savePhoto { PhotoStorage.saveFromCamera(applicationContext, place.id) }
    }

    /** Abre el selector de fotos del sistema (no necesita permisos). */
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) savePhoto { PhotoStorage.saveFromGallery(applicationContext, uri, place.id) }
    }

    /** Pide el permiso de cámara si el usuario todavía no lo ha aceptado. */
    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else showMessage(getString(R.string.camera_denied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val found = PlaceRepository.getById(intent.getIntExtra(IntentKeys.PLACE_ID, -1))
        if (found == null) {
            Toast.makeText(this, R.string.error_place_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        place = found
        initialFavorite = PlaceRepository.isFavorite(place.id)
        initialInTrip = PlaceRepository.isInTrip(place.id)
        initialVisited = PlaceRepository.isVisited(place.id)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupOwnerMenu()
        showPlaceInfo()
        showUserState()
        showPhoto()
        setupActions()
        loadWeather()
    }

    // =====================================================================
    // Mostrar información
    // =====================================================================

    /** Datos del lugar (tabla places). Se vuelve a llamar después de editarlo. */
    private fun showPlaceInfo() {
        val color = ContextCompat.getColor(this, place.category.colorRes)
        val white = ContextCompat.getColor(this, R.color.white)

        binding.collapsingToolbar.title = place.name
        binding.collapsingToolbar.setContentScrimColor(color)
        binding.collapsingToolbar.setStatusBarScrimColor(color)
        binding.collapsingToolbar.setExpandedTitleColor(white)
        binding.collapsingToolbar.setCollapsedTitleTextColor(white)
        binding.headerContainer.setBackgroundColor(color)
        binding.tvEmoji.text = place.emoji

        binding.tvCategory.setText(place.category.labelRes)
        binding.tvCategory.backgroundTintList = ColorStateList.valueOf(color)
        binding.tvCustomBadge.visibility = if (place.isCustom) View.VISIBLE else View.GONE
        binding.tvLocation.text = PlaceFormatter.location(this, place)
        binding.tvDescription.text = place.description

        // Los lugares creados por el usuario no tienen consejo
        if (place.tip.isBlank()) {
            binding.tvTip.visibility = View.GONE
        } else {
            binding.tvTip.visibility = View.VISIBLE
            binding.tvTip.text = getString(R.string.detail_tip, PlaceRepository.userName, place.tip)
        }
        showGuide()
    }

    /** Ficha "Planea tu visita". Solo la tienen los lugares del catálogo. */
    private fun showGuide() {
        val guide = PlaceGuides.forPlace(place.id)
        if (guide == null) {
            binding.guideSection.visibility = View.GONE
            return
        }
        binding.guideSection.visibility = View.VISIBLE
        binding.tvGuideDuration.text = getString(R.string.guide_duration, guide.duration)
        binding.tvGuideDifficulty.text =
            getString(R.string.guide_difficulty, getString(guide.difficulty.labelRes))
        binding.tvGuideBestTime.text = getString(R.string.guide_best_time, guide.bestTime)
        binding.tvGuideBring.text = getString(R.string.guide_bring, guide.whatToBring)
    }

    /** Datos del usuario sobre este lugar (tabla user_place). */
    private fun showUserState() {
        binding.switchVisited.isChecked = initialVisited
        binding.etNote.setText(PlaceRepository.getNote(place.id))

        val rating = PlaceRepository.getRating(place.id)
        binding.ratingBar.rating = rating
        updateRatingLabel(rating)
        updateFavoriteButton()
        updateTripButton()
        updateVisitedBadge()
    }

    private fun updateFavoriteButton() {
        val isFavorite = PlaceRepository.isFavorite(place.id)
        binding.fabFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border
        )
        binding.fabFavorite.contentDescription = getString(
            if (isFavorite) R.string.detail_remove_favorite else R.string.detail_add_favorite
        )
    }

    private fun updateTripButton() {
        val inTrip = PlaceRepository.isInTrip(place.id)
        binding.btnTrip.setText(if (inTrip) R.string.detail_remove_trip else R.string.detail_add_trip)
        binding.btnTrip.setIconResource(if (inTrip) R.drawable.ic_check else R.drawable.ic_route)
    }

    private fun updateVisitedBadge() {
        binding.tvVisitedBadge.visibility =
            if (PlaceRepository.isVisited(place.id)) View.VISIBLE else View.GONE
    }

    private fun updateRatingLabel(rating: Float) {
        binding.tvRatingLabel.text = if (rating <= 0f) {
            getString(R.string.detail_rating_none)
        } else {
            val labels = resources.getStringArray(R.array.rating_labels)
            val index = (ceil(rating).toInt() - 1).coerceIn(0, labels.lastIndex)
            labels[index]
        }
    }

    // =====================================================================
    // Acciones del usuario
    // =====================================================================

    private fun setupActions() {
        binding.fabFavorite.setOnClickListener {
            val nowFavorite = PlaceRepository.toggleFavorite(place.id)
            updateFavoriteButton()
            publishResult()
            showMessage(
                if (nowFavorite) getString(R.string.snackbar_favorite_added, place.name)
                else getString(R.string.snackbar_favorite_removed, place.name)
            )
        }

        binding.btnTrip.setOnClickListener {
            PlaceRepository.toggleTrip(place.id)
            updateTripButton()
            publishResult()
        }

        binding.switchVisited.setOnCheckedChangeListener { _, isChecked ->
            PlaceRepository.setVisited(place.id, isChecked)
            updateVisitedBadge()
            publishResult()
        }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                PlaceRepository.setRating(place.id, rating)
                ratingChanged = true
                updateRatingLabel(rating)
                publishResult()
            }
        }

        // El botón de guardar nota solo se activa si el texto cambió
        binding.btnSaveNote.isEnabled = false
        binding.etNote.doAfterTextChanged { text ->
            binding.btnSaveNote.isEnabled =
                text?.toString()?.trim().orEmpty() != PlaceRepository.getNote(place.id)
        }
        binding.btnSaveNote.setOnClickListener { saveNote() }

        binding.btnTakePhoto.setOnClickListener { openCamera() }
        binding.btnPickPhoto.setOnClickListener { openGallery() }
        binding.btnRemovePhoto.setOnClickListener { removePhoto() }

        binding.btnRetryWeather.setOnClickListener { loadWeather() }
        binding.btnMaps.setOnClickListener { openMaps() }
        binding.btnShare.setOnClickListener { sharePlace() }
    }

    private fun saveNote() {
        PlaceRepository.setNote(place.id, binding.etNote.text?.toString().orEmpty())
        binding.btnSaveNote.isEnabled = false
        binding.etNote.clearFocus()
        hideKeyboard()
        showMessage(getString(R.string.detail_note_saved))
    }

    /**
     * Guarda el resultado que recibirá la pantalla anterior cuando el usuario vuelva.
     * Se llama cada vez que algo cambia, así siempre está actualizado.
     */
    private fun publishResult() {
        val data = Intent().apply {
            putExtra(IntentKeys.PLACE_ID, place.id)
            putExtra(IntentKeys.FAVORITE_CHANGED, PlaceRepository.isFavorite(place.id) != initialFavorite)
            putExtra(IntentKeys.TRIP_CHANGED, PlaceRepository.isInTrip(place.id) != initialInTrip)
            putExtra(IntentKeys.VISITED_CHANGED, PlaceRepository.isVisited(place.id) != initialVisited)
            putExtra(IntentKeys.RATING_CHANGED, ratingChanged)
        }
        setResult(RESULT_OK, data)
    }

    // =====================================================================
    // Foto del lugar (cámara y galería)
    // =====================================================================

    /** Antes de abrir la cámara se revisa que el permiso esté aceptado. */
    private fun openCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            takePicture.launch(PhotoStorage.cameraUri(this))
        } catch (e: ActivityNotFoundException) {
            showMessage(getString(R.string.error_no_camera))
        } catch (e: SecurityException) {
            showMessage(getString(R.string.camera_denied))
        }
    }

    private fun openGallery() {
        try {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: ActivityNotFoundException) {
            showMessage(getString(R.string.detail_photo_error))
        }
    }

    /** Procesa la imagen en segundo plano y guarda su ruta en la base de datos. */
    private fun savePhoto(save: suspend () -> String?) {
        if (!::place.isInitialized) return
        lifecycleScope.launch {
            val path = save()
            if (path == null) {
                showMessage(getString(R.string.detail_photo_error))
            } else {
                PlaceRepository.setPhoto(place.id, path)
                showPhoto()
                showMessage(getString(R.string.detail_photo_saved))
            }
        }
    }

    private fun removePhoto() {
        PlaceRepository.removePhoto(place.id)
        showPhoto()
        showMessage(getString(R.string.detail_photo_removed))
    }

    /** Muestra la foto en el encabezado y en su tarjeta (o el estado vacío). */
    private fun showPhoto() {
        val bitmap = PlaceRepository.getPhotoPath(place.id)?.let { PhotoStorage.load(it) }
        val hasPhoto = bitmap != null
        val photoVisibility = if (hasPhoto) View.VISIBLE else View.GONE

        binding.ivPhoto.setImageBitmap(bitmap)
        binding.ivHeaderPhoto.setImageBitmap(bitmap)
        binding.ivPhoto.visibility = photoVisibility
        binding.ivHeaderPhoto.visibility = photoVisibility
        binding.headerScrim.visibility = photoVisibility
        binding.btnRemovePhoto.visibility = photoVisibility
        binding.tvEmoji.visibility = if (hasPhoto) View.GONE else View.VISIBLE
        binding.tvPhotoEmpty.visibility = if (hasPhoto) View.GONE else View.VISIBLE
    }

    // =====================================================================
    // Editar y eliminar (solo lugares creados por el usuario)
    // =====================================================================

    private fun setupOwnerMenu() {
        if (!place.isCustom) return
        binding.toolbar.inflateMenu(R.menu.menu_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    editLauncher.launch(PlaceFormActivity.newIntent(this, place.id))
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
    }

    private fun onPlaceEdited() {
        val updated = PlaceRepository.getById(place.id)
        if (updated == null) {
            finish()
            return
        }
        place = updated
        showPlaceInfo()
        loadWeather()
        publishResult()
        showMessage(getString(R.string.snackbar_place_updated))
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_place_title)
            .setMessage(getString(R.string.delete_place_message, place.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete_confirm) { _, _ -> deletePlace() }
            .show()
    }

    private fun deletePlace() {
        val name = place.name
        PlaceRepository.deleteCustomPlace(place.id)
        setResult(RESULT_OK, Intent().putExtra(IntentKeys.DELETED_NAME, name))
        finish()
    }

    // =====================================================================
    // Clima (API REST + corrutinas)
    // =====================================================================

    private fun loadWeather() {
        showWeatherState(loading = true)
        lifecycleScope.launch {
            // El repositorio decide si el clima viene de internet o de la copia guardada
            val result = WeatherRepository.getWeather(place)
            if (result == null) {
                showWeatherState(error = true)
            } else {
                showWeather(result)
            }
        }
    }

    private fun showWeather(result: WeatherResult) {
        val weather = result.weather
        showWeatherState()

        // Aviso cuando los datos son una copia guardada (sin conexión)
        if (result.isFromCache) {
            val savedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(result.savedAtMillis))
            binding.tvWeatherOffline.text = getString(R.string.weather_offline, savedAt)
            binding.tvWeatherOffline.visibility = View.VISIBLE
        } else {
            binding.tvWeatherOffline.visibility = View.GONE
        }

        binding.tvWeatherEmoji.text = weather.emoji
        binding.tvTemperature.text = getString(R.string.weather_temperature, weather.temperature.roundToInt())
        binding.tvWeatherDescription.setText(weather.descriptionRes)
        binding.tvWeatherExtra.text = getString(
            R.string.weather_extra, weather.humidity, weather.windSpeed.roundToInt()
        )

        // Pronóstico: cada día de la lista se muestra en su casilla
        val dayViews = listOf(binding.tvForecast1, binding.tvForecast2, binding.tvForecast3)
        dayViews.forEachIndexed { index, view ->
            val day = weather.forecast.getOrNull(index)
            view.text = if (day == null) {
                ""
            } else {
                getString(R.string.weather_forecast_day, day.dayName, day.emoji, day.maxTemp, day.minTemp)
            }
        }
    }

    /** Muestra solo una de las 3 vistas: cargando, error o datos. */
    private fun showWeatherState(loading: Boolean = false, error: Boolean = false) {
        binding.tvWeatherOffline.visibility = View.GONE
        binding.progressWeather.visibility = if (loading) View.VISIBLE else View.GONE
        binding.weatherError.visibility = if (error) View.VISIBLE else View.GONE
        val dataVisibility = if (!loading && !error) View.VISIBLE else View.GONE
        binding.weatherData.visibility = dataVisibility
        binding.forecastContainer.visibility = dataVisibility
    }

    // =====================================================================
    // Intents implícitos
    // =====================================================================

    /** Abre la app de mapas buscando el lugar (o el navegador si no hay app). */
    private fun openMaps() {
        val query = Uri.encode("${place.name}, ${place.municipality}, Huila, Colombia")
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
        try {
            startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
            )
            try {
                startActivity(webIntent)
            } catch (noBrowser: ActivityNotFoundException) {
                Toast.makeText(this, R.string.error_no_maps, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Comparte el lugar por WhatsApp, correo, etc. */
    private fun sharePlace() {
        val text = getString(R.string.share_text, place.name, place.municipality, place.emoji)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_chooser)))
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    companion object {
        /** Forma centralizada de abrir esta pantalla con su parámetro. */
        fun newIntent(context: Context, placeId: Int): Intent =
            Intent(context, DetailActivity::class.java).putExtra(IntentKeys.PLACE_ID, placeId)
    }
}
