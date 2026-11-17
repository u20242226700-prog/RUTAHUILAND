package co.edu.usco.rutahuila.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import co.edu.usco.rutahuila.R

/** Tipo de plan turístico. Cada categoría tiene su nombre y su color. */
enum class Category(@StringRes val labelRes: Int, @ColorRes val colorRes: Int) {
    ADVENTURE(R.string.filter_adventure, R.color.cat_adventure),
    CULTURE(R.string.filter_culture, R.color.cat_culture),
    NATURE(R.string.filter_nature, R.color.cat_nature)
}

/** Filtros de la pestaña Explorar. MINE = lugares creados por el usuario. */
enum class PlaceFilter(val category: Category?) {
    ALL(null),
    ADVENTURE(Category.ADVENTURE),
    CULTURE(Category.CULTURE),
    NATURE(Category.NATURE),
    MINE(null)
}

/** Formas de ordenar la lista. */
enum class SortOrder {
    NAME,
    DISTANCE,
    RATING,
    NEAR_ME
}


data class Place(
    val id: Int,
    val name: String,
    val municipality: String,
    val category: Category,
    val distanceKm: Int,
    val emoji: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val tip: String,
    val isCustom: Boolean = false
)

/** Modelo para la interfaz: el lugar + todo lo que depende del usuario. */
data class PlaceUi(
    val place: Place,
    val isFavorite: Boolean,
    val isVisited: Boolean,
    val isInTrip: Boolean,
    val rating: Float,
    val hasNote: Boolean,
    val hasPhoto: Boolean
)
