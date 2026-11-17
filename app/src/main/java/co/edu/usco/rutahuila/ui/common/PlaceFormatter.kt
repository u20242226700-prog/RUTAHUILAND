package co.edu.usco.rutahuila.ui.common

import android.content.Context
import android.content.Intent
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.util.IntentKeys
import co.edu.usco.rutahuila.util.LocationHelper

/** Textos que se repiten en varias pantallas, centralizados en un solo lugar. */
object PlaceFormatter {

    fun location(context: Context, place: Place): String {
        val kmFromUser = LocationHelper.distanceKm(place)
        return when {
            kmFromUser != null ->
                context.getString(R.string.place_location_near, place.municipality, kmFromUser)
            place.distanceKm == 0 ->
                context.getString(R.string.place_location_city, place.municipality)
            else ->
                context.getString(R.string.place_location_format, place.municipality, place.distanceKm)
        }
    }

    fun rating(context: Context, rating: Float): String =
        context.getString(R.string.place_rating_format, rating)


    fun detailResultMessage(context: Context, data: Intent?): String? {
        if (data == null) return null

        // Caso especial: el lugar se eliminó desde el detalle
        data.getStringExtra(IntentKeys.DELETED_NAME)?.let { name ->
            return context.getString(R.string.snackbar_place_deleted, name)
        }

        val place = PlaceRepository.getById(data.getIntExtra(IntentKeys.PLACE_ID, -1)) ?: return null
        return when {
            data.getBooleanExtra(IntentKeys.FAVORITE_CHANGED, false) ->
                if (PlaceRepository.isFavorite(place.id)) {
                    context.getString(R.string.snackbar_favorite_added, place.name)
                } else {
                    context.getString(R.string.snackbar_favorite_removed, place.name)
                }
            data.getBooleanExtra(IntentKeys.TRIP_CHANGED, false) ->
                if (PlaceRepository.isInTrip(place.id)) {
                    context.getString(R.string.snackbar_trip_added, place.name)
                } else {
                    context.getString(R.string.snackbar_trip_removed, place.name)
                }
            data.getBooleanExtra(IntentKeys.VISITED_CHANGED, false) &&
                PlaceRepository.isVisited(place.id) ->
                context.getString(R.string.snackbar_visited, place.name)
            data.getBooleanExtra(IntentKeys.RATING_CHANGED, false) ->
                context.getString(R.string.snackbar_rated, place.name)
            else -> null
        }
    }
}
