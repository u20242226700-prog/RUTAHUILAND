package co.edu.usco.rutahuila.data.weather

import android.content.Context
import android.content.SharedPreferences
import co.edu.usco.rutahuila.data.Place
import org.json.JSONException
import java.io.IOException

/**
 * Resultado de pedir el clima.
 * isFromCache = true significa que no había internet y se usó la copia guardada.
 */
data class WeatherResult(
    val weather: Weather,
    val savedAtMillis: Long,
    val isFromCache: Boolean
)

/**
 * Decide de dónde sale el clima:
 * 1. Intenta pedirlo a internet y guarda una copia de la respuesta.
 * 2. Si falla (sin conexión), devuelve la última copia guardada.
 *
 * La copia es el texto JSON tal cual llegó, guardado en SharedPreferences.
 */
object WeatherRepository {

    private const val PREFS_NAME = "weather_cache"
    private const val KEY_JSON = "json_"
    private const val KEY_TIME = "time_"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Devuelve null solo si no hay internet NI copia guardada. */
    suspend fun getWeather(place: Place): WeatherResult? =
        try {
            val json = WeatherService.fetchJson(place.latitude, place.longitude)
            val weather = WeatherService.parse(json)
            val now = System.currentTimeMillis()
            save(place.id, json, now)
            WeatherResult(weather, now, isFromCache = false)
        } catch (e: IOException) {
            loadSaved(place.id)
        } catch (e: JSONException) {
            loadSaved(place.id)
        }

    private fun save(placeId: Int, json: String, timeMillis: Long) {
        prefs.edit()
            .putString(KEY_JSON + placeId, json)
            .putLong(KEY_TIME + placeId, timeMillis)
            .apply()
    }

    private fun loadSaved(placeId: Int): WeatherResult? {
        val json = prefs.getString(KEY_JSON + placeId, null) ?: return null
        val time = prefs.getLong(KEY_TIME + placeId, 0L)
        return try {
            WeatherResult(WeatherService.parse(json), time, isFromCache = true)
        } catch (e: JSONException) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
