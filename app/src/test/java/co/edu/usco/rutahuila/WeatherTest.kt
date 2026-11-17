package co.edu.usco.rutahuila

import co.edu.usco.rutahuila.data.weather.WeatherCodes
import co.edu.usco.rutahuila.data.weather.WeatherService
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTest {

    private val exampleJson = """
        {
          "current": {
            "temperature_2m": 31.4,
            "relative_humidity_2m": 48,
            "weather_code": 2,
            "wind_speed_10m": 9.6
          },
          "daily": {
            "time": ["2026-11-17", "2026-11-18", "2026-11-19", "2026-11-20"],
            "weather_code": [2, 61, 0, 95],
            "temperature_2m_max": [33.1, 30.6, 34.2, 29.4],
            "temperature_2m_min": [22.0, 21.4, 22.8, 21.1]
          }
        }
    """

    @Test
    fun parse_leeElClimaActual() {
        val weather = WeatherService.parse(exampleJson)
        assertEquals(31.4, weather.temperature, 0.01)
        assertEquals(48, weather.humidity)
        assertEquals(2, weather.code)
    }

    @Test
    fun parse_elPronosticoNoIncluyeElDiaDeHoy() {
        val weather = WeatherService.parse(exampleJson)
        assertEquals(3, weather.forecast.size)
        assertEquals(61, weather.forecast.first().code)
    }

    @Test
    fun parse_redondeaLasTemperaturasDelPronostico() {
        val tomorrow = WeatherService.parse(exampleJson).forecast.first()
        assertEquals(31, tomorrow.maxTemp)
        assertEquals(21, tomorrow.minTemp)
    }

    @Test
    fun codigosDelClima_seConviertenEnElEmojiCorrecto() {
        assertEquals("☀️", WeatherCodes.emoji(0))
        assertEquals("🌧️", WeatherCodes.emoji(61))
        assertEquals("⛈️", WeatherCodes.emoji(95))
    }
}
