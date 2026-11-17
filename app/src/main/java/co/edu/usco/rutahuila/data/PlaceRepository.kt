package co.edu.usco.rutahuila.data

import android.content.Context
import android.content.SharedPreferences
import co.edu.usco.rutahuila.data.db.DatabaseHelper
import co.edu.usco.rutahuila.data.db.PlaceDao
import co.edu.usco.rutahuila.data.weather.WeatherRepository
import co.edu.usco.rutahuila.util.LocationHelper
import co.edu.usco.rutahuila.util.normalized
import java.io.File

/**
 * Fuente única de datos de la app (patrón Repository + Singleton).
 * Las pantallas SOLO hablan con este objeto; no saben de dónde salen los datos.
 *
 * Usa tres tipos de almacenamiento, cada uno para lo que sirve mejor:
 *
 * 1. BASE DE DATOS SQLite (por medio de [PlaceDao]):
 *    los lugares y todo lo que el usuario hace con ellos
 *    (favoritos, visitados, ruta, calificaciones, notas y lugares propios).
 *
 * 2. ARCHIVOS internos (por medio de [PhotoStorage]):
 *    las fotos. En la base de datos solo se guarda la ruta del archivo.
 *
 * 3. SharedPreferences (clave-valor):
 *    datos sueltos y sencillos (nombre, avatar, modo oscuro y fecha del viaje).
 */
object PlaceRepository {

    private const val PREFS_NAME = "ruta_huila_prefs"
    private const val KEY_USER_NAME = "last_user_name"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_TRIP_DATE = "trip_date"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_PERMISSIONS_ASKED = "permissions_asked"

