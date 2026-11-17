package co.edu.usco.rutahuila.data.weather

import androidx.annotation.StringRes
import co.edu.usco.rutahuila.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Clima actual de un lugar + el pronóstico de los próximos días. */
data class Weather(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val code: Int,
    val forecast: List<DayForecast>
) {
    val emoji: String get() = WeatherCodes.emoji(code)

    @get:StringRes
    val descriptionRes: Int get() = WeatherCodes.descriptionRes(code)
}

/** Pronóstico de un día: nombre corto del día, estado y temperaturas. */
data class DayForecast(
    val dayName: String,
    val code: Int,
    val maxTemp: Int,
    val minTemp: Int
) {
    val emoji: String get() = WeatherCodes.emoji(code)
}

/** Traduce los códigos WMO que devuelve la API a un emoji y una descripción. */
object WeatherCodes {

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    @StringRes
    fun descriptionRes(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        in 51..57 -> R.string.weather_drizzle
        in 61..67 -> R.string.weather_rain
        in 71..77 -> R.string.weather_snow
        in 80..82 -> R.string.weather_showers
        in 95..99 -> R.string.weather_storm
        else -> R.string.weather_unknown
    }
}

/**
 * Consume la API REST pública y gratuita de Open-Meteo (no necesita llave).
 * Documentación: https://open-meteo.com/en/docs
 */
object WeatherService {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val TIMEOUT_MS = 8_000

    /** Hoy + los 3 días siguientes. */
    private const val FORECAST_DAYS = 4

    /**
     * Pide el clima actual y el pronóstico en UNA sola petición y devuelve
     * el texto JSON de la respuesta.
     * Se ejecuta en un hilo de fondo (Dispatchers.IO) para no congelar la pantalla.
     */
    suspend fun fetchJson(latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            val url = URL(
                "$BASE_URL?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&forecast_days=$FORECAST_DAYS&timezone=auto"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP ${connection.responseCode}")
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    /** Convierte el JSON de respuesta en un objeto [Weather]. */
    fun parse(json: String): Weather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        return Weather(
            temperature = current.getDouble("temperature_2m"),
            humidity = current.getInt("relative_humidity_2m"),
            windSpeed = current.getDouble("wind_speed_10m"),
            code = current.getInt("weather_code"),
            forecast = parseForecast(root.getJSONObject("daily"))
        )
    }

    /**
     * "daily" trae una lista por cada dato (fechas, códigos, máximas, mínimas).
     * La posición 0 es hoy, por eso se empieza desde la 1.
     */
    private fun parseForecast(daily: JSONObject): List<DayForecast> {
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxTemps = daily.getJSONArray("temperature_2m_max")
        val minTemps = daily.getJSONArray("temperature_2m_min")

        return (1 until dates.length()).map { i ->
            DayForecast(
                dayName = dayName(dates.getString(i)),
                code = codes.getInt(i),
                maxTemp = maxTemps.getDouble(i).roundToInt(),
                minTemp = minTemps.getDouble(i).roundToInt()
            )
        }
    }

    /** "2026-11-12" -> "jue" (en el idioma del celular) */
    private fun dayName(isoDate: String): String =
        runCatching {
            LocalDate.parse(isoDate).dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .removeSuffix(".")
        }.getOrDefault(isoDate)
}