    const val DEFAULT_AVATAR = "😎"
    const val NO_DATE = 0L

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var dao: PlaceDao

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            appContext = context.applicationContext
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            dao = PlaceDao(DatabaseHelper(appContext))
        }
    }

    // =====================================================================
    // Lugares
    // =====================================================================

    fun getAll(): List<Place> = dao.getAllPlaces()

    fun totalPlaces(): Int = dao.getAllPlaces().size

    fun getById(id: Int): Place? = dao.getPlace(id)

    fun randomPlace(): Place = dao.getAllPlaces().random()

    fun search(filter: PlaceFilter, query: String, sort: SortOrder): List<Place> {
        val text = query.normalized()
        val filtered = dao.getAllPlaces()
            .filter { place ->
                when (filter) {
                    PlaceFilter.MINE -> place.isCustom
                    else -> filter.category == null || place.category == filter.category
                }
            }
            .filter { place ->
                text.isEmpty() ||
                    place.name.normalized().contains(text) ||
                    place.municipality.normalized().contains(text)
            }
        return sortPlaces(filtered, sort)
    }

    private fun sortPlaces(list: List<Place>, sort: SortOrder): List<Place> = when (sort) {
        SortOrder.NAME -> list.sortedBy { it.name.normalized() }
        SortOrder.DISTANCE -> list.sortedWith(compareBy<Place> { it.distanceKm }.thenBy { it.name })
        SortOrder.RATING -> list.sortedWith(
            compareByDescending<Place> { getRating(it.id) }.thenBy { it.name }
        )
        // Distancia real desde la ubicación del usuario (GPS)
        SortOrder.NEAR_ME -> list.sortedBy { LocationHelper.distanceKm(it) ?: Int.MAX_VALUE }
    }

    fun toUi(place: Place): PlaceUi {
        val state = dao.getUserState(place.id)
        return PlaceUi(
            place = place,
            isFavorite = state.isFavorite,
            isVisited = state.isVisited,
            isInTrip = state.isInTrip,
            rating = state.rating,
            hasNote = state.note.isNotBlank(),
            hasPhoto = state.photoPath != null
        )
    }

    // =====================================================================
    // Lugares propios (crear, editar y borrar)
    // =====================================================================

    fun addCustomPlace(
        name: String,
        municipality: Municipality,
        category: Category,
        emoji: String,
        description: String
    ): Int = dao.insertPlace(buildCustomPlace(0, name, municipality, category, emoji, description))

    fun updateCustomPlace(
        id: Int,
        name: String,
        municipality: Municipality,
        category: Category,
        emoji: String,
        description: String
    ) = dao.updatePlace(buildCustomPlace(id, name, municipality, category, emoji, description))

    /** Borra un lugar propio junto con su foto. */
    fun deleteCustomPlace(id: Int) {
        removePhoto(id)
        dao.deletePlace(id)
    }

    /** Las coordenadas y la distancia salen del municipio elegido. */
    private fun buildCustomPlace(
        id: Int,
        name: String,
        municipality: Municipality,
        category: Category,
        emoji: String,
        description: String
    ) = Place(
        id = id,
        name = name,
        municipality = municipality.name,
        category = category,
        distanceKm = municipality.distanceKm,
        emoji = emoji,
        latitude = municipality.latitude,
        longitude = municipality.longitude,
        description = description,
        tip = "",
        isCustom = true
    )

    // =====================================================================
    // Favoritos
    // =====================================================================

    fun isFavorite(id: Int): Boolean = dao.getUserState(id).isFavorite

    /** Agrega o quita de favoritos. Devuelve true si quedó como favorito. */
    fun toggleFavorite(id: Int): Boolean {
        val nowFavorite = !isFavorite(id)
        dao.setFavorite(id, nowFavorite)
        return nowFavorite
    }

    fun favoritePlaces(): List<Place> = dao.getFavoritePlaces()

    fun favoritesCount(): Int = dao.countFavorites()

    // =====================================================================
    // Visitados
    // =====================================================================

    fun isVisited(id: Int): Boolean = dao.getUserState(id).isVisited

    fun setVisited(id: Int, visited: Boolean) = dao.setVisited(id, visited)

    fun visitedCount(): Int = dao.countVisited()

    fun tripIds(): List<Int> = dao.getTripIds()

    fun tripPlaces(): List<Place> = dao.getTripPlaces()

    fun isInTrip(id: Int): Boolean = dao.getUserState(id).isInTrip


    fun toggleTrip(id: Int): Boolean {
        val ids = tripIds().toMutableList()
        val nowInTrip = if (ids.remove(id)) false else ids.add(id)
        dao.saveTripOrder(ids)
        return nowInTrip
    }

    fun removeFromTrip(id: Int): Int {
        val ids = tripIds().toMutableList()
        val index = ids.indexOf(id)
        if (index >= 0) {
            ids.removeAt(index)
            dao.saveTripOrder(ids)
        }
        return index
    }

    fun insertIntoTrip(id: Int, index: Int) {
        val ids = tripIds().toMutableList()
        if (id in ids) return
        ids.add(index.coerceIn(0, ids.size), id)
        dao.saveTripOrder(ids)
    }

    fun moveInTrip(from: Int, to: Int) {
        val ids = tripIds().toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        val item = ids.removeAt(from)
        ids.add(to, item)
        dao.saveTripOrder(ids)
    }

    fun clearTrip() {
        dao.saveTripOrder(emptyList())
        tripDateMillis = NO_DATE
    }

    var tripDateMillis: Long
        get() = prefs.getLong(KEY_TRIP_DATE, NO_DATE)
        set(value) = prefs.edit().putLong(KEY_TRIP_DATE, value).apply()

    // =====================================================================
    // Calificaciones y notas
    // =====================================================================

    fun getRating(id: Int): Float = dao.getUserState(id).rating

    fun setRating(id: Int, rating: Float) = dao.setRating(id, rating)

    fun ratedCount(): Int = dao.countRated()

    fun averageRating(): Float = dao.averageRating()

    fun getNote(id: Int): String = dao.getUserState(id).note

    fun setNote(id: Int, note: String) = dao.setNote(id, note.trim())

    // =====================================================================
    // Fotos
    // =====================================================================

    fun getPhotoPath(id: Int): String? =
        dao.getUserState(id).photoPath?.takeIf { File(it).exists() }


    fun setPhoto(id: Int, path: String) {
        val oldPath = dao.getUserState(id).photoPath
        dao.setPhotoPath(id, path)
        if (oldPath != null && oldPath != path) PhotoStorage.delete(oldPath)
    }

    fun removePhoto(id: Int) {
        dao.getUserState(id).photoPath?.let { PhotoStorage.delete(it) }
        dao.setPhotoPath(id, null)
    }

    fun photosCount(): Int = dao.countPhotos()

    // =====================================================================
    // Perfil y ajustes (SharedPreferences)
    // =====================================================================

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var avatar: String
        get() = prefs.getString(KEY_AVATAR, DEFAULT_AVATAR) ?: DEFAULT_AVATAR
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var arePermissionsAsked: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, value).apply()

    /** Borra TODO lo que el usuario ha guardado (base de datos, fotos, clima y ajustes). */
    fun clearAll() {
        dao.resetUserData()
        PhotoStorage.deleteAll(appContext)
        WeatherRepository.clear()
        prefs.edit().clear().apply()
    }
}
